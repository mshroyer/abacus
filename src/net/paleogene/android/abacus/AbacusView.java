package net.paleogene.android.abacus;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * View that draws, handles touch events for a visual abacus.
 */
public class AbacusView extends SurfaceView
implements SurfaceHolder.Callback {

    class DrawThread extends Thread {
        
        private Semaphore sem = new Semaphore(1);
        
        private boolean isPaused = false;

        public DrawThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        /**
         * Politely ask the thread to stop running.
         */
        public void stopDrawing() {
            interrupt();
        }
        
        /**
         * Pause the drawing thread by acquiring the internal draw mutex.
         * Only a single thread should be allowed to call either this method
         * or resumeDrawing().
         * 
         * @throws InterruptedException
         */
        public void pauseDrawing()
        throws InterruptedException {
            if ( ! isPaused ) {
                sem.acquire();
                isPaused = true;
            }
        }
        
        /**
         * Allow the drawing thread to resume by releasing the internal draw
         * mutex.  Only a single thread should be allowed to call either this
         * method or pauseDrawing().
         */
        public void resumeDrawing() {
            if ( isPaused ) {
                sem.release();
                isPaused = false;
            }
        }

        @Override
        public void run() {
            while ( ! isInterrupted() ) {
                try {
                    sem.acquire();
                } catch ( InterruptedException e ) {
                    return;
                }

                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized ( mSurfaceHolder ) {
                        doDraw(c);
                    }
                } finally {
                    if ( c != null ) mSurfaceHolder.unlockCanvasAndPost(c);
                }
                
                sem.release();
            }
        }
    }

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mSurfaceHolder;

    private DrawThread thread;

    private AbacusEngine rs;

    private RowEngine motionRow = null;
    private int motionBead = -1;
    
    private int mCanvasWidth = 1;
    private int mCanvasHeight = 1;
    
    private Context context;
    
    public AbacusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.context = context;

        // Register interest in changes to the surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    private void doDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if ( rs != null ) rs.draw(canvas);
    }
    
    private void showReadout() {
        if ( Abacus.readout != null ) {
            int result = rs.getValue();
            if ( result > -1 )
                Abacus.readout.setText(Integer.toString(result));
            else
                Abacus.readout.setText("?");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        rs = new AbacusEngine(mCanvasWidth, mCanvasHeight, 6, context);
        
        showReadout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if ( thread == null || thread.getState() == Thread.State.TERMINATED )
            thread = new DrawThread(holder);
        
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Wait until the thread has really shut down, otherwise it might touch
        // the surface after we return and explode...
        boolean retry = true;
        thread.stopDrawing();
        while ( retry ) {
            try {
                thread.join();
                retry = false;
            } catch ( InterruptedException e ) {
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch ( event.getAction() ) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
            if (motionBead > -1) {
                synchronized ( mSurfaceHolder ) {
                    motionRow.moveBeadTo(motionBead, (int) event.getX());
                }
            } else {
                int x, y;
                
                /* Process historical events so that beads don't "slip" */
                /* TODO Try interpolating between such events too... */
                for ( int i = 0; i < event.getHistorySize(); i++ ) {
                    x = (int) event.getHistoricalX(i);
                    y = (int) event.getHistoricalY(i);
                    
                    motionRow = rs.getRowAt(x, y);
                    if ( motionRow != null ) {
                        motionBead = motionRow.getBeadAt(x, y);
                        if ( motionBead > -1 ) {
                            thread.resumeDrawing();
                            break;
                        }
                    }
                }

                if ( motionBead == -1 ) {
                    x = (int) event.getX();
                    y = (int) event.getY();
                    motionRow = rs.getRowAt(x, y);
                    if ( motionRow != null ) {
                        motionBead = motionRow.getBeadAt(x, y);
                        if ( motionBead > -1 )
                            thread.resumeDrawing();
                    }
                }
            }
            return true;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if ( motionBead > -1 ) showReadout();
            motionRow = null;
            motionBead = -1;
            try {
                thread.pauseDrawing();
            } catch ( InterruptedException e ) {
            }
            return true;

        default:
            return super.onTouchEvent(event);
        }
    }

}

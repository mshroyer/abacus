package net.paleogene.android.abacus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * View that draws, handles touch events for a visual abacus.
 */
public class AbacusView extends SurfaceView
implements SurfaceHolder.Callback {

    class DrawThread extends Thread {

        public DrawThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running.  Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while ( mRun ) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized ( mSurfaceHolder ) {
                        doDraw(c);
                    }
                } finally {
                    if ( c != null ) mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }

    /** Indicates whether the surface has been created and is ready to draw */
    private boolean mRun = false;

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
        
        Log.v("Abacus", "surfaceChanged()");
        
        mCanvasWidth = width;
        mCanvasHeight = height;
        rs = new AbacusEngine(mCanvasWidth, mCanvasHeight, 6, context);
        
        // TODO This part should really be in surfaceCreated()...
        showReadout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        
        Log.v("Abacus", "surfaceCreated()");

        if ( thread == null || thread.getState() == Thread.State.TERMINATED )
            thread = new DrawThread(holder);
        
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        
        Log.v("Abacus", "surfaceDestroyed()");

        // Wait until the thread has really shut down, otherwise it might touch
        // the surface after we return and explode...
        boolean retry = true;
        thread.setRunning(false);
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
                        if ( motionBead > -1 )
                            break;
                    }
                }

                if ( motionBead == -1 ) {
                    x = (int) event.getX();
                    y = (int) event.getY();
                    motionRow = rs.getRowAt(x, y);
                    if ( motionRow != null )
                        motionBead = motionRow.getBeadAt(x, y);
                }
            }
            return true;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if ( motionBead > -1 ) showReadout();
            motionRow = null;
            motionBead = -1;
            return true;

        default:
            return super.onTouchEvent(event);
        }
    }

}

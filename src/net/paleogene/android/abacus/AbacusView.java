package net.paleogene.android.abacus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AbacusView extends SurfaceView
implements SurfaceHolder.Callback {

    class AbacusThread extends Thread {

        public AbacusThread(SurfaceHolder surfaceHolder) {
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
                    if ( c != null )
                        mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
    
    class Abacus {
        private Point position;
        private int numRows;
        public Row[] rows;
        private Paint paint;
        final int borderWidth = 10;
        private int width;
        private int beadRY;
        
        public Abacus(Point position, int width, int beadRX, int beadRY,
                int numBeads, int numRows) {
            this.position = position;
            this.numRows = numRows;
            this.rows = new Row[numRows];
            this.width = width;
            this.beadRY = beadRY;
            
            for ( int i = 0; i < numRows; i++ ) {
                Point rowPosition = new Point();
                rowPosition.x = position.x + borderWidth;
                rowPosition.y = position.y + (3*i+1)*beadRY + borderWidth;
                this.rows[i] = new Row(rowPosition, width, beadRX, beadRY, numBeads);
            }
            
            paint = new Paint();
            paint.setColor(Color.argb(255, 188, 157, 118)); // brownish
            paint.setStyle(Paint.Style.FILL);
        }
        
        public Row getRowAt(int x, int y) {
            for ( int i = 0; i < numRows; i++ ) {
                if ( ( x >= position.x + borderWidth )
                        && ( x <= position.x + borderWidth + width )
                        && ( y >= position.y + (3*i+1)*beadRY + borderWidth )
                        && ( y <= position.y + (3*i+3)*beadRY + borderWidth ) )
                    return rows[i];
            }
            return null;
        }
        
        public void draw(Canvas canvas) {
            canvas.drawRect(position.x,
                            position.y,
                            position.x + width + 2*borderWidth,
                            position.y + borderWidth,
                            paint);
            canvas.drawRect(position.x + width + borderWidth,
                            position.y,
                            position.x + width + 2*borderWidth,
                            position.y + (3*numRows+1)*beadRY + 2*borderWidth,
                            paint);
            canvas.drawRect(position.x,
                            position.y + (3*numRows+1)*beadRY + borderWidth,
                            position.x + width + 2*borderWidth,
                            position.y + (3*numRows+1)*beadRY + 2*borderWidth,
                            paint);
            canvas.drawRect(position.x,
                            position.y,
                            position.x + borderWidth,
                            position.y + (3*numRows+1)*beadRY + 2*borderWidth,
                            paint);

            for ( Row r : rows ) {
                r.draw(canvas);
            }
        }
    }

    class Row {
        
        /** Location of the row's upper-right corner on the canvas */
        private Point position;
        
        /** Total width of the row in pixels */
        private int width;
        
        /** Half the beads' width */
        private int beadRX;
        
        /** Half the beads' height */
        private int beadRY;
        
        /** The number of beads on this row */
        private int numBeads;

        /** The horizontal location of the center of each bead on the row */
        private int[] beads;

        private Paint beadPaint, rowPaint;
        private RectF beadRectF = new RectF();
        
        public Row(Point position, int width, int beadRX, int beadRY,
                int numBeads) {
            this.position = position;
            this.width = width;
            this.beadRX = beadRX;
            this.beadRY = beadRY;
            this.numBeads = numBeads;

            beadPaint = new Paint();
            beadPaint.setColor(Color.argb(255, 73, 137, 30));
            beadPaint.setStyle(Paint.Style.FILL);

            rowPaint = new Paint();
            rowPaint.setColor(Color.argb(255, 188, 157, 118)); // brownish
            //rowPaint.setColor(Color.argb(255, 112, 82, 46)); // brownish
            rowPaint.setStyle(Paint.Style.FILL);

            /* Place the beads starting on the left... */
            beads = new int[numBeads];
            for ( int i = 0; i < numBeads; i++ ) {
                beads[i] = ( 2 * i + 1 ) * beadRX;
            }
        }
        
        public Point getPosition() {
            return position;
        }
        
        public int moveBeadTo(int i, int x) {
            return moveBeadToInternal(i, x - position.x - beadRX);
        }

        private int moveBeadToInternal(int i, int x) {
            // Don't allow beads to be dragged off the ends of the row
            x = ( x >= beadRX )         ? x : beadRX;
            x = ( x <= width - beadRX ) ? x : width - beadRX;
            
            // Handle collisions between beads
            if ( x > beads[i] ) {
                if ( ( i < numBeads - 1 )
                        && ( x + beadRX > beads[i+1] - beadRX ) ) {
                    x = moveBeadToInternal(i + 1, x + 2*beadRX) - 2*beadRX;
                }
            } else if ( x < beads[i] ){
                if ( ( i > 0 )
                        && ( x - beadRX < beads[i-1] + beadRX ) ) {
                    x = moveBeadToInternal(i - 1, x - 2*beadRX) + 2*beadRX;
                }
            }
            
            return beads[i] = x;
        }

        public void draw(Canvas canvas) {
            final int rowThickness = 5;

            canvas.drawRect(position.x,
                            position.y + beadRY - rowThickness,
                            position.x + width,
                            position.y + beadRY + rowThickness,
                            rowPaint);

            for ( int i = 0; i < numBeads; i++ ) {
                beadRectF.left   = (float) ( position.x + beads[i] - beadRX );
                beadRectF.right  = (float) ( position.x + beads[i] + beadRX );
                beadRectF.top    = (float) ( position.y );
                beadRectF.bottom = (float) ( position.y + 2 * beadRY );
                canvas.drawOval(beadRectF, beadPaint);
            }
        }

        public int getBeadAt(int x, int y) {
            for ( int i = 0; i < numBeads; i++ ) {
                if ( ( x >= ( position.x + beads[i] - beadRX ) ) 
                        && ( x <= ( position.x + beads[i] + beadRX ) )
                        && ( y >= ( position.y ) )
                        && ( y <= ( position.y + 2 * beadRY ) ) ) {
                    return i;
                }
            }
            return -1;
        }
    }

    /** Indicates whether the surface has been created and is ready to draw */
    private boolean mRun = false;

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mSurfaceHolder;

    private AbacusThread thread;

    private Abacus abacus;

    private Row motionRow = null;
    private int motionBead = -1;
    private float motionStartX;
    private float motionStartY;

    public AbacusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Register interest in changes to the surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        abacus = new Abacus(new Point(40, 50), 372, 15, 25, 10, 5);

        // Just create the thread; it's started in surfaceCreated()
        thread = new AbacusThread(holder);
    }

    private void doDraw(Canvas canvas) {

        canvas.drawColor(Color.BLACK);
        abacus.draw(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // TODO Something or other
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread.getState() == Thread.State.TERMINATED)
            thread = new AbacusThread(holder);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
                motionRow.moveBeadTo(motionBead, (int) event.getX());
            } else {
                int x = (int) event.getX();
                int y = (int) event.getY();
                motionRow = abacus.getRowAt(x, y);
                if ( motionRow != null )
                    motionBead = motionRow.getBeadAt(x, y);
                motionStartX = event.getX();
                motionStartY = event.getY();
            }
            return true;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            motionRow = null;
            motionBead = -1;
            return true;

        default:
            return super.onTouchEvent(event);
        }
    }

}

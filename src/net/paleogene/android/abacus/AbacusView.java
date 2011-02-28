package net.paleogene.android.abacus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * View that draws, handles touch events for a visual abacus.
 */
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
    
    class RowSet {
        private Point position;
        private int numRows;
        public Row[] rows;
        private Paint paint;
        final int borderWidth = 10;
        private int width;
        private int beadHeight;
        
        public RowSet(Point position, int width, int beadWidth, int beadHeight,
                int numBeads, int numRows) {
            this.position = position;
            this.numRows = numRows;
            this.rows = new Row[numRows];
            this.width = width;
            this.beadHeight = beadHeight;
            
            for ( int i = 0; i < numRows; i++ ) {
                Point rowPosition = new Point();
                rowPosition.x = position.x + borderWidth;
                rowPosition.y = position.y + (3*i+1)*beadHeight/2 + borderWidth;
                this.rows[i] = new Row(rowPosition, width, beadWidth, beadHeight, numBeads);
            }
            
            paint = new Paint();
            paint.setColor(Color.argb(255, 188, 157, 118)); // brownish
            paint.setStyle(Paint.Style.FILL);
        }
        
        public Row getRowAt(int x, int y) {
            for ( int i = 0; i < numRows; i++ ) {
                if ( ( x >= position.x + borderWidth )
                        && ( x <= position.x + borderWidth + width )
                        && ( y >= position.y + (3*i+1)*beadHeight/2 + borderWidth )
                        && ( y <= position.y + (3*i+3)*beadHeight/2 + borderWidth ) )
                    return rows[i];
            }
            return null;
        }
        
        /**
         * Calculate the total value of all rows in the abacus.  If any of
         * the rows have an indeterminate value (as signaled by a -1 returned
         * by that row's getValue() method, the entire abacus is considered
         * indeterminate and this method will also return -1.
         * 
         * @return Current value on the abacus, or -1 if indeterminate
         */
        public int getValue() {
            int accumulator = 0;
            
            for ( Row r : rows ) {
                accumulator *= 10;
                int rval = r.getValue();
                if ( rval > -1 )
                    accumulator += rval;
                else
                    return -1;
            }
            
            return accumulator;
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
                            position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                            paint);
            canvas.drawRect(position.x,
                            position.y + (3*numRows+1)*beadHeight/2 + borderWidth,
                            position.x + width + 2*borderWidth,
                            position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                            paint);
            canvas.drawRect(position.x,
                            position.y,
                            position.x + borderWidth,
                            position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                            paint);

            for ( Row r : rows ) {
                r.draw(canvas);
            }
        }
    }

    class Row {
        
        /** Location of the row's upper-right corner on the canvas */
        private Point position;
        
        /** Total width of the row in real device pixels */
        private int width;
        
        /** The beads' width in real device pixels */
        private int beadWidth;
        
        /** The beads' height in real device pixels */
        private int beadHeight;
        
        /** The number of beads on this row */
        private int numBeads;

        /** The horizontal location of the center of each bead on the row */
        private int[] beads;

        private Paint beadPaint, rowPaint;
        private RectF beadRectF = new RectF();
        
        private Drawable beadImg;
        
        public Row(Point position, int width, int beadWidth, int beadHeight,
                int numBeads) {
            this.position = position;
            this.width = width;
            this.beadWidth = beadWidth;
            this.beadHeight = beadHeight;
            this.numBeads = numBeads;
            
            Resources res = getResources();
            beadImg = res.getDrawable(R.drawable.bead);

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
                beads[i] = i * beadWidth;
            }
        }
        
        public Point getPosition() {
            return position;
        }
        
        public int moveBeadTo(int i, int x) {
            return moveBeadToInternal(i, x - position.x);
        }

        private int moveBeadToInternal(int i, int x) {
            // Don't allow beads to be dragged off the ends of the row
            x = ( x >= 0 )                 ? x : 0;
            x = ( x <= width - beadWidth ) ? x : width - beadWidth;
            
            // Handle collisions between beads
            if ( x > beads[i] ) {
                // ... when moving right:
                if ( ( i < numBeads - 1 )
                        && ( x + beadWidth > beads[i+1] ) ) {
                    x = moveBeadToInternal(i + 1, x + beadWidth) - beadWidth;
                }
            } else if ( x < beads[i] ){
                // ... when moving left:
                if ( ( i > 0 )
                        && ( x - beadWidth < beads[i-1] ) ) {
                    x = moveBeadToInternal(i - 1, x - beadWidth) + beadWidth;
                }
            }
            
            return beads[i] = x;
        }
        
        /**
         * Get the row's current value
         * 
         * @return Current value set on the row, or -1 if indeterminate
         */
        public int getValue() {
            if ( beads[0] >= 1.5 * beadWidth )
                return 9;
            for ( int i = 0; i < numBeads - 1; i++ )
                if ( beads[i+1] - beads[i] >= 1.5 * beadWidth ) 
                    return 8 - i;
            if ( beads[numBeads-1] <= width - 2.5 * beadWidth )
                return 0;
            
            return -1;
        }

        public void draw(Canvas canvas) {
            final int rowThickness = 5;

            canvas.drawRect(position.x,
                            position.y + beadHeight / 2 - rowThickness,
                            position.x + width,
                            position.y + beadHeight / 2 + rowThickness,
                            rowPaint);

            for ( int i = 0; i < numBeads; i++ ) {
                beadRectF.left   = (float) ( position.x + beads[i] );
                beadRectF.right  = (float) ( position.x + beads[i] + beadWidth );
                beadRectF.top    = (float) ( position.y );
                beadRectF.bottom = (float) ( position.y + beadHeight );
                canvas.drawOval(beadRectF, beadPaint);
                /*
                beadImg.setBounds(position.x + beads[i] - beadRX,
                                  position.y,
                                  position.x + beads[i] + beadRX,
                                  position.y + 2*beadRY);
                beadImg.draw(canvas);
                */
            }
        }

        public int getBeadAt(int x, int y) {
            for ( int i = 0; i < numBeads; i++ ) {
                if ( ( x >= ( position.x + beads[i] ) ) 
                        && ( x <= ( position.x + beads[i] + beadWidth ) )
                        && ( y >= ( position.y ) )
                        && ( y <= ( position.y + beadHeight ) ) ) {
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

    private RowSet rs;

    private Row motionRow = null;
    private int motionBead = -1;
    
    public AbacusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Register interest in changes to the surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        rs = new RowSet(new Point(60, 50), 330, 30, 50, 9, 5);

        // Just create the thread; it's started in surfaceCreated()
        thread = new AbacusThread(holder);
    }

    private void doDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        rs.draw(canvas);
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
        // TODO Something or other
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread.getState() == Thread.State.TERMINATED)
            thread = new AbacusThread(holder);
        thread.setRunning(true);
        thread.start();
        showReadout();
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
                int x, y;
                
                /* Process historical events so that beads don't "slip" */
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

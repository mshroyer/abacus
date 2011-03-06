package net.paleogene.android.abacus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

public class RowEngine {
    
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
    
    private Drawable beadDrawable;
    
    public RowEngine(Point position, int beadWidth, int beadHeight, Drawable bead) {
        this.position = position;
        this.width = 11*beadWidth;
        this.beadWidth = beadWidth;
        this.beadHeight = beadHeight;
        this.numBeads = 9;
        this.beadDrawable = bead;
        
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
            if ( beads[i+1] - beads[i] >= 2.5 * beadWidth ) 
                return 8 - i;
        if ( beads[numBeads-1] <= width - 2.5 * beadWidth )
            return 0;
        
        return -1;
    }

    public void draw(Canvas canvas) {
        int rowThickness = beadWidth/2;
        rowThickness -= rowThickness % 2;
        
        canvas.drawRect(position.x,
                        position.y + beadHeight/2 - rowThickness/2,
                        position.x + width,
                        position.y + beadHeight/2 + rowThickness/2,
                        rowPaint);

        for ( int i = 0; i < numBeads; i++ ) {
            /*
            beadRectF.left   = (float) ( position.x + beads[i] );
            beadRectF.right  = (float) ( position.x + beads[i] + beadWidth );
            beadRectF.top    = (float) ( position.y );
            beadRectF.bottom = (float) ( position.y + beadHeight );
            canvas.drawOval(beadRectF, beadPaint);
            */
            beadDrawable.setBounds(position.x + beads[i],
                                   position.y,
                                   position.x + beads[i] + beadWidth,
                                   position.y + beadHeight);
            beadDrawable.draw(canvas);
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

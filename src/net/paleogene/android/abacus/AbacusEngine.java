package net.paleogene.android.abacus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class AbacusEngine {
    private Point position;
    private int numRows;
    public RowEngine[] rows;
    private Paint paint;
    private int beadHeight;
    private int beadWidth;
    private int borderWidth;
    private int rowWidth;
    
    public AbacusEngine(int width, int height, int numRows, Context context) {
        
        this.beadWidth = Math.min(height/(3*numRows+2), width/12);
        this.beadHeight = 2 * beadWidth;
        
        this.numRows = numRows;
        this.rows = new RowEngine[numRows];
        
        this.borderWidth = beadWidth/2;
        this.rowWidth = 11*beadWidth;
        
        this.position = new Point((width-12*beadWidth)/2,
                                  (height-(3*numRows+2)*beadWidth)/2);
        
        Resources res = context.getResources();
        Bitmap resBmp = BitmapFactory.decodeResource(res, R.drawable.bead);
        Bitmap beadBmp = Bitmap.createScaledBitmap(resBmp, beadWidth, beadHeight, false);
        Drawable beadDrawable = new BitmapDrawable(beadBmp);
        
        for ( int i = 0; i < numRows; i++ ) {
            Point rowPosition = new Point();
            rowPosition.x = position.x + borderWidth;
            rowPosition.y = position.y + (1+3*i)*beadHeight/2 + borderWidth;
            this.rows[i] = new RowEngine(rowPosition, beadWidth, beadHeight, beadDrawable);
        }
        
        paint = new Paint();
        paint.setColor(Color.argb(255, 188, 157, 118)); // brownish
        paint.setStyle(Paint.Style.FILL);
    }
    
    public RowEngine getRowAt(int x, int y) {
        for ( int i = 0; i < numRows; i++ ) {
            if ( ( x >= position.x + borderWidth )
                    && ( x <= position.x + borderWidth + rowWidth )
                    && ( y >= position.y + borderWidth + (1+3*i)*beadHeight/2 )
                    && ( y <= position.y + borderWidth + (4+3*i)*beadHeight/2 ) )
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
        
        for ( RowEngine r : rows ) {
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
                        position.x + rowWidth + 2*borderWidth,
                        position.y + borderWidth,
                        paint);
        canvas.drawRect(position.x + rowWidth + borderWidth,
                        position.y,
                        position.x + rowWidth + 2*borderWidth,
                        position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                        paint);
        canvas.drawRect(position.x,
                        position.y + (3*numRows+1)*beadHeight/2 + borderWidth,
                        position.x + rowWidth + 2*borderWidth,
                        position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                        paint);
        canvas.drawRect(position.x,
                        position.y,
                        position.x + borderWidth,
                        position.y + (3*numRows+1)*beadHeight/2 + 2*borderWidth,
                        paint);

        for ( RowEngine r : rows ) {
            r.draw(canvas);
        }
    }
}

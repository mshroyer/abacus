package net.paleogene.android.abacus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AbacusView extends SurfaceView
implements SurfaceHolder.Callback {
	
	class AbacusThread extends Thread {
		
		public AbacusThread(SurfaceHolder surfaceHolder, Context context) {
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
	
	class Row {
		private Point position;
		private int width;
		private int beadSize;
		private int numBeads;
		
		private int[] beads;
		
		private Paint beadPaint, rowPaint;
				
		public Row(Point position, int width, int beadSize, int numBeads) {
			this.position = position;
			this.width = width;
			this.beadSize = beadSize;
			this.numBeads = numBeads;
			
			beadPaint = new Paint();
			beadPaint.setColor(Color.BLUE);
			beadPaint.setStyle(Paint.Style.FILL);
			
			rowPaint = new Paint();
			rowPaint.setColor(Color.argb(255, 112, 82, 46));
			rowPaint.setStyle(Paint.Style.FILL);
			
			beads = new int[numBeads];
			for ( int i = 0; i < numBeads; i++ ) {
				beads[i] = 2 * i * beadSize;
			}
		}
		
		public void moveBeadToCoordinate(int i, float x) {
			beads[i] = (int) x - position.x - beadSize;
		}
		
		public void draw(Canvas canvas) {
			int y = position.y + beadSize;
			
			canvas.drawRect(position.x - 10,
							y - 5,
							position.x + width + 2 * beadSize + 10,
							y + 5,
							rowPaint);
			
			for ( int i = 0; i < numBeads; i++ ) {
				canvas.drawCircle((float) position.x + beadSize + beads[i],
								  (float) y,
								  beadSize,
								  beadPaint);
			}
		}
		
		public int getBeadAt(float x, float y) {
			for ( int i = 0; i < numBeads; i++ ) {
				double d = Math.sqrt(Math.pow(beads[i] + position.x + beadSize - x, 2)
									+ Math.pow(position.y + beadSize - y, 2));
				if ( d <= (double) beadSize )
					return i;
			}
			return -1;
		}
	}
	
	/** Indicates whether the surface has been created and is ready to draw */
	private boolean mRun = false;
	
	/** Handle to the surface manager object we interact with */
	private SurfaceHolder mSurfaceHolder;
	
	private AbacusThread thread;
	
	private Row row;
	
	private int motionBead = -1;
	private float motionStartX;
	private float motionStartY;
	
	public AbacusView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Register interest in changes to the surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		row = new Row(new Point(50, 50), 320, 25, 1);
		
		// Just create the thread; it's started in surfaceCreated()
		thread = new AbacusThread(holder, context);
	}
	
	private void doDraw(Canvas canvas) {
		
		canvas.drawColor(Color.BLACK);
		row.draw(canvas);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Something or other
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
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
			motionBead = row.getBeadAt(event.getX(), event.getY());
			motionStartX = event.getX();
			motionStartY = event.getY();
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if ( motionBead > -1 )
				row.moveBeadToCoordinate(motionBead, event.getX());
			return true;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			motionBead = -1;
			return true;
			
		default:
			return super.onTouchEvent(event);
		}
	}

}

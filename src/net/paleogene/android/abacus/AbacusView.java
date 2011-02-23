package net.paleogene.android.abacus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AbacusView extends SurfaceView
implements SurfaceHolder.Callback {
	
	/** Indicates whether the surface has been created and is ready to draw */
	private boolean mRun = false;
	
	class AbacusThread extends Thread {
		
		public AbacusThread(SurfaceHolder surfaceHolder, Context context,
				Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;
			mContext = context;
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
	
	/** Handle to the surface manager object we interact with */
	private SurfaceHolder mSurfaceHolder;
	
	private Handler mHandler;
	
	private Context mContext;
	
	/**
	 * Current width of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasWidth = 1;
	
	/**
	 * Current height of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasHeight = 1;
	
	private AbacusThread thread;
	
	private float pointX = 200;
	private float pointY = 200;
	private float lastX = 0;
	private float lastY = 0;
	
	private boolean moving = false;
	
	public void setSurfaceSize(int width, int height) {
		synchronized ( mSurfaceHolder ) {
			mCanvasWidth  = width;
			mCanvasHeight = height;
		}
	}

	public AbacusView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Register interest in changes to the surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		// Just create the thread; it's started in surfaceCreated()
		thread = new AbacusThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
			}
		});
	}
	
	private void doDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Paint.Style.FILL);
		
		canvas.drawCircle(pointX, pointY, 25, paint);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		setSurfaceSize(width, height);
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
			if ( ( Math.abs( pointX - event.getX() ) <= 25 )
					&& ( Math.abs( pointY - event.getY() ) <= 25 ) ) { 
				moving = true;
				lastX = event.getX();
				lastY = event.getY();
			}
			return true;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			moving = false;
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if ( moving ) {
				pointX += event.getX() - lastX;
				pointY += event.getY() - lastY;
				lastX = event.getX();
				lastY = event.getY();
			}
			return true;
			
		default:
			return super.onTouchEvent(event);
		}
	}

}

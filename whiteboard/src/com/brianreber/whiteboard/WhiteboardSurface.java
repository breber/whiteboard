package com.brianreber.whiteboard;

import java.util.ArrayList;
import java.util.List;

import net.margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The SurfaceView allowing drawing
 * 
 * @author breber
 */
public class WhiteboardSurface extends SurfaceView implements SurfaceHolder.Callback, OnColorChangedListener {

	/**
	 * The Thread that updates the UI
	 */
	private CanvasThread mThread;

	/**
	 * A list of paths that are drawn on the screen
	 */
	private List<PathWrapper> mPaths = new ArrayList<PathWrapper>();

	/**
	 * The current path being updated
	 */
	private Path mPath;

	/**
	 * The painting parameters for the current Path
	 */
	private Paint mPaint;

	/**
	 * Create a new WhiteboardSurface
	 * 
	 * @param context
	 */
	public WhiteboardSurface(Context context) {
		super(context);
		getHolder().addCallback(this);
		setFocusable(true);

		mPaint = new Paint();
		mPaint.setDither(true);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(10);
	}

	/**
	 * Get a Bitmap of the current state
	 * 
	 * @return a Bitmap of the current state
	 */
	public Bitmap getBitmap() {
		Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		onDraw(canvas);

		return bitmap;
	}

	/**
	 * Performs an undo operation
	 */
	public void undo() {
		synchronized (mThread.getSurfaceHolder()) {
			if (mPaths.size() > 0) {
				mPaths.remove(mPaths.size() - 1);
			}
		}
	}

	/**
	 * Clear the canvas
	 */
	public void clearCanvas() {
		synchronized (mThread.getSurfaceHolder()) {
			mPaths.clear();
		}
	}

	/**
	 * Get the color of the Paint
	 * 
	 * @return the paint color
	 */
	public int getColor() {
		return mPaint.getColor();
	}

	/* (non-Javadoc)
	 * @see net.margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener#onColorChanged(int)
	 */
	@Override
	public void onColorChanged(int color) {
		mPaint = new Paint(mPaint);
		mPaint.setColor(color);
	}

	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (mThread.getSurfaceHolder()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mPath = new Path();
				mPath.moveTo(event.getX(), event.getY());
				mPath.lineTo(event.getX(), event.getY());
				mPaths.add(new PathWrapper(mPath, mPaint));
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				mPath.lineTo(event.getX(), event.getY());
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mPath.lineTo(event.getX(), event.getY());
			}

			return true;
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	public void onDraw(Canvas canvas) {
		canvas.drawColor(Color.WHITE);

		for (PathWrapper path : mPaths) {
			canvas.drawPath(path.getPath(), path.getPaint());
		}
	}

	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Nothing needs to be done here
	}

	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mThread == null) {
			mThread = new CanvasThread(getHolder(), this);
		}

		mThread.setRunning(true);
		mThread.start();
	}

	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// simply copied from sample application LunarLander:
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
				mThread = null;
			} catch (InterruptedException e) {
				// we will try it again and again...
			}
		}
	}
}

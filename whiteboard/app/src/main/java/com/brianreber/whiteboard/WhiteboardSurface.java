package com.brianreber.whiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener;

import java.util.ArrayList;
import java.util.List;

/**
 * The SurfaceView allowing drawing
 * 
 * @author breber
 */
public class WhiteboardSurface extends SurfaceView implements SurfaceHolder.Callback, OnColorChangedListener, CustomColorPickerDialog.OnPenSizeChangedListener {

	/**
	 * The Thread that updates the UI
	 */
	private CanvasThread mThread;

	/**
	 * The base bitmap
	 */
	private Bitmap mBaseBitmap = null;

	/**
	 * A list of paths that are drawn on the screen
	 */
	private List<PathWrapper> mPaths = new ArrayList<>();

	/**
	 * The current path being updated
	 */
	private SparseArray<Path> mPathMap = new SparseArray<>();

	/**
	 * The painting parameters for the current Path
	 */
	private Paint mPaint;

	/**
	 * Represents whether multitouch drawing is enabled
	 */
	private boolean enableMultitouch = true;

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
		mPaint.setDither(false);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(7);
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
	 * Set the base bitmap
	 * 
	 * @param aBitmap
	 */
	public void setBaseBitmap(Bitmap aBitmap) {
		synchronized (mThread.getSurfaceHolder()) {
			mPaths.clear();
			mBaseBitmap = aBitmap;
		}
	}

	/**
	 * Performs an undo operation
	 */
	public boolean undo() {
		synchronized (mThread.getSurfaceHolder()) {
			if (mPaths.size() > 0) {
				mPaths.remove(mPaths.size() - 1);

				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Clear the canvas
	 */
	public void clearCanvas() {
		synchronized (mThread.getSurfaceHolder()) {
			mPaths.clear();
			mBaseBitmap = null;
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

	/**
	 * Get the current pen size
	 * 
	 * @return the current pen size
	 */
	public int getPenSize() {
		return (int) mPaint.getStrokeWidth();
	}

	/**
	 * Enable/disable multitouch
	 * 
	 * @param aMultitouchEnabled
	 */
	public void setMultitouch(boolean aMultitouchEnabled) {
		enableMultitouch = aMultitouchEnabled;
	}

	/* (non-Javadoc)
	 * @see net.margaritov.preference.colorpicker.ColorPickerDialog.OnPenSizeChangedListener#onSizeChanged(int)
	 */
	@Override
	public void onSizeChanged(int size) {
		mPaint = new Paint(mPaint);
		mPaint.setStrokeWidth(size);
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
			final int action = event.getAction();
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_DOWN:
				if (!enableMultitouch) {
					break;
				}
			case MotionEvent.ACTION_DOWN: {
				final int pointerIndex = event.getActionIndex();
				final int pointerId = event.getPointerId(pointerIndex);
				final float x = event.getX(pointerIndex);
				final float y = event.getY(pointerIndex);

				Path path = new Path();
				path.moveTo(x, y);
				path.lineTo(x, y);

				mPathMap.put(pointerId, path);
				mPaths.add(new PathWrapper(path, mPaint));
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				// Add the historical data
				for (int h = 0; h < event.getHistorySize(); h++) {
					for (int p = 0; p < event.getPointerCount(); p++) {
						final float x = event.getHistoricalX(p, h);
						final float y = event.getHistoricalY(p, h);
						final int pointerId = event.getPointerId(p);

						Path path = mPathMap.get(pointerId);
						if (path != null) {
							path.lineTo(x, y);
						}
					}
				}

				// Add current data
				for (int p = 0; p < event.getPointerCount(); p++) {
					final float x = event.getX(p);
					final float y = event.getY(p);
					final int pointerId = event.getPointerId(p);
					Path path = mPathMap.get(pointerId);
					if (path != null) {
						path.lineTo(x, y);
					}
				}

				break;
			}

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_POINTER_UP: {
				// Extract the index of the pointer that left the touch sensor
				final int pointerIndex = event.getActionIndex();
				final int pointerId = event.getPointerId(pointerIndex);
				final float x = event.getX(pointerIndex);
				final float y = event.getY(pointerIndex);

				Path path = mPathMap.get(pointerId);
				mPathMap.remove(pointerId);
				if (path != null) {
					path.lineTo(x, y);
				}
				break;
			}
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

		if (mBaseBitmap != null) {
			canvas.drawBitmap(mBaseBitmap, 0, 0, null);
		}

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

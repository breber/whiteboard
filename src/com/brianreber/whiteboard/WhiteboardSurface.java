package com.brianreber.whiteboard;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WhiteboardSurface extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = WhiteboardSurface.class.getSimpleName();

	private CanvasThread mThread;
	private ArrayList<Path> mPaths = new ArrayList<Path>();
	private Path mPath;
	private Paint mPaint;

	public WhiteboardSurface(Context context) {
		super(context);
		getHolder().addCallback(this);
		mThread = new CanvasThread(getHolder(), this);
		setFocusable(true);

		mPaint = new Paint();
		mPaint.setDither(true);
		mPaint.setColor(Color.RED);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(3);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (mThread.getSurfaceHolder()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mPath = new Path();
				mPath.moveTo(event.getX(), event.getY());
				mPath.lineTo(event.getX(), event.getY());
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				mPath.lineTo(event.getX(), event.getY());
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mPath.lineTo(event.getX(), event.getY());
				mPaths.add(mPath);
			}

			return true;
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		for (Path path : mPaths) {
			canvas.drawPath(path, mPaint);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mThread.setRunning(true);
		mThread.start();
	}

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
			} catch (InterruptedException e) {
				// we will try it again and again...
			}
		}
	}
}

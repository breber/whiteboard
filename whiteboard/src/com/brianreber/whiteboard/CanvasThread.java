package com.brianreber.whiteboard;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Represents a thread that will update a canvas
 * 
 * @author breber
 */
public class CanvasThread extends Thread {
	/**
	 * The SurfaceHolder that contains the Canvas
	 */
	private SurfaceHolder mSurfaceHolder;

	/**
	 * The SurfaceView that will draw the Canvas
	 */
	private WhiteboardSurface mPanel;

	/**
	 * Whether or not to continue updating the canvas
	 */
	private boolean mRun = false;

	/**
	 * Create a new CanvasThread with the given parameters
	 * 
	 * @param surfaceHolder
	 * @param panel
	 */
	public CanvasThread(SurfaceHolder surfaceHolder, WhiteboardSurface panel) {
		mSurfaceHolder = surfaceHolder;
		mPanel = panel;
	}

	/**
	 * Set the Run parameter
	 * 
	 * @param run whether we should be repainting the screen
	 */
	public void setRunning(boolean run) {
		mRun = run;
	}

	/**
	 * Get an instance of the SurfaceHolder
	 * 
	 * @return an instance of the SurfaceHolder
	 */
	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		Canvas c;
		while (mRun) {
			c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					if (c != null) {
						mPanel.onDraw(c);
					}
				}
			} finally {
				// do this in a finally so that if an exception is thrown
				// during the above, we don't leave the Surface in an
				// inconsistent state
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}
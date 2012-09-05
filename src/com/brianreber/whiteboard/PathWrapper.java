package com.brianreber.whiteboard;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Wraps a Path and Paint so that each line can have a different color
 * 
 * @author breber
 */
public class PathWrapper {

	private Path mPath;

	private Paint mPaint;

	/**
	 * @param path
	 * @param paint
	 */
	public PathWrapper(Path path, Paint paint) {
		this.mPath = path;
		this.mPaint = paint;
	}

	/**
	 * @return the path
	 */
	public Path getPath() {
		return mPath;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(Path path) {
		this.mPath = path;
	}

	/**
	 * @return the paint
	 */
	public Paint getPaint() {
		return mPaint;
	}

	/**
	 * @param paint the paint to set
	 */
	public void setPaint(Paint paint) {
		this.mPaint = paint;
	}

}

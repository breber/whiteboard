package com.brianreber.whiteboard;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class WhiteboardActivity extends Activity {

	/**
	 * The WhiteboardSurface instance
	 */
	private WhiteboardSurface mSurface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_whiteboard);

		mSurface = new WhiteboardSurface(this);

		LinearLayout v = (LinearLayout) findViewById(R.id.whiteboardWrapper);
		v.addView(mSurface);

		ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: save image...

				Toast.makeText(WhiteboardActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.deleteButton);
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: maybe popup to make sure?

				mSurface.clearCanvas();
			}
		});

		ImageButton undoButton = (ImageButton) findViewById(R.id.undoButton);
		undoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSurface.undo();
			}
		});
	}

}

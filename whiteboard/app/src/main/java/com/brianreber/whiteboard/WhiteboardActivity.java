package com.brianreber.whiteboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class WhiteboardActivity extends Activity {

	/**
	 * Activity result from opening the image
	 */
	private static final int OPEN_RESULT = Math.abs("OPEN".hashCode());

	/**
	 * Activity result from sharing the image
	 */
	private static final int SHARE_RESULT = Math.abs("SHARE".hashCode());

	/**
	 * Activity result from preferences activity
	 */
	private static final int PREFS_RESULT = Math.abs("PREFERENCES".hashCode());

	/**
	 * The path to the image that is shared
	 */
	private static final String sShareFileName = "whiteboardshared.png";

	/**
	 * Dropbox API Access
	 */
	private DropboxAPI<AndroidAuthSession> mDBApi;

	/**
	 * The WhiteboardSurface instance
	 */
	private WhiteboardSurface mSurface;

	/**
	 * Are we currently saving?
	 */
	private boolean mIsSaving = false;

	/**
	 * The filename
	 */
	private String mFileName = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_whiteboard);

		mSurface = new WhiteboardSurface(this);

		// Set multitouch
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSurface.setMultitouch(prefs.getBoolean(getString(R.string.prefMultitouchEnabled), true));

		LinearLayout v = (LinearLayout) findViewById(R.id.whiteboardWrapper);
		v.addView(mSurface);

		// Get access to Dropbox API
		mDBApi = DropboxUtils.getDropboxApi(this);

		ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mIsSaving = true;
				performSave();
			}
		});

		ImageButton openButton = (ImageButton) findViewById(R.id.openButton);
		openButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDBApi.getSession().isLinked()) {
					Intent i = new Intent(WhiteboardActivity.this, ListDropboxFiles.class);
					startActivityForResult(i, OPEN_RESULT);
				} else {
					mDBApi.getSession().startOAuth2Authentication(WhiteboardActivity.this);
				}
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.deleteButton);
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: maybe popup to make sure?

				mSurface.clearCanvas();
				mFileName = null;
			}
		});

		ImageButton undoButton = (ImageButton) findViewById(R.id.undoButton);
		undoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSurface.undo();
			}
		});

		ImageButton colorButton = (ImageButton) findViewById(R.id.colorButton);
		colorButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int swatchColor = mSurface.getColor();
				int penSize = mSurface.getPenSize();

				CustomColorPickerDialog cpd = new CustomColorPickerDialog(WhiteboardActivity.this, swatchColor, penSize);
				cpd.setOnColorChangedListener(mSurface);
				cpd.setOnPenSizeChangedListener(mSurface);
				cpd.show();
			}
		});

		ImageButton shareButton = (ImageButton) findViewById(R.id.shareButton);
		shareButton.setOnClickListener(new OnClickListener() {
			@SuppressLint("WorldReadableFiles")
			@Override
			public void onClick(View v) {
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("image/png");

				try {
					FileOutputStream fos = WhiteboardActivity.this.openFileOutput(sShareFileName, Context.MODE_PRIVATE);
					File f = getFileStreamPath(sShareFileName);
					Bitmap bitmap = mSurface.getBitmap();
					bitmap.compress(CompressFormat.PNG, 0, fos);

					share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));

					startActivityForResult(Intent.createChooser(share, "Share image:"), SHARE_RESULT);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		ImageButton prefsButton = (ImageButton) findViewById(R.id.prefsButton);
		prefsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					startActivityForResult(new Intent(WhiteboardActivity.this, WhiteboardPreferenceActivity.class), PREFS_RESULT);
				} else {
					startActivityForResult(new Intent(WhiteboardActivity.this, WhiteboardPreferenceActivitySupport.class), PREFS_RESULT);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// If we want to use the back button as an undo button, do that
		// When there is nothing else to undo, just let the super method handle it
		if (prefs.getBoolean(getString(R.string.prefBackUndo), false)) {
			if (!mSurface.undo()) {
				super.onBackPressed();
			}
		} else {
			super.onBackPressed();
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (mDBApi.getSession().authenticationSuccessful()) {
			DropboxUtils.finishAuthentication(this);

			if (mIsSaving) {
				performSave();
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// When we return from the share intent, delete the temp file
		if (SHARE_RESULT == requestCode) {
			File f = getFileStreamPath(sShareFileName);
			if (f.exists()) {
				f.delete();
			}
		} else if (PREFS_RESULT == requestCode) {
			// When we return from the preferences, update the Whiteboard Surface
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			mSurface.setMultitouch(prefs.getBoolean(getString(R.string.prefMultitouchEnabled), true));
		} else if (OPEN_RESULT == requestCode && RESULT_OK == resultCode) {
			final String fileName = data.getStringExtra("fileName");
			Toast.makeText(this, "Opening file..." + fileName, Toast.LENGTH_SHORT).show();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						DropboxInputStream dis = mDBApi.getFileStream(fileName, null);
						Bitmap bm = BitmapFactory.decodeStream(dis);
						mSurface.setBaseBitmap(bm);
						mFileName = fileName;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Start performing the save
	 * 
	 * Prompt the user for a filename if they are logged in to Dropbox. If they
	 * aren't logged in to Dropbox, start the Dropbox authentication
	 */
	private void performSave() {
		if (mDBApi.getSession().isLinked()) {
			if (mFileName != null) {
				saveToDropbox(mFileName, true);
			} else {
				AlertDialog.Builder dlg = new AlertDialog.Builder(WhiteboardActivity.this);

				dlg.setTitle("Save As");
				dlg.setMessage("Choose a file name");

				// Set an EditText view to get user input
				final EditText input = new EditText(WhiteboardActivity.this);
				input.setText(new Date().toString());
				input.selectAll();
				dlg.setView(input);

				dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						final String value = input.getText().toString();

						mFileName = value;
						saveToDropbox(value, false);
					}
				});

				dlg.show();
			}
			mIsSaving = false;
		} else {
			mDBApi.getSession().startOAuth2Authentication(WhiteboardActivity.this);
		}
	}

	/**
	 * Get the image and save it to Dropbox
	 * 
	 * @param fileName the name to save the file as
	 * @param overwrite should we overwrite the file if it exists?
	 */
	private void saveToDropbox(final String fileName, final boolean overwrite) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmap = mSurface.getBitmap();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
				byte[] bitmapdata = bos.toByteArray();
				ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

				try {
					Entry newEntry;

					String fullFileName = fileName;

					// Make sure it ends in ".png"
					if (!fullFileName.endsWith(".png")) {
						fullFileName = fullFileName + ".png";
					}

					if (overwrite) {
						newEntry = mDBApi.putFileOverwrite(fullFileName, bs, bitmapdata.length, null);
					} else {
						newEntry = mDBApi.putFile(fullFileName, bs, bitmapdata.length, null, null);
					}

					// Update the filename if it was modified
					WhiteboardActivity.this.mFileName = newEntry.fileName();

					Log.i("DbExampleLog", "The uploaded file's rev is: " + newEntry.rev);
				} catch (DropboxUnlinkedException e) {
					// User has unlinked, ask them to link again here.
					Log.e("DbExampleLog", "User has unlinked.");
				} catch (DropboxException e) {
					Log.e("DbExampleLog", "Something went wrong while uploading.");
				}
			}
		}).start();
	}

}

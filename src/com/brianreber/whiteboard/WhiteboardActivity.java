package com.brianreber.whiteboard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class WhiteboardActivity extends Activity {

	/**
	 * Dropbox App Key
	 */
	private static final String APP_KEY = "sipz3v1k0xkmftv";

	/**
	 * Dropbox App Secret
	 */
	private static final String APP_SECRET = "azlt2f1yyr2xmo8";

	/**
	 * Dropbox Access Type
	 */
	private static final AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

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
	private boolean isSaving = false;

	/**
	 * The filename
	 */
	private String fileName = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_whiteboard);

		mSurface = new WhiteboardSurface(this);

		LinearLayout v = (LinearLayout) findViewById(R.id.whiteboardWrapper);
		v.addView(mSurface);

		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		AccessTokenPair access = getStoredKeys();
		if (access != null) {
			mDBApi.getSession().setAccessTokenPair(access);
		}

		// TODO: size

		ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isSaving = true;
				performSave();
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.deleteButton);
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: maybe popup to make sure?

				mSurface.clearCanvas();
				fileName = null;
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

				Dialog dlg = new ColorPickerDialog(WhiteboardActivity.this, mSurface, swatchColor);
				dlg.show();
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				mDBApi.getSession().finishAuthentication();

				AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();

				// Provide your own storeKeys to persist the access token pair
				// A typical way to store tokens is using SharedPreferences
				storeKeys(tokens.key, tokens.secret);
			} catch (IllegalStateException e) {
				Log.i("DbAuthLog", "Error authenticating", e);
			}

			if (isSaving) {
				performSave();
			}
		}
	}

	/**
	 * Start performing the save
	 * 
	 * Prompt the user for a filename if they are logged in to Dropbox. If they
	 * aren't logged in to Dropbox, start the Dropbox authentication
	 */
	private void performSave() {
		if (mDBApi.getSession().isLinked()) {
			if (fileName != null) {
				saveToDropbox(fileName, true);
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

						fileName = value;
						saveToDropbox(value, false);
					}
				});

				dlg.show();
			}
			isSaving = false;
		} else {
			mDBApi.getSession().startAuthentication(WhiteboardActivity.this);
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
					WhiteboardActivity.this.fileName = newEntry.fileName();

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

	/**
	 * Store Dropbox Keys
	 * 
	 * @param key
	 * @param secret
	 */
	private void storeKeys(String key, String secret) {
		SharedPreferences prefs = getPreferences(0);
		Editor editor = prefs.edit();

		editor.putString("APP_KEY", key);
		editor.putString("APP_SECRET", secret);

		editor.commit();
	}

	/**
	 * Get the Dropbox Keys
	 * 
	 * @return the AccessTokenPair
	 */
	private AccessTokenPair getStoredKeys() {
		SharedPreferences prefs = getPreferences(0);

		if (prefs.contains("APP_KEY") && prefs.contains("APP_SECRET")) {
			return new AccessTokenPair(prefs.getString("APP_KEY", ""), prefs.getString("APP_SECRET", ""));
		}

		return null;
	}

}

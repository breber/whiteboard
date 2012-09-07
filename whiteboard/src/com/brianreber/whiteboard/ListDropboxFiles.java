package com.brianreber.whiteboard;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * List all Files from our Dropbox Folder
 * 
 * @author breber
 */
public class ListDropboxFiles extends Activity {

	/**
	 * The Handler used to perform operations on the UI thread
	 */
	private Handler h = new Handler();

	/**
	 * The ArrayAdapter for the file names
	 */
	private ArrayAdapter<String> adapter;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create a new ListView
		final ListView lv = new ListView(ListDropboxFiles.this);

		// When a user clicks on filename, return the name in the result intent
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("fileName", adapter.getItem(arg2));
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});
		setContentView(lv);

		// Load all file names in the background
		new Thread(new Runnable() {
			@Override
			public void run() {
				final List<String> files = DropboxUtils.getListOfFiles();

				h.post(new Runnable() {
					@Override
					public void run() {
						adapter = new ArrayAdapter<String>(ListDropboxFiles.this, android.R.layout.simple_list_item_1, files);
						lv.setAdapter(adapter);
					}
				});
			}
		}).start();
	}

}

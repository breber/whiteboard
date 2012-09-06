package com.brianreber.whiteboard;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

/**
 * Application preferences for devices which do not support PreferenceFragments
 * 
 * @author breber
 */
public class WhiteboardPreferenceActivitySupport extends PreferenceActivity {

	/**
	 * The Disconnect from Dropbox button
	 */
	private Button disconnectDropbox;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		ListView lv = getListView();

		disconnectDropbox = new Button(this);
		disconnectDropbox.setText(R.string.unlinkDropbox);
		disconnectDropbox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DropboxUtils.clearAuthentication(WhiteboardPreferenceActivitySupport.this);
				enableDisableButton();
			}
		});

		enableDisableButton();
		lv.addHeaderView(disconnectDropbox);
	}

	/**
	 * Enable or disable the Unlink button
	 */
	private void enableDisableButton() {
		if (disconnectDropbox != null) {
			// Enable the button when we are linked, disable when we aren't
			if (DropboxUtils.isLinked(this)) {
				disconnectDropbox.setEnabled(true);
			} else {
				disconnectDropbox.setEnabled(false);
			}
		}
	}
}

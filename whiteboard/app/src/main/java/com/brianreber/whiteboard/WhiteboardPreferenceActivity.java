package com.brianreber.whiteboard;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Application preferences for devices which support PreferenceFragments
 * 
 * @author breber
 */
public class WhiteboardPreferenceActivity extends PreferenceActivity {

	/**
	 * The Disconnect from Dropbox button
	 */
	private Button disconnectDropbox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);

		disconnectDropbox = (Button) findViewById(R.id.disconnectDropbox);
		if (disconnectDropbox != null) {
			disconnectDropbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					DropboxUtils.clearAuthentication(WhiteboardPreferenceActivity.this);
					enableDisableButton();
				}
			});

			enableDisableButton();
		}
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

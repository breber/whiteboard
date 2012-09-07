package com.brianreber.whiteboard;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A Fragment containing preferences
 * 
 * @author breber
 */
@TargetApi(11)
public class WhiteboardPreferenceFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}

}

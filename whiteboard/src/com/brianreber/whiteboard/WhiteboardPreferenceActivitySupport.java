package com.brianreber.whiteboard;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WhiteboardPreferenceActivitySupport extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

	}

}

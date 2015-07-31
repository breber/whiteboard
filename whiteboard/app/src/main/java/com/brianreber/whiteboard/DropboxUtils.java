package com.brianreber.whiteboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.util.ArrayList;
import java.util.List;

public class DropboxUtils {

	/**
	 * Preference key for App Secret
	 */
	private static final String PREFS_APPSECRET = "APP_SECRET";

	/**
	 * Preference key for App Key
	 */
	private static final String PREFS_APPKEY = "APP_KEY";

	/**
	 * Preference key
	 */
	private static final String PREFS_KEY = "WHITEBOARD";

	/**
	 * Dropbox App Key
	 */
	private static final String APP_KEY = "sipz3v1k0xkmftv";

	/**
	 * Dropbox App Secret
	 */
	private static final String APP_SECRET = "azlt2f1yyr2xmo8";

	/**
	 * The current Dropbox Session
	 */
	private static DropboxAPI<AndroidAuthSession> sCurrentSession = null;

	// Private constructor
	private DropboxUtils() { }

	/**
	 * Get Dropbox API access
	 * 
	 * @return an active Dropbox session
	 */
	public static DropboxAPI<AndroidAuthSession> getDropboxApi(Context aContext) {
		AppKeyPair appKeys = new AppKeyPair(DropboxUtils.APP_KEY, DropboxUtils.APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys);
		sCurrentSession = new DropboxAPI<>(session);

		setUpSession(aContext);

		return sCurrentSession;
	}

	/**
	 * Set up the current session
	 * 
	 * @param aContext
	 */
	private static void setUpSession(Context aContext) {
		AccessTokenPair access = getStoredKeys(aContext);
		if (access != null && sCurrentSession != null) {
			sCurrentSession.getSession().setAccessTokenPair(access);
		}
	}

	/**
	 * Complete the authentication process
	 * 
	 * @param aContext
	 */
	public static void finishAuthentication(Context aContext) {
		try {
			if (sCurrentSession != null) {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				sCurrentSession.getSession().finishAuthentication();

				AccessTokenPair tokens = sCurrentSession.getSession().getAccessTokenPair();

				// Provide your own storeKeys to persist the access token pair
				// A typical way to store tokens is using SharedPreferences
				storeKeys(aContext, tokens.key, tokens.secret);
			}
		} catch (IllegalStateException e) {
			Log.i("DbAuthLog", "Error authenticating", e);
		}
	}

	/**
	 * Clear all Authentication
	 * 
	 * @param aContext
	 */
	public static void clearAuthentication(Context aContext) {
		SharedPreferences prefs = aContext.getSharedPreferences(PREFS_KEY, 0);
		Editor editor = prefs.edit();

		editor.remove(PREFS_APPKEY);
		editor.remove(PREFS_APPSECRET);

		editor.apply();

		// Unlink the current session
		if (sCurrentSession != null) {
			sCurrentSession.getSession().unlink();
		}
	}

	/**
	 * Check whether we are linked to Dropbox
	 * 
	 * @param aContext
	 * @return true if we are linked, false otherwise
	 */
	public static boolean isLinked(Context aContext) {
		return sCurrentSession != null && sCurrentSession.getSession().isLinked() && getStoredKeys(aContext) != null;

	}

	/**
	 * Store Dropbox Keys
	 * 
	 * @param ctx
	 * @param key
	 * @param secret
	 */
	public static void storeKeys(Context ctx, String key, String secret) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_KEY, 0);
		Editor editor = prefs.edit();

		editor.putString(PREFS_APPKEY, key);
		editor.putString(PREFS_APPSECRET, secret);

		editor.apply();
	}

	/**
	 * Get the Dropbox Keys
	 * 
	 * @param aContext the Activity to get the preferences from
	 * @return the AccessTokenPair
	 */
	private static AccessTokenPair getStoredKeys(Context aContext) {
		SharedPreferences prefs = aContext.getSharedPreferences(PREFS_KEY, 0);

		if (prefs.contains(PREFS_APPKEY) && prefs.contains(PREFS_APPSECRET)) {
			return new AccessTokenPair(prefs.getString(PREFS_APPKEY, ""), prefs.getString(PREFS_APPSECRET, ""));
		}

		return null;
	}

	/**
	 * Get a list of files
	 * 
	 * @return a list of files
	 */
	public static List<String> getListOfFiles() {
		List<String> toRet = new ArrayList<>();

		try {
			List<Entry> result = sCurrentSession.search("", ".png", 0, false);

			for (Entry e : result) {
				toRet.add(e.fileName());
			}
		} catch (DropboxException e) {
			e.printStackTrace();
		}

		return toRet;
	}

}

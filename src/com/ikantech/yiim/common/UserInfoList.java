package com.ikantech.yiim.common;

import org.json.JSONArray;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.ikantech.support.util.YiPrefsKeeper.YiPrefsKeepable;

public class UserInfoList implements YiPrefsKeepable {
	private static final String USERINFO_PREFS_NAME = "yiim_us";
	private JSONArray mUserList;
	private String mUser;

	private static UserInfoList mInstance = null;

	public static UserInfoList getInstance() {
		if (mInstance == null) {
			mInstance = new UserInfoList();
		}
		return mInstance;
	}

	private UserInfoList() {
		mUser = null;
		mUserList = new JSONArray();
	}

	@Override
	public void save(Editor editor) {
		editor.putString("user", mUser);
		editor.putString("users", mUserList.toString());
	}

	@Override
	public void restore(SharedPreferences preferences) {
		String users = preferences.getString("users", null);
		try {
			if (users != null) {
				mUserList = new JSONArray(users);
			}
		} catch (Exception e) {
		}
		mUser = preferences.getString("user", null);
	}

	@Override
	public String getPrefsName() {
		// TODO Auto-generated method stub
		return USERINFO_PREFS_NAME;
	}

	public void setUser(String user) {
		if (user != null && !user.equals(mUser)) {
			mUserList.put(user);
		}
		mUser = user;
	}

	public String getUser() {
		return mUser;
	}
}

package com.ikantech.yiim.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.ikantech.support.util.YiBase64;
import com.ikantech.support.util.YiPrefsKeeper;
import com.ikantech.support.util.YiPrefsKeeper.YiPrefsKeepable;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;

public class UserInfo implements YiPrefsKeepable {
	private static final String USERINFO_PREFS_NAME = "yiim_us_";

	private String mUserName;
	private String mPasswd;
	private boolean mRememberPasswd;
	private boolean mAutoLogin;

	private boolean mIsMsgNoTip;
	private boolean mIsMsgTipAudio;
	private boolean mIsMsgTipVibrator;
	private boolean mIsFirstLogin;

	private static UserInfo mUserInfo = null;

	private UserInfo(String username) {
		mUserName = username;
		mPasswd = null;
	}

	@Override
	public void save(Editor editor) {
		editor.putString("username", mUserName);
		editor.putBoolean("remember_pwd", mRememberPasswd);
		editor.putBoolean("auto_login", mAutoLogin);
		editor.putBoolean("msg_no_tip", mIsMsgNoTip);
		editor.putBoolean("msg_tip_audio", mIsMsgTipAudio);
		editor.putBoolean("msg_tip_vibrator", mIsMsgTipVibrator);
		editor.putBoolean("is_first_login", mIsFirstLogin);
		if (mRememberPasswd) {
			editor.putString("passwd", YiBase64.encode(mPasswd));
		} else {
			editor.putString("passwd", "");
		}
	}

	@Override
	public void restore(SharedPreferences preferences) {
		mUserName = preferences.getString("username", null);
		mPasswd = preferences.getString("passwd", null);
		mRememberPasswd = preferences.getBoolean("remember_pwd", false);
		mIsMsgNoTip = preferences.getBoolean("msg_no_tip", false);
		mIsMsgTipAudio = preferences.getBoolean("msg_tip_audio", true);
		mIsMsgTipVibrator = preferences.getBoolean("msg_tip_vibrator", false);
		mAutoLogin = preferences.getBoolean("auto_login", false);
		mIsFirstLogin = preferences.getBoolean("is_first_login", true);
		if (!YiUtils.isStringInvalid(mPasswd)) {
			mPasswd = YiBase64.decode(mPasswd);
		}
	}

	@Override
	public String getPrefsName() {
		return USERINFO_PREFS_NAME + mUserName;
	}

	public String getUserName() {
		return mUserName;
	}

	public void setUserName(String username) {
		this.mUserName = username;
	}

	public String getPasswd() {
		return mPasswd;
	}

	public void setPasswd(String passwd) {
		this.mPasswd = passwd;
	}

	public boolean isRememberPasswd() {
		return mRememberPasswd;
	}

	public void setRememberPasswd(boolean rememberPasswd) {
		this.mRememberPasswd = rememberPasswd;
	}

	public boolean isAutoLogin() {
		return mAutoLogin;
	}

	public void setAutoLogin(boolean autoLogin) {
		this.mAutoLogin = autoLogin;
	}

	public boolean isMsgNoTip() {
		return mIsMsgNoTip;
	}

	public void setMsgNoTip(boolean isMsgNoTip) {
		this.mIsMsgNoTip = isMsgNoTip;
	}

	public boolean isMsgTipAudio() {
		return mIsMsgTipAudio;
	}

	public void setMsgTipAudio(boolean isMsgTipAudio) {
		this.mIsMsgTipAudio = isMsgTipAudio;
	}

	public boolean isMsgTipVibrator() {
		return mIsMsgTipVibrator;
	}

	public void setMsgTipVibrator(boolean isMsgTipVibrator) {
		this.mIsMsgTipVibrator = isMsgTipVibrator;
	}

	public boolean isFirstLogin() {
		return mIsFirstLogin;
	}

	public void setFirstLogin(boolean isFirstLogin) {
		this.mIsFirstLogin = isFirstLogin;
	}

	public String getUser() {
		return mUserName + "@" + XmppConnectionUtils.getXmppHost();
	}

	public static UserInfo getUserInfo(Context context, boolean force) {
		YiPrefsKeeper.read(context, UserInfoList.getInstance());
		if (UserInfoList.getInstance().getUser() != null
				&& (mUserInfo == null || force)) {
			mUserInfo = new UserInfo(UserInfoList.getInstance().getUser());
			YiPrefsKeeper.read(context, mUserInfo);
		}
		return mUserInfo;
	}

	public static UserInfo getUserInfo(Context context) {
		return getUserInfo(context, false);
	}
}

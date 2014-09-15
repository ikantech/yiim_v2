package com.ikantech.yiim.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiPrefsKeeper;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.UserInfoList;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.ui.base.CustomTitleActivity;

public class LoginActivity extends CustomTitleActivity {
	private EditText mUserNameEditText;
	private EditText mPasswdEditText;
	private CheckBox mRememberPwdCheckBox;
	private CheckBox mAutoLoginCheckBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_login);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		cancelProgressDialog();
		switch (result.what) {
		case XMPP_START:
			if (result.status.equals(Status.FAILED)) {
				showMsgDialog(getString(R.string.err_init_failed),
						getString(R.string.str_ok));
			}
			break;
		case XMPP_LOGIN:
			if (result.status.equals(Status.FAILED)) {
				YiLog.getInstance().e("login failed: %s", result.obj);
				showMsgDialog(getString(R.string.err_login_failed),
						getString(R.string.str_ok));
				getXmppBinder().restartXmppService(this);
			} else {
				UserInfoList.getInstance().setUser(
						mUserNameEditText.getText().toString());
				YiPrefsKeeper.write(this, UserInfoList.getInstance());

				UserInfo info = UserInfo.getUserInfo(this, true);
				info.setUserName(mUserNameEditText.getText().toString());
				info.setPasswd(mPasswdEditText.getText().toString());
				info.setAutoLogin(mAutoLoginCheckBox.isChecked());
				info.setRememberPasswd(mRememberPwdCheckBox.isChecked());
				YiPrefsKeeper.write(this, info);

				getYiIMApplication().setLogin(true);
				getXmppBinder().initAfterLogin();

				Intent intent = new Intent(LoginActivity.this,
						MainActivity.class);
				startActivity(intent);
				finish();
			}
			break;

		default:
			break;
		}
	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mUserNameEditText = (EditText) findViewById(R.id.login_user_edit);
		mPasswdEditText = (EditText) findViewById(R.id.login_passwd_edit);
		mRememberPwdCheckBox = (CheckBox) findViewById(R.id.login_remember_pwd);
		mAutoLoginCheckBox = (CheckBox) findViewById(R.id.login_auto_login);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		setTitleBarLeftBtnText(getString(R.string.str_exit));
		setTitleBarRightBtnText(getString(R.string.str_register));

		UserInfo userInfo = UserInfo.getUserInfo(this);
		if (userInfo != null) {
			if (!isStringInvalid(userInfo.getUserName())) {
				mUserNameEditText.setText(userInfo.getUserName());
			}

			if (!isStringInvalid(userInfo.getPasswd())) {
				mPasswdEditText.setText(userInfo.getPasswd());
			}

			mRememberPwdCheckBox.setChecked(userInfo.isRememberPasswd());
			mAutoLoginCheckBox.setChecked(userInfo.isAutoLogin());
		}

		getXmppBinder().startXmppService(this);
		showProgressDialog(R.string.str_loading_init);
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	public void onForgetPasswdClick(View view) {

	}

	@Override
	public void onTitleBarLeftBtnClick(View view) {
		// TODO Auto-generated method stub
		getXmppBinder().stopXmppService(null);
		super.onTitleBarLeftBtnClick(view);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		getXmppBinder().stopXmppService(null);
		super.onBackPressed();
	}

	@Override
	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub
		Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
		startActivity(intent);
	}

	public void onLoginClick(View view) {
		if (isStringInvalid(mUserNameEditText.getText().toString().trim())) {
			showMsgDialog(getString(R.string.err_empty_user_name),
					getString(R.string.str_ok));
			return;
		}
		if (isStringInvalid(mPasswdEditText.getText().toString().trim())) {
			showMsgDialog(getString(R.string.err_empty_passwd),
					getString(R.string.str_ok));
			return;
		}

		if (!mUserNameEditText.getText().toString().trim()
				.matches("^[a-z0-9]{5,}$")) {
			showMsgDialog(getString(R.string.err_illegal_username));
			return;
		}

		getXmppBinder().login(mUserNameEditText.getText().toString().trim(),
				mPasswdEditText.getText().toString().trim(), this);
		showProgressDialog(R.string.str_logining);
	}
}

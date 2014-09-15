package com.ikantech.yiim.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.UserInfo;

public class SplashActivity extends Activity {
	private static final int MSG_LOADING_TIMEOUT = 0x00;
	private static final int LOADING_DELAYED = 2000;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MSG_LOADING_TIMEOUT:
				suggestLaunch();
				break;
			default:
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		YiIMApplication application = (YiIMApplication) getApplication();
		if (application.isFirstLaunch()) {
			application.setFirstLaunch(false);
			setContentView(R.layout.activity_splash);
			mHandler.sendEmptyMessageDelayed(MSG_LOADING_TIMEOUT,
					LOADING_DELAYED);
		} else {
			suggestLaunch();
		}
	}

	private void suggestLaunch() {
		YiIMApplication application = (YiIMApplication) getApplication();

		Intent intent = null;

		if (application.isLogin()) {
			intent = new Intent(SplashActivity.this, MainActivity.class);
		} else {
			intent = new Intent(SplashActivity.this, LoginActivity.class);

			UserInfo userInfo = UserInfo.getUserInfo(this);
			if (userInfo != null && userInfo.isAutoLogin()
					&& userInfo.isRememberPasswd()
					&& !YiUtils.isStringInvalid(userInfo.getUserName())
					&& !YiUtils.isStringInvalid(userInfo.getPasswd())) {
				intent = new Intent(SplashActivity.this, MainActivity.class);
			}
		}

		startActivity(intent);
		overridePendingTransition(0, R.anim.translate_out);
		finish();
	}
}

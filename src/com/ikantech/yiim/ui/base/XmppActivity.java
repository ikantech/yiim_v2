package com.ikantech.yiim.ui.base;

import android.os.Bundle;
import android.view.WindowManager;

import com.ikantech.support.ui.YiUIBaseActivity;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.service.XmppService.XmppBinder;

public abstract class XmppActivity extends YiUIBaseActivity implements
		XmppListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// auto hide keyboard
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onXmppResonpse(final XmppResult result) {
		getHandler().post(new Runnable() {
			@Override
			public void run() {
				onUIXmppResponse(result);
			}
		});
	}

	protected abstract void onUIXmppResponse(XmppResult result);

	protected XmppBinder getXmppBinder() {
		return getYiIMApplication().getXmppService();
	}

	protected YiIMApplication getYiIMApplication() {
		return (YiIMApplication) getApplication();
	}

}

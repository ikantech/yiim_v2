package com.ikantech.yiim.runnable;

import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.listener.XmppListener;

public abstract class XmppRunnable implements Runnable {
	protected XmppListener mListener;

	public XmppRunnable(XmppListener listener) {
//		if (listener == null) {
//			throw new NullPointerException("listener non-null");
//		}
		mListener = listener;
	}

	public abstract XmppResult execute();

	protected abstract XmppCmds getCmd();

	protected XmppResult createResult() {
		XmppResult ret = new XmppResult();
		ret.what = getCmd();

		return ret;
	}

	@Override
	public void run() {
		XmppResult result = execute();
		if (mListener != null) {
			mListener.onXmppResonpse(result);
		}
	}
}

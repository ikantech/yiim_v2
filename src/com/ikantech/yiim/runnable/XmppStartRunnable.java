package com.ikantech.yiim.runnable;

import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.listener.XmppListener;

public class XmppStartRunnable extends XmppRunnable {

	public XmppStartRunnable(XmppListener listener) {
		super(listener);
	}

	@Override
	public XmppResult execute() {
		XmppResult result = createResult();
		try {
			XmppConnectionUtils.getInstance().getConnection();
			result.status = Status.SUCCESS;
		} catch (Exception e) {
			result.obj = e.getMessage();
		}
		return result;
	}

	@Override
	protected XmppCmds getCmd() {
		// TODO Auto-generated method stub
		return XmppCmds.XMPP_START;
	}

}
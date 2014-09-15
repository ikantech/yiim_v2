package com.ikantech.yiim.common;

import android.os.Bundle;

public class XmppResult {
	public enum Status {
		SUCCESS, FAILED
	}

	public enum XmppCmds {
		XMPP_UNKNOWN, XMPP_LOGIN, XMPP_START, XMPP_STOP, XMPP_REGISTER, XMPP_LOAD_ROSTER, XMPP_DELETE_ENTRY, XMPP_ADD_ENTRY
	}

	public XmppCmds what;
	public Status status = Status.FAILED;
	public Bundle data;
	public int arg0;
	public int arg1;
	public Object obj;

	public XmppResult() {
		data = null;
		what = XmppCmds.XMPP_UNKNOWN;
		arg0 = 0;
		arg1 = 0;
		obj = null;
	}
}

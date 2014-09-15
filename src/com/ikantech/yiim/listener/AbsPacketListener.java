package com.ikantech.yiim.listener;

import java.util.Map;

import org.jivesoftware.smack.PacketListener;

import android.content.Context;

import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.YiIMUtils;

public abstract class AbsPacketListener implements PacketListener {
	protected Context mContext;
	protected XmppBinder mXmppService;

	public AbsPacketListener(Context context, XmppBinder xmppBinder) {
		if (context == null || xmppBinder == null) {
			throw new NullPointerException("params non-null");
		}
		mContext = context;
		mXmppService = xmppBinder;
	}

	public void broadcast(String what, Map<String, String> params) {
		YiIMUtils.broadcast(mContext, what, params);
	}
}

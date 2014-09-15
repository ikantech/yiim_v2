package com.ikantech.yiim.runnable;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;

import com.ikantech.support.util.YiFileUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.listener.FileDownloadListener;
import com.ikantech.yiim.listener.XmppListener;

public class XmppLoginRunnable extends XmppRunnable {
	private String mUserName;
	private String mPasswd;
	private Type mType;
	private String mStatus;
	private Mode mMode;

	public XmppLoginRunnable(XmppListener listener) {
		super(listener);
		mType = Type.available;
		mMode = Mode.available;
		mStatus = null;
	}

	@Override
	public XmppResult execute() {
		XmppResult result = createResult();
		try {
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection.isAuthenticated()) {
				XmppConnectionUtils.getInstance().closeConnection();
				connection = XmppConnectionUtils.getInstance().getConnection();
			}
			connection.login(mUserName, mPasswd);
			Presence presence = new Presence(mType, mStatus, 1, mMode);
			connection.sendPacket(presence);

			FileDownloadListener.setRecieveFilePath(YiFileUtils.getStorePath()
					+ "yiim/" + mUserName + "file_recv/");

			result.status = Status.SUCCESS;
		} catch (Exception e) {
			result.obj = e.getMessage();
		}
		return result;
	}

	@Override
	protected XmppCmds getCmd() {
		// TODO Auto-generated method stub
		return XmppCmds.XMPP_LOGIN;
	}

	public void setUserName(String userName) {
		this.mUserName = userName;
	}

	public void setPasswd(String passwd) {
		this.mPasswd = passwd;
	}

	public void setType(Type type) {
		this.mType = type;
	}

	public void setStatus(String status) {
		this.mStatus = status;
	}

	public void setMode(Mode mode) {
		this.mMode = mode;
	}
}

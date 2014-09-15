package com.ikantech.yiim.runnable;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;

import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.listener.XmppListener;

public class XmppRegisterRunnable extends XmppRunnable {
	private String mUserName;
	private String mPasswd;

	public XmppRegisterRunnable(XmppListener listener) {
		super(listener);
		// TODO Auto-generated constructor stub
	}

	@Override
	public XmppResult execute() {
		// TODO Auto-generated method stub
		XmppResult result = createResult();

		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(XmppConnectionUtils.getXmppHost());
		reg.setUsername(mUserName);
		reg.setPassword(mPasswd);
		PacketFilter filter = new AndFilter(new PacketIDFilter(
				reg.getPacketID()), new PacketTypeFilter(IQ.class));
		IQ iq = null;
		try {
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection.isAuthenticated()) {
				XmppConnectionUtils.getInstance().closeConnection();
				connection = XmppConnectionUtils.getInstance().getConnection();
			}
			
			PacketCollector collector = XmppConnectionUtils.getInstance()
					.getConnection().createPacketCollector(filter);
			XmppConnectionUtils.getInstance().getConnection().sendPacket(reg);
			iq = (IQ) collector.nextResult(SmackConfiguration
					.getPacketReplyTimeout());
			collector.cancel();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			result.obj = e.getMessage();
			return result;
		}

		if (iq == null) {
			result.obj = "No response from server.";
		} else if (iq.getType() == IQ.Type.RESULT) {
			result.status = Status.SUCCESS;
		} else {
			result.obj = "unknown error";
		}

		return result;
	}

	@Override
	protected XmppCmds getCmd() {
		// TODO Auto-generated method stub
		return XmppCmds.XMPP_REGISTER;
	}

	public void setUserName(String mUserName) {
		this.mUserName = mUserName;
	}

	public void setPasswd(String mPasswd) {
		this.mPasswd = mPasswd;
	}

}

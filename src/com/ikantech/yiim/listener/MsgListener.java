package com.ikantech.yiim.listener;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.DelayInformation;

import android.content.ContentValues;
import android.content.Context;

import com.ikantech.support.util.YiLog;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.FileUtils;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class MsgListener extends AbsPacketListener {
	private FileUtils mFileUtils;

	// 定义PacketFilter，过滤非Chat的包
	public static final PacketFilter PACKET_FILTER = new PacketFilter() {
		@Override
		public boolean accept(Packet packet) {
			// TODO Auto-generated method stub
			if (Message.class.isInstance(packet)) {
				Message message = (Message) packet;
				if (Message.Type.chat.equals(message.getType())
						|| Message.Type.groupchat.equals(message.getType())) {
					return true;
				}
			}
			return false;
		}
	};

	public MsgListener(Context context, XmppBinder xmppBinder) {
		super(context, xmppBinder);
		// TODO Auto-generated constructor stub
		mFileUtils = FileUtils.getInstance();
	}

	@Override
	public void processPacket(Packet packet) {
		// TODO Auto-generated method stub
		if (!Message.class.isInstance(packet)) {
			return;
		}
		try {

			Message message = (Message) packet;

			DelayInformation delayInfo = (DelayInformation) message
					.getExtension("x", "jabber:x:delay");

			if (message.getBody() == null || message.getFrom() == null) {
				return;
			}

			YiLog.getInstance().i("msg %s", message.toXML());

			if ((Message.Type.chat.equals(message.getType()) || Message.Type.groupchat
					.equals(message.getType())) && message.getBody() != null) {
				ContentValues values = new ContentValues();

				Map<String, String> params = new HashMap<String, String>();

				if ((Message.Type.groupchat.equals(message.getType()) && StringUtils
						.getJidResouce(packet.getFrom()).equals(
								UserInfo.getUserInfo(mContext).getUserName()))) {
					values.put(MsgColumns.SENDER, packet.getTo());
					values.put(MsgColumns.RECEIVER, packet.getFrom());

					params.put("from", packet.getTo());
				} else {
					values.put(MsgColumns.SENDER, packet.getFrom());
					values.put(MsgColumns.RECEIVER, packet.getTo());

					params.put("from", packet.getFrom());
				}

				String who = null;
				if ((packet.getFrom().startsWith(UserInfo.getUserInfo(mContext)
						.getUserName()))) {
					who = packet.getTo();
				} else {
					who = packet.getFrom();
				}

				if (who == null || who.length() < 1) {
					return;
				}

				Long now = Long.valueOf(System.currentTimeMillis());

				YiIMMessage imMessage = YiIMMessage.fromString(message
						.getBody());
				if (MsgType.AUDIO.equals(imMessage.getType())) {
					try {
						String filename = mFileUtils.storeAudioFile(UserInfo
								.getUserInfo(mContext).getUserName(), imMessage
								.getBody());
						imMessage.setBody(filename);
						values.put(MsgColumns.CONTENT, imMessage.toString());
					} catch (Exception e) {
						// TODO: handle exception
						values.put(MsgColumns.CONTENT, "");
					}
				} else {
					values.put(MsgColumns.CONTENT, message.getBody());
				}

				String time = null;
				if (delayInfo != null) {
					time = String.valueOf(delayInfo.getStamp().getTime());
				} else {
					time = String.valueOf(now);
				}
				values.put(MsgColumns.CREATE_DATE, time);

				if (Message.Type.groupchat.equals(message.getType())) {
					ContentValues values2 = new ContentValues();
					try {
						long time1 = Long.valueOf(time);
						time1 += 30000;
						values2.put(MultiChatRoomColumns.LAST_MSG_TIME, time1);
					} catch (Exception e) {
						// TODO: handle exception
					}
					mContext.getContentResolver().update(
							MultiChatRoomColumns.CONTENT_URI,
							values2,
							MultiChatRoomColumns.ROOM_JID
									+ "='"
									+ StringUtils.escapeUserResource(packet
											.getFrom()) + "' and "
									+ MultiChatRoomColumns.OWNER + "='"
									+ UserInfo.getUserInfo(mContext).getUser()
									+ "'", null);
				}

				YiIMUtils.insertMsg(mContext, values, imMessage, who,
						message.getFrom(), time);

				broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

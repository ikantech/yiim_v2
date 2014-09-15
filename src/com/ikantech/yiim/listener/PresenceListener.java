package com.ikantech.yiim.listener;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.provider.VCardManager.VCardColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class PresenceListener extends AbsPacketListener {
	// 定义PacketFilter，过滤非Presence的包
	public static final PacketFilter PACKET_FILTER = new PacketTypeFilter(
			Presence.class);

	public PresenceListener(Context context, XmppBinder xmppBinder) {
		super(context, xmppBinder);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void processPacket(Packet packet) {
		// TODO Auto-generated method stub
		try {

			Presence presence = (Presence) packet;
			ContentValues values = new ContentValues();
			values.put(ConversationColumns.USER, UserInfo.getUserInfo(mContext)
					.getUser());
			values.put(ConversationColumns.MSG, presence.getFrom());
			values.put(ConversationColumns.MSG_TYPE,
					ConversationType.MSG.getCode());

			Map<String, String> params = new HashMap<String, String>();
			params.put("from", packet.getFrom());

			if (presence.getType().equals(Presence.Type.subscribe)) {// 对方向自己发送好友请求
				// 如果对方已经是自己的好友，则直接同意好友请求
				try {
					RosterEntry rosterEntry = XmppConnectionUtils.getInstance()
							.getConnection().getRoster()
							.getEntry(packet.getFrom());
					if (rosterEntry != null) {
						Presence presence1 = new Presence(
								Presence.Type.subscribed);
						presence1.setTo(packet.getFrom());
						XmppConnectionUtils.getInstance().getConnection()
								.sendPacket(presence1);
						return;
					}
				} catch (Exception e) {
					// TODO: handle exception
				}

				Cursor cursor = mContext.getContentResolver()
						.query(ConversationColumns.CONTENT_URI,
								new String[] { ConversationColumns._ID },
								ConversationColumns.USER
										+ " like '"
										+ mXmppService.getXmppConnection()
												.getUser()
												.replaceAll("/.+$", "")
										+ "%' AND " + ConversationColumns.MSG
										+ "=? AND "
										+ ConversationColumns.MSG_TYPE
										+ "=? AND " + ConversationColumns.DEALT
										+ "=0",
								new String[] {
										presence.getFrom(),
										ConversationType.ENTRY_ADD_REQUEST
												.toString() }, null);
				if (cursor != null) {
					try {
						if (cursor.getCount() == 1) {
							// 如果数据库中已经有没有处理的相同请求，则更新一下修改时间即可
							cursor.moveToFirst();

							Uri uri = ContentUris.withAppendedId(
									ConversationColumns.CONTENT_URI,
									cursor.getLong(0));
							values = new ContentValues();
							values.put(ConversationColumns.MODIFIED_DATE,
									Long.valueOf(System.currentTimeMillis()));
							mContext.getContentResolver().update(uri, values,
									null, null);
							// 通知重新加载好友列表
							broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
							// broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES,
							// null);
							return;
						}
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						cursor.close();
						cursor = null;
					}
				}
				// 向数据库中插入一条记录
				values.put(ConversationColumns.SUB_MSG, mContext.getResources()
						.getString(R.string.str_entry_add_request));
				values.put(ConversationColumns.MSG_TYPE,
						ConversationType.ENTRY_ADD_REQUEST.getCode());
				values.put(ConversationColumns.DEALT, 0);
				mContext.getContentResolver().insert(
						ConversationColumns.CONTENT_URI, values);

				// 通知重新加载好友列表
				broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
				// broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
			} else if (presence.getType() == Presence.Type.unsubscribe) {// 对方将自己删除
				Presence response = new Presence(Presence.Type.unsubscribed);
				response.setTo(presence.getFrom());
				mXmppService.getXmppConnection().sendPacket(response);

				// 删除数据库中，残留的同用户的未处理的好友添加请求。
				Cursor cursor = mContext.getContentResolver()
						.query(ConversationColumns.CONTENT_URI,
								new String[] { ConversationColumns._ID },
								ConversationColumns.USER
										+ " like '"
										+ mXmppService.getXmppConnection()
												.getUser()
												.replaceAll("/.+$", "")
										+ "%' AND " + ConversationColumns.MSG
										+ "=? AND "
										+ ConversationColumns.MSG_TYPE
										+ "=? AND " + ConversationColumns.DEALT
										+ "=0",
								new String[] {
										presence.getFrom(),
										ConversationType.ENTRY_ADD_REQUEST
												.toString() }, null);
				if (cursor != null) {
					try {
						cursor.moveToFirst();
						do {
							Uri uri = ContentUris.withAppendedId(
									ConversationColumns.CONTENT_URI,
									cursor.getLong(0));
							mContext.getContentResolver().delete(uri, null,
									null);
						} while (cursor.moveToNext());
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						// 用完cursor，别忘了关，否则会报异常的
						cursor.close();
						cursor = null;
					}
				}
				// 更新好友关系类型
				YiIMUtils.updateRosterType(mContext, packet.getFrom(),
						ItemType.none);

				// 向数据库中插入一条记录
				insertMsg(packet.getFrom(), packet.getTo(), mContext
						.getResources().getString(R.string.str_entry_delete));

				// 通知重新加载好友列表
				broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
				broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
			} else if (presence.getType() == Presence.Type.subscribed) {// 对方同意好友添加请求
				// 向数据库中插入一条记录
				insertMsg(packet.getFrom(), packet.getTo(), mContext
						.getResources()
						.getString(R.string.str_entry_add_accept));

				// 更新好友关系类型
				YiIMUtils.updateRosterType(mContext, packet.getFrom(),
						ItemType.from);
				
				YiIMUtils.updatePresenceType(mContext, presence.getFrom());

				// 通知重新加载好友列表
				broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
				// broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
			} else if (presence.getType() == Presence.Type.unsubscribed) { // 对方拒绝添加为好友
				RosterEntry entry = mXmppService.getXmppConnection()
						.getRoster().getEntry(presence.getFrom());
				if (entry != null && entry.getType().equals(ItemType.from)) {
					// 向数据库中插入一条记录
					insertMsg(
							packet.getFrom(),
							packet.getTo(),
							mContext.getResources().getString(
									R.string.str_entry_add_refuse));

					// 通知重新加载好友列表
					broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
					// broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
				}
			} else if (presence.getType() == Presence.Type.available
					|| presence.getType() == Type.unavailable) {// 好友在线状态改变
				YiLog.getInstance().i(" presence type %s", presence.getType());
				if (!presence.getFrom().startsWith(
						UserInfo.getUserInfo(mContext).getUserName())) {
					ContentValues values2 = new ContentValues();
					if (presence.getType() == Type.unavailable) {
						values2.put(VCardColumns.PRESENCE, "unavailable");
					} else {
						values2.put(VCardColumns.PRESENCE, "online");
					}
					mContext.getContentResolver().update(
							VCardColumns.CONTENT_URI,
							values2,
							VCardColumns.USERID
									+ "='"
									+ StringUtils.escapeUserResource(presence
											.getFrom()) + "'", null);
					broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void insertMsg(String from, String to, String msg) {
		try {
			ContentValues msgValues = new ContentValues();
			msgValues.put(MsgColumns.SENDER, from);
			msgValues.put(MsgColumns.RECEIVER, to);

			String time = String.valueOf(System.currentTimeMillis());
			msgValues.put(MsgColumns.CREATE_DATE, time);

			YiIMMessage message = new YiIMMessage();
			message.setBody(msg);

			msgValues.put(MsgColumns.CONTENT, message.toString());

			YiIMUtils.insertMsg(mContext, msgValues, message, from, from, time);

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

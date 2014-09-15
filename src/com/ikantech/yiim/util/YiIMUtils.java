package com.ikantech.yiim.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;

public class YiIMUtils {
	private YiIMUtils() {

	}

	public static boolean isMultChat(String jid) {
		if (jid != null && jid.contains("conference")) {
			return true;
		}
		return false;
	}

	public static void deleteChatRecord(Context context, String user)
			throws Exception {
		String currentUser = UserInfo.getUserInfo(context).getUser();
		user = StringUtils.escapeUserResource(user);

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					MsgColumns.CONTENT_URI,
					new String[] { MsgColumns._ID, MsgColumns.CONTENT },
					"(" + MsgColumns.SENDER + " like '" + currentUser
							+ "%' and " + MsgColumns.RECEIVER + " like '"
							+ user + "%') or (" + MsgColumns.SENDER + " like '"
							+ user + "%' and " + MsgColumns.RECEIVER
							+ " like '" + currentUser + "%')", null, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					YiIMMessage message = YiIMMessage.fromString(cursor
							.getString(1));
					if (message.getType().equals(YiIMMessage.MsgType.AUDIO)
							|| message.getType().equals(
									YiIMMessage.MsgType.IMAGE)
							|| message.getType().equals(
									YiIMMessage.MsgType.VIDEO)
							|| message.getType().equals(
									YiIMMessage.MsgType.FILE)) {
						File file = new File(message.getBody());
						if (file.exists()
								&& file.getAbsolutePath().startsWith(
										FileUtils.getInstance()
												.getStoreRootPath())) {
							file.delete();
						}
					}
				} while (cursor.moveToNext());
			}
			context.getContentResolver().delete(
					MsgColumns.CONTENT_URI,
					"(" + MsgColumns.SENDER + " like '" + currentUser
							+ "%' and " + MsgColumns.RECEIVER + " like '"
							+ user + "%') or (" + MsgColumns.SENDER + " like '"
							+ user + "%' and " + MsgColumns.RECEIVER
							+ " like '" + currentUser + "%')", null);
		} catch (Exception e) {
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static void deleteConversation(Context context, String user)
			throws Exception {
		String currentUser = UserInfo.getUserInfo(context).getUser();
		user = StringUtils.escapeUserResource(user);

		Cursor cursor = null;
		try {
			// 删除好友请求
			context.getContentResolver().delete(
					ConversationColumns.CONTENT_URI,
					ConversationColumns.MSG + " like '" + user + "%' and "
							+ ConversationColumns.USER + " like '"
							+ currentUser + "%'", null);
		} catch (Exception e) {
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static void updatePresenceType(Context context, String user) {
		String currentUser = UserInfo.getUserInfo(context).getUser();
		user = StringUtils.escapeUserResource(user);

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					RosterColumns.CONTENT_URI,
					new String[] { RosterColumns._ID },
					RosterColumns.USERID + "='" + user + "' and "
							+ RosterColumns.OWNER + "='" + currentUser + "'",
					null, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				XMPPConnection connection = XmppConnectionUtils.getInstance()
						.getConnection();
				if (connection != null && connection.isConnected()
						&& connection.isAuthenticated()) {
					RosterEntry rosterEntry = connection.getRoster().getEntry(
							user);
					if (rosterEntry != null) {
						do {
							ContentValues values = new ContentValues();
							values.put(RosterColumns.ROSTER_TYPE, rosterEntry
									.getType().toString());
							context.getContentResolver().update(
									ContentUris.withAppendedId(
											RosterColumns.CONTENT_URI,
											cursor.getLong(0)), values, null,
									null);
						} while (cursor.moveToNext());
					}
				}
			}
		} catch (Exception e) {

		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static int addGroup(Context context, String groupName) {
		String owner = UserInfo.getUserInfo(context).getUser();
		Cursor groupCursor = null;
		try {
			groupCursor = context.getContentResolver().query(
					RosterGroupColumns.CONTENT_URI,
					new String[] { RosterGroupColumns._ID },
					RosterGroupColumns.NAME + "='" + groupName + "' and "
							+ RosterGroupColumns.OWNER + "='" + owner + "'",
					null, null);
			if (groupCursor != null && groupCursor.getCount() != 0) {
				return -1;
			}

			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection != null && connection.isConnected()
					&& connection.isAuthenticated()) {
				connection.getRoster().createGroup(groupName);

				ContentValues values = new ContentValues();
				values.put(RosterGroupColumns.NAME, groupName);
				values.put(RosterGroupColumns.OWNER, owner);

				context.getContentResolver().insert(
						RosterGroupColumns.CONTENT_URI, values);
				return 0;
			} else {
				return -2;
			}
		} catch (Exception e) {
			return -2;
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
				groupCursor = null;
			}
		}
	}

	public static int moveToGroup(Context context, long rosterId,
			long fromGroupId, long toGroupId) {
		Cursor entryCursor = null;
		Cursor fromGroupCursor = null;
		Cursor toGroupCursor = null;
		try {
			entryCursor = context.getContentResolver().query(
					ContentUris.withAppendedId(RosterColumns.CONTENT_URI,
							rosterId), new String[] { RosterColumns.USERID },
					null, null, null);

			if (entryCursor == null || entryCursor.getCount() != 1) {
				return -1;
			}
			entryCursor.moveToFirst();

			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				return -1;
			}

			Roster roster = connection.getRoster();
			RosterEntry rosterEntry = roster.getEntry(entryCursor.getString(0));
			if (rosterEntry == null) {
				return -1;
			}

			fromGroupCursor = context.getContentResolver().query(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							fromGroupId),
					new String[] { RosterGroupColumns.NAME }, null, null, null);

			if (fromGroupCursor == null || fromGroupCursor.getCount() != 1) {
				return -1;
			}
			fromGroupCursor.moveToFirst();

			toGroupCursor = context.getContentResolver().query(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							toGroupId),
					new String[] { RosterGroupColumns.NAME }, null, null, null);

			if (toGroupCursor == null || toGroupCursor.getCount() != 1) {
				return -1;
			}
			toGroupCursor.moveToFirst();

			String fromGroupName = fromGroupCursor.getString(0);
			String toGroupName = toGroupCursor.getString(0);

			if ("unfiled".equals(fromGroupName)) {
				RosterGroup toRosterGroup = roster.getGroup(toGroupName);
				if (toRosterGroup != null) {
					toRosterGroup.addEntry(rosterEntry);
				} else {
					return -1;
				}
			} else if ("unfiled".equals(toGroupName)) {
				RosterGroup fromRosterGroup = roster.getGroup(fromGroupName);
				if (fromRosterGroup != null) {
					fromRosterGroup.removeEntry(rosterEntry);
				} else {
					return -1;
				}
			} else {
				RosterGroup fromRosterGroup = roster.getGroup(fromGroupName);
				RosterGroup toRosterGroup = roster.getGroup(toGroupName);
				if (fromRosterGroup == null || toRosterGroup == null) {
					return -1;
				}
				fromRosterGroup.removeEntry(rosterEntry);
				toRosterGroup.addEntry(rosterEntry);
			}

			ContentValues values = new ContentValues();
			values.put(RosterColumns.GROUP_ID, toGroupId);
			context.getContentResolver().update(
					ContentUris.withAppendedId(RosterColumns.CONTENT_URI,
							rosterId), values, null, null);

			return 0;
		} catch (Exception e) {
			return -1;
		} finally {
			if (entryCursor != null) {
				entryCursor.close();
				entryCursor = null;
			}

			if (fromGroupCursor != null) {
				fromGroupCursor.close();
				fromGroupCursor = null;
			}

			if (toGroupCursor != null) {
				toGroupCursor.close();
				toGroupCursor = null;
			}
		}
	}

	public static int deleteGroup(Context context, long groupId) {
		String owner = UserInfo.getUserInfo(context).getUser();

		Cursor groupCursor = null;
		Cursor entriesCursor = null;
		Cursor defaultGroupCursor = null;
		try {
			groupCursor = context.getContentResolver().query(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							groupId), new String[] { RosterGroupColumns.NAME },
					null, null, null);
			if (groupCursor == null || groupCursor.getCount() != 1) {
				return -1;
			}
			groupCursor.moveToFirst();
			String groupName = groupCursor.getString(0);
			if ("unfiled".equals(groupName)) {
				return -2;
			}

			defaultGroupCursor = context.getContentResolver().query(
					RosterGroupColumns.CONTENT_URI,
					new String[] { RosterGroupColumns._ID },
					RosterGroupColumns.NAME + "='unfiled' and "
							+ RosterGroupColumns.OWNER + "='" + owner + "'",
					null, null);
			if (defaultGroupCursor == null
					|| defaultGroupCursor.getCount() != 1) {
				return -1;
			}
			defaultGroupCursor.moveToFirst();
			int defaultGroupId = defaultGroupCursor.getInt(0);

			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				return -1;
			}

			RosterGroup rosterGroup = connection.getRoster()
					.getGroup(groupName);
			if (rosterGroup == null) {
				return -1;
			}

			Collection<RosterEntry> entries = rosterGroup.getEntries();
			if (entries != null && entries.size() > 0) {
				for (RosterEntry rosterEntry : entries) {
					rosterGroup.removeEntry(rosterEntry);
				}
			}

			entriesCursor = context.getContentResolver().query(
					RosterColumns.CONTENT_URI,
					new String[] { RosterColumns._ID },
					RosterColumns.GROUP_ID + "=" + groupId, null, null);
			List<Integer> mIntegers = new ArrayList<Integer>();
			if (entriesCursor != null && entriesCursor.getCount() > 0) {
				entriesCursor.moveToFirst();
				do {
					mIntegers.add(entriesCursor.getInt(0));
				} while (entriesCursor.moveToNext());
			}

			for (Integer integer : mIntegers) {
				ContentValues values = new ContentValues();
				values.put(RosterColumns.GROUP_ID, defaultGroupId);
				context.getContentResolver().update(
						ContentUris.withAppendedId(RosterColumns.CONTENT_URI,
								integer), values, null, null);
			}

			context.getContentResolver().delete(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							groupId), null, null);
			return 0;
		} catch (Exception e) {
			return -1;
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
				groupCursor = null;
			}

			if (entriesCursor != null) {
				entriesCursor.close();
				entriesCursor = null;
			}

			if (defaultGroupCursor != null) {
				defaultGroupCursor.close();
				defaultGroupCursor = null;
			}
		}
	}

	public static int renameGroup(Context context, long groupId, String newName) {
		String owner = UserInfo.getUserInfo(context).getUser();

		Cursor groupCursor = null;
		Cursor defaultGroupCursor = null;
		try {
			groupCursor = context.getContentResolver().query(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							groupId), new String[] { RosterGroupColumns.NAME },
					null, null, null);
			if (groupCursor == null || groupCursor.getCount() != 1) {
				return -1;
			}
			groupCursor.moveToFirst();
			String groupName = groupCursor.getString(0);
			if ("unfiled".equals(groupName)) {
				return -2;
			}

			defaultGroupCursor = context.getContentResolver().query(
					RosterGroupColumns.CONTENT_URI,
					new String[] { RosterGroupColumns._ID },
					RosterGroupColumns.NAME + "='" + newName + "' and "
							+ RosterGroupColumns.OWNER + "='" + owner + "'",
					null, null);
			if (defaultGroupCursor != null
					&& defaultGroupCursor.getCount() == 1) {
				return -3;
			}

			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				return -1;
			}

			Roster roster = connection.getRoster();
			RosterGroup rosterGroup = roster.getGroup(groupName);
			if (rosterGroup == null) {
				return -1;
			}

			Collection<RosterEntry> entries = rosterGroup.getEntries();
			if (entries != null && entries.size() > 0) {
				// 添加至新组
				RosterGroup toGroup = roster.getGroup(newName);
				if (toGroup == null) {
					toGroup = roster.createGroup(newName);
				}

				for (RosterEntry rosterEntry : entries) {
					toGroup.addEntry(rosterEntry);
				}

				// 从原组中移除
				for (RosterEntry rosterEntry : entries) {
					rosterGroup.removeEntry(rosterEntry);
				}
			}

			ContentValues values = new ContentValues();
			values.put(RosterGroupColumns.NAME, newName);
			context.getContentResolver().update(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							groupId), values, null, null);

			return 0;
		} catch (Exception e) {
			return -1;
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
				groupCursor = null;
			}

			if (defaultGroupCursor != null) {
				defaultGroupCursor.close();
				defaultGroupCursor = null;
			}
		}
	}

	/**
	 * 创建房间
	 * 
	 * @param roomName
	 *            房间名称
	 */
	public static MultiUserChat createMultiUserChat(Context context,
			String user, String roomName, String roomDesc, String password)
			throws Exception {
		XMPPConnection connection = XmppConnectionUtils.getInstance()
				.getRawConnection();
		if (connection == null || !connection.isConnected()
				|| !connection.isAuthenticated())
			throw new Exception("no connection");

		MultiUserChat muc = null;
		String roomJid = roomName + "@conference."
				+ XmppConnectionUtils.getXmppHost();
		try {
			try {
				if (isRoomExist(roomJid)) {
					throw new Exception("room already exist");
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			MultiChatDesc desc = MultiChatDesc.fromString(roomDesc);

			// 创建一个MultiUserChat
			muc = new MultiUserChat(connection, roomJid);
			// 创建聊天室
			muc.create(StringUtils.escapeUserHost(user));
			// 获得聊天室的配置表单
			Form form = muc.getConfigurationForm();
			// 根据原始表单创建一个要提交的新表单。
			Form submitForm = form.createAnswerForm();
			// 向要提交的表单添加默认答复
			for (Iterator<FormField> fields = form.getFields(); fields
					.hasNext();) {
				FormField field = (FormField) fields.next();
				if (!FormField.TYPE_HIDDEN.equals(field.getType())
						&& field.getVariable() != null) {
					// 设置默认值作为答复
					submitForm.setDefaultAnswer(field.getVariable());
				}
			}
			// 设置聊天室的新拥有者
			// List<String> owners = new ArrayList<String>();
			// owners.add(user);// 用户JID
			// submitForm.setAnswer("muc#roomconfig_roomowners", owners);
			// 设置聊天室是持久聊天室，即将要被保存下来
			submitForm.setAnswer("muc#roomconfig_persistentroom", false);
			// 房间仅对成员开放
			submitForm.setAnswer("muc#roomconfig_membersonly", false);
			// 允许占有者邀请其他人
			submitForm.setAnswer("muc#roomconfig_allowinvites", true);
			if (!YiUtils.isStringInvalid(password)) {
				// 进入是否需要密码
				submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",
						true);
				// 设置进入密码
				submitForm.setAnswer("muc#roomconfig_roomsecret", password);
			}
			// 房间描述
			submitForm.setAnswer("muc#roomconfig_roomdesc", roomDesc);
			// 能够发现占有者真实 JID 的角色
			// submitForm.setAnswer("muc#roomconfig_whois", "anyone");
			// 登录房间对话
			// submitForm.setAnswer("muc#roomconfig_enablelogging", true);
			// 仅允许注册的昵称登录
			// submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
			// 允许使用者修改昵称
			// submitForm.setAnswer("x-muc#roomconfig_canchangenick", true);
			// 允许用户注册房间
			// submitForm.setAnswer("x-muc#roomconfig_registration", false);
			// 发送已完成的表单（有默认值）到服务器来配置聊天室
			muc.sendConfigurationForm(submitForm);

			localJoinMultiUserChat(context, roomJid, desc.getName(), roomDesc);
			return muc;
		} catch (Exception e) {
			// if (muc != null) {
			// deleteChatRecord(context, roomJid);
			// deleteConversation(context, roomJid);
			// }
			throw e;
		}
	}

	public static boolean isRoomExist(String roomJid) throws Exception {
		XMPPConnection connection = XmppConnectionUtils.getInstance()
				.getRawConnection();
		if (connection == null || !connection.isAuthenticated()) {
			throw new Exception("no connections");
		}
		try {
			RoomInfo roomInfo = MultiUserChat.getRoomInfo(connection, roomJid);
			if (roomInfo != null) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 加入会议室
	 * 
	 * @param user
	 *            昵称
	 * @param password
	 *            会议室密码
	 * @param roomsName
	 *            会议室名
	 */
	public static MultiUserChat joinMultiUserChat(Context context, String user,
			String roomJid, String password) throws Exception {
		XMPPConnection connection = XmppConnectionUtils.getInstance()
				.getRawConnection();
		if (connection == null || !connection.isConnected()
				|| !connection.isAuthenticated())
			throw new Exception("connection not ready");

		Cursor cursor = null;
		try {
			// 使用XMPPConnection创建一个MultiUserChat窗口
			MultiUserChat muc = new MultiUserChat(connection, roomJid);
			// 聊天室服务将会决定要接受的历史记录数量
			DiscussionHistory history = new DiscussionHistory();

			cursor = context.getContentResolver().query(
					MultiChatRoomColumns.CONTENT_URI,
					new String[] { MultiChatRoomColumns.LAST_MSG_TIME },
					MultiChatRoomColumns.ROOM_JID + "='" + roomJid + "' and "
							+ MultiChatRoomColumns.OWNER + "='"
							+ UserInfo.getUserInfo(context).getUser() + "'",
					null, null);
			if (cursor != null && cursor.getCount() == 1) {
				cursor.moveToFirst();
				history.setSince(new Date(cursor.getLong(0)));
			} else {
				history.setMaxStanzas(15);
			}
			// 用户加入聊天室
			muc.join(StringUtils.escapeUserHost(user), password, history,
					SmackConfiguration.getPacketReplyTimeout());
			return muc;
		} catch (Exception e) {
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static String getMultUserChatDesc(Context context, String roomJid) {
		String owner = UserInfo.getUserInfo(context).getUser();

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					MultiChatRoomColumns.CONTENT_URI,
					new String[] { MultiChatRoomColumns.ROOM_DESC },
					MultiChatRoomColumns.ROOM_JID + "='" + roomJid + "' and "
							+ MultiChatRoomColumns.OWNER + "='" + owner + "'",
					null, null);
			if (cursor != null && cursor.getCount() == 1) {
				cursor.moveToFirst();
				return cursor.getString(0);
			}
			return "";
		} catch (Exception e) {
			return "";
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static boolean isMultiChatJoined(Context context, String roomJid) {
		String owner = UserInfo.getUserInfo(context).getUser();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					MultiChatRoomColumns.CONTENT_URI,
					new String[] { MultiChatRoomColumns._ID },
					MultiChatRoomColumns.ROOM_JID + "='" + roomJid + "' and "
							+ MultiChatRoomColumns.OWNER + "='" + owner + "'",
					null, null);
			if (cursor != null && cursor.getCount() == 1) {
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		return false;
	}

	public static int localJoinMultiUserChat(Context context, String roomJid,
			String roomName, String roomDesc) {
		if (roomJid == null) {
			return -1;
		}

		String owner = UserInfo.getUserInfo(context).getUser();

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					MultiChatRoomColumns.CONTENT_URI,
					new String[] { MultiChatRoomColumns._ID },
					MultiChatRoomColumns.ROOM_JID + "='" + roomJid + "' and "
							+ MultiChatRoomColumns.OWNER + "='" + owner + "'",
					null, null);
			if (cursor != null && cursor.getCount() > 0) {
				return -2;
			}

			ContentValues values = new ContentValues();
			values.put(MultiChatRoomColumns.ROOM_JID, roomJid);
			if (!YiUtils.isStringInvalid(roomName)) {
				values.put(MultiChatRoomColumns.ROOM_NAME, roomName);
			}
			if (!YiUtils.isStringInvalid(roomDesc)) {
				values.put(MultiChatRoomColumns.ROOM_DESC, roomDesc);
			}
			values.put(MultiChatRoomColumns.OWNER, owner);

			context.getContentResolver().insert(
					MultiChatRoomColumns.CONTENT_URI, values);

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	public static void insertMsg(Context context, ContentValues msgValues,
			YiIMMessage message, String who, String from, String time) {
		try {
			context.getContentResolver().insert(MsgColumns.CONTENT_URI,
					msgValues);

			// 更新会话，如果已经有一条记录，则更新当前消息及未查看消息数量
			Cursor cursor = context.getContentResolver().query(
					ConversationColumns.CONTENT_URI,
					new String[] { ConversationColumns._ID,
							ConversationColumns.DEALT },
					ConversationColumns.USER + " like '"
							+ UserInfo.getUserInfo(context).getUser()
							+ "%' AND " + ConversationColumns.MSG + " like '"
							+ who.replaceAll("/.+$", "") + "%' and "
							+ ConversationColumns.MSG_TYPE + "=?",
					new String[] { ConversationType.CHAT_RECORD.toString() },
					null);

			if (cursor != null) {
				try {
					if (cursor.getCount() == 1) {
						cursor.moveToFirst();
						ContentValues values = new ContentValues();
						if (from.startsWith(UserInfo.getUserInfo(context)
								.getUserName())
								|| (StringUtils.getJidResouce(from)
										.equals(UserInfo.getUserInfo(context)
												.getUserName()))) {
							values.put(ConversationColumns.DEALT, 0);
						} else {
							values.put(ConversationColumns.DEALT,
									cursor.getInt(1) + 1);
						}
						values.put(ConversationColumns.MSG_DATE, time);
						values.put(ConversationColumns.MODIFIED_DATE,
								System.currentTimeMillis());
						if (MsgType.AUDIO.equals(message.getType())) {
							values.put(
									ConversationColumns.SUB_MSG,
									context.getResources().getString(
											R.string.str_msg_audio));
						} else if (MsgType.BIG_EMOTION
								.equals(message.getType())) {
							values.put(
									ConversationColumns.SUB_MSG,
									context.getResources().getString(
											R.string.str_msg_big_emotion));
						} else if (MsgType.IMAGE.equals(message.getType())) {
							values.put(
									ConversationColumns.SUB_MSG,
									context.getResources().getString(
											R.string.str_msg_image));
						} else if (MsgType.VIDEO.equals(message.getType())) {
							values.put(
									ConversationColumns.SUB_MSG,
									context.getResources().getString(
											R.string.str_msg_video));
						} else {
							values.put(ConversationColumns.SUB_MSG,
									message.getBody());
						}
						context.getContentResolver().update(
								ContentUris.withAppendedId(
										ConversationColumns.CONTENT_URI,
										cursor.getLong(0)), values, null, null);
					} else {
						YiIMUtils.insertNewChatRecordConversation(context,
								message, who, from);
					}
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					cursor.close();
					cursor = null;
				}
			} else {
				YiIMUtils.insertNewChatRecordConversation(context, message,
						who, from);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static void insertNewChatRecordConversation(Context context,
			YiIMMessage message, String who, String from) {
		try {
			ContentValues values = new ContentValues();
			values.put(ConversationColumns.USER, UserInfo.getUserInfo(context)
					.getUser());
			values.put(ConversationColumns.MSG, who);
			if (MsgType.AUDIO.equals(message.getType())) {
				values.put(ConversationColumns.SUB_MSG, context.getResources()
						.getString(R.string.str_msg_audio));
			} else if (MsgType.BIG_EMOTION.equals(message.getType())) {
				values.put(ConversationColumns.SUB_MSG, context.getResources()
						.getString(R.string.str_msg_big_emotion));
			} else {
				values.put(ConversationColumns.SUB_MSG, message.getBody());
			}

			String time = message.getParams().get("time");
			if (time != null) {
				values.put(ConversationColumns.MSG_DATE, time);
			}

			values.put(ConversationColumns.MSG_TYPE,
					ConversationType.CHAT_RECORD.toString());
			if (from.startsWith(UserInfo.getUserInfo(context).getUser())
					|| (from.contains("conference") && StringUtils
							.getJidResouce(from)
							.equals(UserInfo.getUserInfo(context).getUserName()))) {
				values.put(ConversationColumns.DEALT, 0);
			}
			context.getContentResolver().insert(
					ConversationColumns.CONTENT_URI, values);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static void updateRosterType(Context context, String who,
			ItemType type) {
		try {
			ContentValues values = new ContentValues();
			values.put(RosterColumns.ROSTER_TYPE, type.toString());

			context.getContentResolver().update(
					RosterColumns.CONTENT_URI,
					values,
					RosterColumns.USERID + "='"
							+ StringUtils.escapeUserResource(who) + "' and "
							+ RosterColumns.OWNER + "='"
							+ UserInfo.getUserInfo(context).getUser() + "'",
					null);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static void broadcast(Context context, String what,
			Map<String, String> params) {
		Intent intent = new Intent(what);
		if (params != null) {
			Set<String> keys = params.keySet();
			for (String string : keys) {
				intent.putExtra(string, params.get(string));
			}
		}
		context.sendBroadcast(intent);
	}
}

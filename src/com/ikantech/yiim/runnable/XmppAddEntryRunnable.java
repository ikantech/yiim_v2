package com.ikantech.yiim.runnable;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;

public class XmppAddEntryRunnable extends XmppRunnable {

	private Context mContext;
	private String mUser;
	private String mName;
	private String mGroupName;
	private boolean isRequest;

	public XmppAddEntryRunnable(Context context, String user,
			XmppListener listener) {
		super(listener);
		mContext = context;
		mUser = user;
		mName = null;
		mGroupName = null;
		isRequest = true;
	}

	@Override
	public XmppResult execute() {
		XmppResult result = createResult();

		try {
			Connection connection = XmppConnectionUtils.getInstance()
					.getConnection();

			if (!isRequest) {
				Presence presence = new Presence(Presence.Type.subscribed);
				presence.setTo(mUser);
				connection.sendPacket(presence);
			}

			// 添加好友到好友列表
			Roster roster = connection.getRoster();
			if (YiUtils.isStringInvalid(mGroupName)) {
				roster.createEntry(mUser, mName, null);
			} else {
				roster.createEntry(mUser, mName, new String[] { mGroupName });
			}

			String owner = UserInfo.getUserInfo(mContext).getUser();
			Cursor groupCursor = null;
			Cursor entryCursor = null;
			try {
				if (YiUtils.isStringInvalid(mGroupName)) {
					mGroupName = "unfiled";
				}

				groupCursor = mContext.getContentResolver().query(
						RosterGroupColumns.CONTENT_URI,
						new String[] { RosterGroupColumns._ID },
						RosterGroupColumns.NAME + " = '" + mGroupName
								+ "' and " + RosterGroupColumns.OWNER + "='"
								+ owner + "'", null, null);
				long groupId = -1;
				if (groupCursor != null && groupCursor.getCount() == 1) {
					groupCursor.moveToFirst();
					groupId = groupCursor.getLong(0);
				} else {
					ContentValues values = new ContentValues();
					values.put(RosterGroupColumns.NAME, mGroupName);
					values.put(RosterGroupColumns.OWNER, owner);
					Uri uri = mContext.getContentResolver().insert(
							RosterGroupColumns.CONTENT_URI, values);
					groupId = ContentUris.parseId(uri);
				}

				entryCursor = mContext.getContentResolver().query(
						RosterColumns.CONTENT_URI,
						new String[] { RosterColumns._ID },
						RosterColumns.GROUP_ID + "=" + groupId + " and "
								+ RosterColumns.USERID + "='" + mUser
								+ "' and " + RosterColumns.OWNER + "='" + owner
								+ "'", null, null);
				if (entryCursor == null || entryCursor.getCount() == 0) {
					XmppVcard vCard = new XmppVcard(mContext);
					vCard.load(XmppConnectionUtils.getInstance()
							.getConnection(), mUser, true);

					ContentValues enValues = new ContentValues();
					enValues.put(RosterColumns.GROUP_ID, groupId);
					enValues.put(RosterColumns.USERID, mUser);
					enValues.put(RosterColumns.MEMO_NAME, mName);

					enValues.put(RosterColumns.ROSTER_TYPE,
							ItemType.none.toString());

					enValues.put(RosterColumns.OWNER, owner);

					mContext.getContentResolver().insert(
							RosterColumns.CONTENT_URI, enValues);

					// 通知更新好友列表
					Intent intent = new Intent(
							Const.NOTIFY_RELOAD_ROSTER_ENTRIES);
					mContext.sendBroadcast(intent);
				}
			} catch (Exception e) {
			} finally {
				if (groupCursor != null) {
					groupCursor.close();
					groupCursor = null;
				}

				if (entryCursor != null) {
					entryCursor.close();
					entryCursor = null;
				}
			}

			// 清除未处理的好友请求
			Cursor cursor = mContext.getContentResolver().query(
					ConversationColumns.CONTENT_URI,
					new String[] { ConversationColumns._ID },
					ConversationColumns.USER + " like '"
							+ connection.getUser().replaceAll("/.+$", "")
							+ "%' AND " + ConversationColumns.MSG + "=? AND "
							+ ConversationColumns.MSG_TYPE + "=? AND "
							+ ConversationColumns.DEALT + "=0",
					new String[] { mUser,
							ConversationType.ENTRY_ADD_REQUEST.toString() },
					null);
			if (cursor != null && cursor.getCount() > 0) {
				try {
					cursor.moveToFirst();
					do {
						ContentValues values = new ContentValues();
						values.put(ConversationColumns.DEALT, 1);

						mContext.getContentResolver().update(
								ContentUris.withAppendedId(
										ConversationColumns.CONTENT_URI,
										cursor.getLong(0)), values, null, null);
					} while (cursor.moveToNext());
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					cursor.close();
					cursor = null;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			result.obj = e.getMessage();
		}

		return result;
	}

	@Override
	protected XmppCmds getCmd() {
		return XmppCmds.XMPP_ADD_ENTRY;
	}

	public String getUser() {
		return mUser;
	}

	public void setUser(String user) {
		this.mUser = user;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getGroupName() {
		return mGroupName;
	}

	public void setGroupName(String groupName) {
		this.mGroupName = groupName;
	}

	public boolean isRequest() {
		return isRequest;
	}

	public void setRequest(boolean isRequest) {
		this.isRequest = isRequest;
	}
}

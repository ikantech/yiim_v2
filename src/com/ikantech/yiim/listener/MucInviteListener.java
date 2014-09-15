package com.ikantech.yiim.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.ikantech.yiim.R;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.StringUtils;

public class MucInviteListener implements InvitationListener {
	private Context mContext;
	private XmppBinder mXmppBinder;

	public MucInviteListener(Context context, XmppBinder xmppBinder) {
		mContext = context;
		mXmppBinder = xmppBinder;
	}

	@Override
	public void invitationReceived(Connection conn, String room,
			String inviter, String reason, String password, Message message) {
		ContentValues values = new ContentValues();

		String from = StringUtils.escapeUserResource(room) + "/"
				+ StringUtils.escapeUserHost(inviter);

		Map<String, String> params = new HashMap<String, String>();
		params.put("from", from);
		
		values.put(ConversationColumns.USER, UserInfo.getUserInfo(mContext)
				.getUser());
		values.put(ConversationColumns.MSG, from);

		Cursor cursor = mContext.getContentResolver().query(
				ConversationColumns.CONTENT_URI,
				new String[] { ConversationColumns._ID },
				ConversationColumns.USER + " like '"
						+ UserInfo.getUserInfo(mContext).getUser() + "%' AND "
						+ ConversationColumns.MSG + "=? AND "
						+ ConversationColumns.MSG_TYPE + "=? AND "
						+ ConversationColumns.DEALT + "=0",
				new String[] { from,
						ConversationType.ENTRY_ADD_REQUEST.toString() }, null);
		if (cursor != null) {
			try {
				if (cursor.getCount() == 1) {
					// 如果数据库中已经有没有处理的相同请求，则更新一下修改时间即可
					cursor.moveToFirst();

					Uri uri = ContentUris.withAppendedId(
							ConversationColumns.CONTENT_URI, cursor.getLong(0));
					values = new ContentValues();
					values.put(ConversationColumns.MODIFIED_DATE,
							Long.valueOf(System.currentTimeMillis()));
					mContext.getContentResolver().update(uri, values, null,
							null);
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
		values.put(
				ConversationColumns.SUB_MSG,
				mContext.getResources().getString(
						R.string.str_room_add_request,
						StringUtils.escapeUserHost(room)));
		values.put(ConversationColumns.MSG_TYPE,
				ConversationType.ENTRY_ADD_REQUEST.getCode());
		values.put(ConversationColumns.DEALT, 0);
		mContext.getContentResolver().insert(ConversationColumns.CONTENT_URI,
				values);

		// 通知重新加载好友列表
		broadcast(Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
		// broadcast(Const.NOTIFY_RELOAD_ROSTER_ENTRIES, null);
	}

	public void broadcast(String what, Map<String, String> params) {
		Intent intent = new Intent(what);
		if (params != null) {
			Set<String> keys = params.keySet();
			for (String string : keys) {
				intent.putExtra(string, params.get(string));
			}
		}
		mContext.sendBroadcast(intent);
	}
}

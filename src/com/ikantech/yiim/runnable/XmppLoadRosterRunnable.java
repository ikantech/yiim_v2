package com.ikantech.yiim.runnable;

import java.util.Collection;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.util.StringUtils;

public class XmppLoadRosterRunnable extends XmppRunnable {
	public static final String LOADROSTER_SUCCESS = "com.chyitech.yiim.ACTION_LOADROSTER_SUCCESS";
	public static final String LOADROSTER_FAILED = "com.chyitech.yiim.ACTION_LOADROSTER_FAILED";

	private static boolean mIsLoadingRoster = false;

	private Context mContext;

	public XmppLoadRosterRunnable(Context context, XmppListener listener) {
		super(listener);
		mContext = context;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// 另开一个线程，更新好友的时间比较长。
		YiLog.getInstance().i("XmppLoadRosterRunnable run");
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mIsLoadingRoster = true;
				XmppResult result = execute();
				mIsLoadingRoster = false;
				if (mListener != null) {
					mListener.onXmppResonpse(result);
				}
			}
		}, "LoadRoster").start();
	}

	@Override
	public XmppResult execute() {
		// TODO Auto-generated method stub
		XmppResult result = createResult();
		try {
			String owner = UserInfo.getUserInfo(mContext).getUser();

			Connection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				Intent intent = new Intent(LOADROSTER_FAILED);
				mContext.sendBroadcast(intent);
				return result;
			}

			Roster roster = connection.getRoster();
			Collection<RosterGroup> groups = roster.getGroups();
			mContext.getContentResolver().delete(
					RosterGroupColumns.CONTENT_URI,
					RosterGroupColumns.OWNER + "='" + owner + "'", null);
			mContext.getContentResolver().delete(RosterColumns.CONTENT_URI,
					RosterColumns.OWNER + "='" + owner + "'", null);

			// 获取分组
			for (RosterGroup rosterGroup : groups) {
				ContentValues values = new ContentValues();
				values.put(RosterGroupColumns.NAME, rosterGroup.getName());
				values.put(RosterGroupColumns.OWNER, owner);
				Uri uri = mContext.getContentResolver().insert(
						RosterGroupColumns.CONTENT_URI, values);
				long id = ContentUris.parseId(uri);

				// 获取分组好友
				Collection<RosterEntry> entries = rosterGroup.getEntries();
				YiLog.getInstance().i("group : %s entries count : %d", rosterGroup.getName(), entries.size());
				loadRosterEntry(id, entries, roster, owner);
			}

			ContentValues values = new ContentValues();
			values.put(RosterGroupColumns.NAME, "unfiled");
			values.put(RosterGroupColumns.OWNER, owner);
			Uri uri = mContext.getContentResolver().insert(
					RosterGroupColumns.CONTENT_URI, values);
			long id = ContentUris.parseId(uri);

			Collection<RosterEntry> entries = roster.getUnfiledEntries();
			
			YiLog.getInstance().i("group : %s entries count : %d", "unfiled", entries.size());
			
			loadRosterEntry(id, entries, roster, owner);

			result.status = Status.SUCCESS;

			Intent intent = new Intent(LOADROSTER_SUCCESS);
			mContext.sendBroadcast(intent);
		} catch (Exception e) {
			result.obj = e.getMessage();

			Intent intent = new Intent(LOADROSTER_FAILED);
			mContext.sendBroadcast(intent);
		}
		return result;
	}

	private void loadRosterEntry(long groupId, Collection<RosterEntry> entries,
			Roster roster, String owner) throws Exception {
		try {
			for (RosterEntry rosterEntry : entries) {
				String userId = StringUtils.escapeUserResource(rosterEntry
						.getUser());

				XmppVcard vCard = new XmppVcard(mContext);
				vCard.load(XmppConnectionUtils.getInstance().getConnection(),
						userId, true);

				ContentValues enValues = new ContentValues();
				enValues.put(RosterColumns.GROUP_ID, groupId);
				enValues.put(RosterColumns.USERID, userId);
				enValues.put(RosterColumns.MEMO_NAME, rosterEntry.getName());

				enValues.put(RosterColumns.ROSTER_TYPE, rosterEntry.getType()
						.toString());

				enValues.put(RosterColumns.OWNER, owner);

				mContext.getContentResolver().insert(RosterColumns.CONTENT_URI,
						enValues);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static boolean isLoadingRoster() {
		return mIsLoadingRoster;
	}

	public static void setLoadingRoster(boolean isLoadingRoster) {
		mIsLoadingRoster = isLoadingRoster;
	}

	@Override
	protected XmppCmds getCmd() {
		// TODO Auto-generated method stub
		return XmppCmds.XMPP_LOAD_ROSTER;
	}

}

package com.ikantech.yiim.runnable;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;

import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class XmppDeleteEntryRunnable extends XmppRunnable {
	private Context mContext;
	private String mUserId;
	private long mGroupId;
	private long mRosterId;

	public XmppDeleteEntryRunnable(Context context, String userId,
			long groupId, XmppListener listener) {
		super(listener);
		// TODO Auto-generated constructor stub
		mUserId = StringUtils.escapeUserResource(userId);
		mGroupId = groupId;
		mContext = context;
		mRosterId = -1;
	}

	public XmppDeleteEntryRunnable(Context context, long rosterId,
			XmppListener listener) {
		super(listener);
		mRosterId = rosterId;
		mUserId = null;
		mGroupId = -1;
		mContext = context;
	}

	@Override
	public XmppResult execute() {
		// TODO Auto-generated method stub
		XmppResult result = createResult();

		Cursor mCursor = null;
		try {
			// 删除关系表
			if (mRosterId != -1) {
				mCursor = mContext.getContentResolver().query(
						ContentUris.withAppendedId(RosterColumns.CONTENT_URI,
								mRosterId),
						new String[] { RosterColumns.USERID,
								RosterColumns.GROUP_ID }, null, null, null);
				if (mCursor != null && mCursor.getCount() == 1) {
					mCursor.moveToFirst();
					mUserId = mCursor.getString(0);
					mGroupId = mCursor.getInt(1);
				}
			}

			Roster roster = XmppConnectionUtils.getInstance().getConnection()
					.getRoster();
			RosterEntry rosterEntry = roster.getEntry(mUserId);
			if (rosterEntry != null) {
				roster.removeEntry(rosterEntry);
			}

			mContext.getContentResolver().delete(
					RosterColumns.CONTENT_URI,
					RosterColumns.USERID + "='"+mUserId+"' and " + RosterColumns.GROUP_ID
							+ "=" + mGroupId,
					null);

			// 删除本地聊天记录
			YiIMUtils.deleteChatRecord(mContext, mUserId);

			// 删除本地会话记录
			YiIMUtils.deleteConversation(mContext, mUserId);

			result.status = Status.SUCCESS;
		} catch (Exception e) {
			YiLog.getInstance().e(e, "delete friend failed.");
			result.obj = e.getMessage();
		} finally {
			if (mCursor != null) {
				mCursor.close();
				mCursor = null;
			}
		}

		return result;
	}

	@Override
	protected XmppCmds getCmd() {
		// TODO Auto-generated method stub
		return XmppCmds.XMPP_DELETE_ENTRY;
	}

}

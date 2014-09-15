package com.ikantech.yiim.runnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.RosterGroup;
import com.ikantech.yiim.entity.TabContactsModel;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.frag.FriendFragment;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.util.StringUtils;

public class LocalLoadRosterRunnable implements Runnable {
	private Handler mHandler = null;
	private Context mContext = null;

	private List<RosterGroup> mGroups2 = null;
	private Map<String, List<TabContactsModel>> mEntries2 = null;

	public LocalLoadRosterRunnable(Context context, Handler handler,
			List<RosterGroup> groups,
			Map<String, List<TabContactsModel>> entries) {
		mContext = context;
		mHandler = handler;
		mGroups2 = groups;
		mEntries2 = entries;
	}

	@Override
	public void run() {
		mGroups2.clear();
		mEntries2.clear();

		Cursor groupCursor = null;
		try {
			String owner = UserInfo.getUserInfo(mContext).getUser();
			groupCursor = mContext.getContentResolver().query(
					RosterGroupColumns.CONTENT_URI,
					new String[] { RosterGroupColumns._ID,
							RosterGroupColumns.NAME },
					RosterGroupColumns.OWNER + "='" + owner + "'", null, null);
			if (groupCursor != null && groupCursor.getCount() > 0) {
				groupCursor.moveToFirst();
				mGroups2.clear();
				do {
					RosterGroup rosterGroup = new RosterGroup();
					String group = groupCursor.getString(1);

					// 替换未分组的组名
					if ("unfiled".equals(group)) {
						group = mContext.getString(R.string.str_my_friend);
					}

					rosterGroup.setName(group);
					rosterGroup.setId(groupCursor.getInt(0));
					mGroups2.add(rosterGroup);

					Cursor entriesCursor = null;
					try {
						entriesCursor = mContext.getContentResolver().query(
								RosterColumns.CONTENT_URI,
								new String[] { RosterColumns._ID,
										RosterColumns.USERID },
								RosterColumns.GROUP_ID + "=? and "
										+ RosterColumns.OWNER + "='" + owner
										+ "'",
								new String[] { String.valueOf(groupCursor
										.getInt(0)) }, null);
						List<TabContactsModel> list = mEntries2.get(group);
						if (list == null) {
							list = new ArrayList<TabContactsModel>();
							mEntries2.put(group, list);
						} else {
							list.clear();
						}

						if (entriesCursor != null
								&& entriesCursor.getCount() > 0) {
							entriesCursor.moveToFirst();
							rosterGroup.setEntryCount(entriesCursor.getCount());
							do {
								TabContactsModel model = new TabContactsModel();
								model.setRosterId(entriesCursor.getInt(0));
								model.setUser(entriesCursor.getString(1));

								XmppVcard vcard = new XmppVcard(mContext);
								vcard.load(XmppConnectionUtils.getInstance()
										.getRawConnection(), model.getUser());

								// 更新在线状态
								vcard.updatePresence();

								// 获取在线状态
								String presence = vcard.getPresence();
								model.setPresence(presence);
								if ("online".equals(presence)) {
									model.setSubMsg("["
											+ mContext
													.getString(R.string.str_online)
											+ "]");
									rosterGroup.addOnlineCount();
								} else {
									model.setSubMsg("["
											+ mContext
													.getString(R.string.str_unavailable)
											+ "]");
								}

								String rosterType = vcard.getRosterType();
								model.setRosterType(rosterType);
								if (ItemType.none.toString().equals(rosterType)) {
									model.setSubMsg(model.getSubMsg()
											+ "["
											+ mContext
													.getString(R.string.str_not_certified)
											+ "]");
								}

								// 加载用户的个性签名
								String sign = vcard.getSign();
								if (!YiUtils.isStringInvalid(sign)) {
									model.setSubMsg(model.getSubMsg() + sign);
								}

								model.setMsg(vcard.getDisplayName());

								list.add(model);
							} while (entriesCursor.moveToNext());
							Collections.sort(list, new FriendListSort());
						}
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						if (entriesCursor != null) {
							entriesCursor.close();
							entriesCursor = null;
						}
					}
				} while (groupCursor.moveToNext());
			}

			Message message = mHandler
					.obtainMessage(FriendFragment.MSG_UPDATE_FRIENDS_LIST);
			message.sendToTarget();
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
				groupCursor = null;
			}
		}
	}

	private class FriendListSort implements Comparator<TabContactsModel> {
		@Override
		public int compare(TabContactsModel lhs, TabContactsModel rhs) {
			// TODO Auto-generated method
			// stub
			int l = 0;
			int r = 1;
			try {
				l = Integer.valueOf(StringUtils.escapeUserHost(lhs.getUser()));
				r = Integer.valueOf(StringUtils.escapeUserHost(rhs.getUser()));
			} catch (Exception e) {
				// TODO: handle exception
			}
			int f = r - l;
			if ("online".equals(lhs.getPresence())
					&& !lhs.getPresence().equals(rhs)) {
				return -1;
			} else if ("online".equals(rhs.getPresence())
					&& !rhs.getPresence().equals(lhs)) {
				return 1;
			} else if (lhs.getPresence().equals(rhs)) {
				return (f > 0 ? -1 : 1);
			}
			return 0;
		}
	}
}

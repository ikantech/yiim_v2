package com.ikantech.yiim.frag.friend;

import android.content.Intent;
import android.database.Cursor;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.ikantech.support.util.YiLog;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.MultiRoomAdapter;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.frag.FriendFragment;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.ChatActivity;
import com.ikantech.yiim.ui.MultiRoomDiscoverActivity;
import com.ikantech.yiim.ui.RoomInfoActivity;
import com.ikantech.yiim.util.StringUtils;

public class GroupManager {
	private View mRootView;

	private ListView mListView;
	private MultiRoomAdapter mAdapter;

	private View mGroupsListBtn;

	private YiFragment mYiFragment;

	private Cursor mCursor;

	public GroupManager(YiFragment yiFragment, View rootView) {
		mYiFragment = yiFragment;
		mRootView = rootView;
	}

	public void onCreateView() {
		mListView = (ListView) mRootView
				.findViewById(R.id.tab_groups_joined_list);

		mGroupsListBtn = mRootView.findViewById(R.id.tab_groups_list_btn);
		mGroupsListBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(mYiFragment.getActivity(),
						MultiRoomDiscoverActivity.class);
				mYiFragment.startActivity(intent);
			}
		});
	}

	public void onActivityCreated() {
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Cursor cursor = (Cursor) mAdapter.getItem(arg2);
				if (cursor != null) {
					Intent intent = new Intent(mYiFragment.getActivity(),
							RoomInfoActivity.class);
					intent.putExtra("room_jid", cursor.getString(1));
					mYiFragment.getActivity().startActivity(intent);
				}
			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				final Cursor cursor = (Cursor) mAdapter.getItem(arg2);
				if (cursor != null) {
					mYiFragment.showMsgDialog(null, mYiFragment.getString(
							R.string.str_room_delete_req,
							StringUtils.escapeUserHost(cursor.getString(2))),
							mYiFragment.getString(R.string.str_yes),
							mYiFragment.getString(R.string.str_no),
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									if (getXmppBinder().deleteRoom(
											cursor.getLong(0),
											cursor.getString(1)) != 0) {
										mYiFragment
												.showMsgDialog(R.string.err_delete_room_failed);
									}
								}
							}, null);
				}
				return false;
			}
		});
		// loadGroups();
	}

	public void onDestroy() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
	}

	public void updateGroups(Object obj) {
		mCursor = (Cursor) obj;

		if (mAdapter == null) {
			if (mCursor != null) {
				mAdapter = new MultiRoomAdapter(mYiFragment.getActivity(),
						mCursor);
				mListView.setAdapter(mAdapter);
			}
		} else {
			mAdapter.changeCursor(mCursor);
			mAdapter.notifyDataSetChanged();
		}
	}

	public void loadGroups() {
		getXmppBinder().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					String owner = UserInfo.getUserInfo(
							getXmppBinder().getServiceContext()).getUser();
					Cursor cursor = mYiFragment
							.getActivity()
							.getContentResolver()
							.query(MultiChatRoomColumns.CONTENT_URI,
									new String[] { MultiChatRoomColumns._ID,
											MultiChatRoomColumns.ROOM_JID,
											MultiChatRoomColumns.ROOM_NAME,
											MultiChatRoomColumns.ROOM_DESC },
									MultiChatRoomColumns.OWNER + "='" + owner
											+ "'", null, null);

					Message message = mYiFragment.getHandler().obtainMessage(
							FriendFragment.MSG_UPDATE_GROUPS_LIST, cursor);
					message.sendToTarget();
				} catch (Exception e) {
					// TODO: handle exception
					YiLog.getInstance().e(e, "discover failed");
				}
			}
		});
	}

	public XmppBinder getXmppBinder() {
		try {
			YiIMApplication application = (YiIMApplication) mYiFragment
					.getActivity().getApplication();
			return application.getXmppService();
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}
}

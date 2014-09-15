package com.ikantech.yiim.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikantech.support.adapter.PinnedHeaderExpandableListViewAdapter;
import com.ikantech.support.common.YiPinnedHeaderExListViewMng;
import com.ikantech.support.common.YiPinnedHeaderExListViewMng.OnPinnedHeaderChangeListener;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.FriendListAdapter;
import com.ikantech.yiim.adapter.FriendListAdapter.FriendItem;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.RosterGroup;
import com.ikantech.yiim.entity.TabContactsModel;
import com.ikantech.yiim.frag.FriendFragment;
import com.ikantech.yiim.runnable.LocalLoadRosterRunnable;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.StringUtils;

public class InviteFriendActivity extends CustomTitleActivity {
	private View mPinnedHeaderView;
	private ImageView mPinnedImageView;
	private TextView mPinnedTextView;
	private TextView mPinnedRightTextView;

	private ExpandableListView mListView;

	private List<RosterGroup> mGroups = new ArrayList<RosterGroup>();

	private Map<String, List<TabContactsModel>> mEntries = new HashMap<String, List<TabContactsModel>>();

	private List<RosterGroup> mGroups2 = new ArrayList<RosterGroup>();
	private Map<String, List<TabContactsModel>> mEntries2 = new HashMap<String, List<TabContactsModel>>();

	private List<FriendItem> mSelectedFriendItems = new ArrayList<FriendListAdapter.FriendItem>();

	private PinnedHeaderExpandableListViewAdapter mAdapter = null;
	private YiPinnedHeaderExListViewMng mExListViewMng;

	private String mRoomJid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_invite_friend);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case FriendFragment.MSG_UPDATE_FRIENDS_LIST:
			updateFriendList();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mListView = (ExpandableListView) findViewById(R.id.invite_friend_list);

		mPinnedHeaderView = findViewById(R.id.invite_friend_list_group_header);

		mPinnedImageView = (ImageView) findViewById(R.id.friend_list_group_img);
		mPinnedTextView = (TextView) findViewById(R.id.friend_list_group_text);
		mPinnedRightTextView = (TextView) findViewById(R.id.friend_list_group_right_text);

		mPinnedImageView.setBackgroundResource(R.drawable.group_unfold_arrow);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mRoomJid = getIntent().getStringExtra("room_jid");

		mAdapter = new FriendListAdapter(this, mGroups, mEntries,
				mSelectedFriendItems, true);

		setTitleBarRightBtnText(getString(R.string.str_finish));

		mExListViewMng = new YiPinnedHeaderExListViewMng(mListView, mAdapter,
				mPinnedHeaderView);
		mExListViewMng
				.setOnPinnedHeaderChangeListener(new OnPinnedHeaderChangeListener() {

					@Override
					public void onPinnedHeaderChanged(int groupPosition) {
						// TODO Auto-generated method stub
						RosterGroup rosterGroup = (RosterGroup) mAdapter
								.getGroup(groupPosition);
						if (rosterGroup != null) {
							mPinnedTextView.setText(rosterGroup.getName());
							// mPinnedRightTextView.setText(String.format("%d/%d",
							// rosterGroup.getOnlineCount(),
							// rosterGroup.getEntryCount()));
						}
					}
				});

		mListView.setAdapter(mAdapter);

		loadFriendList();
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mListView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// TODO Auto-generated method stub
				FriendItem item = new FriendItem(groupPosition, childPosition);
				if (mSelectedFriendItems.contains(item)) {
					mSelectedFriendItems.remove(item);
				} else {
					mSelectedFriendItems.add(item);
				}
				mAdapter.notifyDataSetChanged();
				return false;
			}
		});
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub
		if (mSelectedFriendItems.size() > 0) {
			String[] rets = new String[mSelectedFriendItems.size()];
			for (int i = 0; i < mSelectedFriendItems.size(); i++) {
				FriendItem item = mSelectedFriendItems.get(i);
				TabContactsModel model = (TabContactsModel) mAdapter.getChild(
						item.groupPosition, item.childPosition);
				if (model != null) {
					rets[i] = model.getUser();
				} else {
					rets[i] = "";
				}
			}
			Intent intent = getIntent();
			intent.putExtra("friends", rets);
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	public void updateFriendList() {
		if (!YiUtils.isStringInvalid(mRoomJid)) {
			Iterator<String> nicks = getXmppBinder().getRoomMembers(mRoomJid);
			if (nicks != null) {
				while (nicks.hasNext()) {
					String nick = nicks.next();
					Iterator<List<TabContactsModel>> iterator = mEntries2
							.values().iterator();
					while (iterator.hasNext()) {
						List<TabContactsModel> models = iterator.next();
						Iterator<TabContactsModel> iterator2 = models
								.iterator();
						while (iterator2.hasNext()) {
							TabContactsModel model = iterator2.next();
							if (model.getUser().startsWith(
									StringUtils.getJidResouce(nick))) {
								iterator2.remove();
							}
						}

					}
				}
			}
		}

		if (mGroups2 != null) {
			mGroups.clear();
			mGroups.addAll(mGroups2);

			mGroups2.clear();
			mGroups2 = null;
		}

		if (mEntries2 != null) {
			mEntries.clear();
			mEntries.putAll(mEntries2);

			mEntries2.clear();
			mEntries2 = null;
		}

		mAdapter.notifyDataSetChanged();
	}

	public void loadFriendList() {
		try {
			YiLog.getInstance().i("loadFriendList");
			getXmppBinder().execute(
					new LocalLoadRosterRunnable(this, getHandler(), mGroups2,
							mEntries2));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

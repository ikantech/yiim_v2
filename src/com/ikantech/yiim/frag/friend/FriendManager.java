package com.ikantech.yiim.frag.friend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;
import com.ikantech.support.adapter.PinnedHeaderExpandableListViewAdapter;
import com.ikantech.support.common.YiPinnedHeaderExListViewMng;
import com.ikantech.support.common.YiPinnedHeaderExListViewMng.OnPinnedHeaderChangeListener;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.FriendListAdapter;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.entity.RosterGroup;
import com.ikantech.yiim.entity.TabContactsModel;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.runnable.LocalLoadRosterRunnable;
import com.ikantech.yiim.runnable.XmppDeleteEntryRunnable;
import com.ikantech.yiim.runnable.XmppLoadRosterRunnable;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.UserInfoActivity;

public class FriendManager implements XmppListener {
	private static final int INT_KEY_GROUP = R.id.key_group;
	private static final int INT_KEY_CHILD = R.id.key_child;

	private View mRootView;
	private View mPinnedHeaderView;
	private ImageView mPinnedImageView;
	private TextView mPinnedTextView;
	private TextView mPinnedRightTextView;
	private NativeLoadRosterReceiver mLoadRosterReceiver;

	private PullToRefreshExpandableListView mListView;
	private YiPinnedHeaderExListViewMng mExListViewMng;
	private List<RosterGroup> mGroups = new ArrayList<RosterGroup>();

	private Map<String, List<TabContactsModel>> mEntries = new HashMap<String, List<TabContactsModel>>();

	private List<RosterGroup> mGroups2 = new ArrayList<RosterGroup>();
	private Map<String, List<TabContactsModel>> mEntries2 = new HashMap<String, List<TabContactsModel>>();

	private PinnedHeaderExpandableListViewAdapter mAdapter = null;

	private YiFragment mYiFragment;

	public FriendManager(YiFragment yiFragment, View rootView) {
		mYiFragment = yiFragment;
		mRootView = rootView;
	}

	public void onCreateView() {
		mListView = (PullToRefreshExpandableListView) mRootView
				.findViewById(R.id.tab_contacts_list);

		mPinnedHeaderView = mRootView
				.findViewById(R.id.friend_list_group_header);

		mPinnedImageView = (ImageView) mRootView
				.findViewById(R.id.friend_list_group_img);
		mPinnedTextView = (TextView) mRootView
				.findViewById(R.id.friend_list_group_text);
		mPinnedRightTextView = (TextView) mRootView
				.findViewById(R.id.friend_list_group_right_text);

		mPinnedImageView.setBackgroundResource(R.drawable.group_unfold_arrow);

		mListView
				.setOnRefreshListener(new OnRefreshListener<ExpandableListView>() {

					@Override
					public void onRefresh(
							PullToRefreshBase<ExpandableListView> refreshView) {
						// TODO Auto-generated method stub
						mGroups.clear();
						mEntries.clear();
						mAdapter.notifyDataSetChanged();
						XmppLoadRosterRunnable runnable = new XmppLoadRosterRunnable(
								mYiFragment.getActivity(), null);
						getXmppBinder().execute(runnable);
					}
				});
	}

	public void onActivityCreated() {
		mAdapter = new FriendListAdapter(mYiFragment.getActivity(), mGroups,
				mEntries);

		mExListViewMng = new YiPinnedHeaderExListViewMng(
				mListView.getRefreshableView(), mAdapter, mPinnedHeaderView);
		mExListViewMng
				.setOnPinnedHeaderChangeListener(new OnPinnedHeaderChangeListener() {

					@Override
					public void onPinnedHeaderChanged(int groupPosition) {
						// TODO Auto-generated method stub
						RosterGroup rosterGroup = (RosterGroup) mAdapter
								.getGroup(groupPosition);
						if (rosterGroup != null) {
							mPinnedTextView.setText(rosterGroup.getName());
							mPinnedRightTextView.setText(String.format("%d/%d",
									rosterGroup.getOnlineCount(),
									rosterGroup.getEntryCount()));
						}
					}
				});

		mListView.setAdapter(mAdapter);

		mListView.getRefreshableView().setOnChildClickListener(
				new ContactItemListener());
		mListView.getRefreshableView().setOnItemLongClickListener(
				new ContactItemLongClickListener());

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(XmppLoadRosterRunnable.LOADROSTER_FAILED);
		intentFilter.addAction(XmppLoadRosterRunnable.LOADROSTER_SUCCESS);
		mLoadRosterReceiver = new NativeLoadRosterReceiver();
		mYiFragment.getActivity().registerReceiver(mLoadRosterReceiver,
				intentFilter);

		if (XmppLoadRosterRunnable.isLoadingRoster()) {
			mListView.setRefreshing();
		}

		// loadFriendList();
	}

	public void onDestroy() {
		mYiFragment.getActivity().unregisterReceiver(mLoadRosterReceiver);
	}

	public void updateFriendList() {
		if (mGroups2 != null) {
			mGroups.clear();
			mGroups.addAll(mGroups2);

			mGroups2.clear();
		}

		if (mEntries2 != null) {
			mEntries.clear();
			mEntries.putAll(mEntries2);

			mEntries2.clear();
		}

		mAdapter.notifyDataSetChanged();
	}

	public void loadFriendList() {
		try {
			YiLog.getInstance().i("loadFriendList");
			getXmppBinder().execute(
					new LocalLoadRosterRunnable(mYiFragment.getActivity(),
							mYiFragment.getHandler(), mGroups2, mEntries2));
		} catch (Exception e) {
			// TODO: handle exception
		}
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

	// 长按删除好友
	private class ContactItemLongClickListener implements
			OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			int groupPosition = (Integer) arg1.getTag(INT_KEY_GROUP);
			int childPosition = (Integer) arg1.getTag(INT_KEY_CHILD);

			final TabContactsModel model = (TabContactsModel) mAdapter
					.getChild(groupPosition, childPosition);
			final RosterGroup rosterGroup = (RosterGroup) mAdapter
					.getGroup(groupPosition);
			if (model != null) {
				if (ItemType.none.toString().equals(model.getRosterType())) {

					// 如果有未认证的用户，则让用户选择是删除用户还是重新发起认证。
					final String[] sections = new String[] {
							mYiFragment.getString(R.string.str_re_auth),
							mYiFragment.getString(R.string.str_delete_friend) };
					Dialog alertDialog = new AlertDialog.Builder(
							mYiFragment.getActivity())
							.setTitle(
									mYiFragment
											.getString(R.string.str_choise_op))
							.setItems(sections,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (which == 0) {
												reRequestAuth(rosterGroup,
														model);
											} else {
												deleteRoster(rosterGroup, model);
											}
										}
									})
							.setNegativeButton(
									mYiFragment.getActivity().getString(
											R.string.str_cancel),
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											// TODO Auto-generated method stub
										}
									}).create();
					alertDialog.show();
				} else {
					deleteRoster(rosterGroup, model);
				}
				return true;
			}
			return false;
		}
	}

	private void reRequestAuth(final RosterGroup rosterGroup,
			final TabContactsModel model) {
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Presence presence = new Presence(Presence.Type.subscribe);
				presence.setTo(model.getUser());
				getXmppBinder().getXmppConnection().sendPacket(presence);
			}
		});
		// XmppAddEntryRunnable runnable = new XmppAddEntryRunnable(
		// mYiFragment.getActivity(), model.getUser(), null);
		// if (rosterGroup.getName() != null
		// && !"unfiled".equals(rosterGroup.getName())) {
		// runnable.setGroupName(rosterGroup.getName());
		// }
		// getXmppBinder().execute(runnable);
	}

	private void deleteRoster(final RosterGroup rosterGroup,
			final TabContactsModel model) {
		mYiFragment.showMsgDialog(
				null,
				mYiFragment.getString(R.string.str_entry_delete_req,
						model.getMsg()),
				mYiFragment.getString(R.string.str_yes),
				mYiFragment.getString(R.string.str_no),
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						XmppDeleteEntryRunnable runnable = new XmppDeleteEntryRunnable(
								mYiFragment.getActivity(), model.getUser(),
								rosterGroup.getId(), FriendManager.this);
						getXmppBinder().execute(runnable);
					}
				}, null);
	}

	private class ContactItemListener implements OnChildClickListener {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			final TabContactsModel model = (TabContactsModel) mAdapter
					.getChild(groupPosition, childPosition);
			if (model == null) {
				return false;
			}

			Intent intent = new Intent(mYiFragment.getActivity(),
					UserInfoActivity.class);
			intent.putExtra("user", model.getUser());
			intent.putExtra("which", mYiFragment.getActivity().getClass()
					.getSimpleName());
			intent.putExtra("roster_id", model.getRosterId());
			mYiFragment.startActivity(intent);

			return true;
		}
	}

	@Override
	public void onXmppResonpse(final XmppResult result) {
		// TODO Auto-generated method stub
		mYiFragment.getHandler().post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (result.what == XmppCmds.XMPP_DELETE_ENTRY) {
					if (result.status == Status.SUCCESS) {
						loadFriendList();
					} else {
						mYiFragment
								.showMsgDialog(R.string.err_entry_delete_failed);
					}
				}
			}
		});
	}

	private class NativeLoadRosterReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, final Intent intent) {
			// TODO Auto-generated method stub
			mYiFragment.getHandler().post(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mListView.onRefreshComplete();
					if (intent.getAction().equals(
							XmppLoadRosterRunnable.LOADROSTER_SUCCESS)
							|| intent.getAction().equals(
									XmppLoadRosterRunnable.LOADROSTER_FAILED)) {
						loadFriendList();
					}
				}
			});
		}
	}
}

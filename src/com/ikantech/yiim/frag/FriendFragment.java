package com.ikantech.yiim.frag;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.ikantech.support.util.YiLog;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.frag.friend.FriendManager;
import com.ikantech.yiim.frag.friend.GroupManager;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.CreateRoomActivity;
import com.ikantech.yiim.ui.FriendAddActivity;
import com.ikantech.yiim.ui.GroupManagerActivity;
import com.ikantech.yiim.ui.MainActivity;
import com.ikantech.yiim.ui.MultiRoomDiscoverActivity;
import com.ikantech.yiim.widget.CommonPopupDialog;

public class FriendFragment extends YiFragment implements View.OnClickListener {
	public static final int MSG_UPDATE_FRIENDS_LIST = 0x01;
	public static final int MSG_UPDATE_GROUPS_LIST = 0x02;
	public static final int MSG_UPDATE_FRIENDS = 0x03;
	public static final int MSG_UPDATE_GROUPS = 0x04;

	private View mRootView;

	private NativeReceiver mNativeReceiver;

	private FriendManager mFriendManager;
	private GroupManager mGroupManager;

	private List<View> mViews;

	private ViewPager mViewPager;
	private TextView mFriendBtn;
	private TextView mGroupBtn;
	private XmppBinder mXmppBinder;

	private CommonPopupDialog mPopupDialog;

	private View mTitleRightTopToolsView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mRootView = inflater.inflate(R.layout.main_frag_friend, null);

		View view1 = inflater.inflate(R.layout.frieng_frag_tab_contacts, null);
		View view2 = inflater.inflate(R.layout.frieng_frag_tab_groups, null);

		mViews = new ArrayList<View>();
		mViews.add(view1);
		mViews.add(view2);

		mViewPager = (ViewPager) mRootView.findViewById(R.id.friend_frag_pager);
		mFriendBtn = (TextView) mRootView
				.findViewById(R.id.friend_frag_friend_btn);
		mGroupBtn = (TextView) mRootView
				.findViewById(R.id.friend_frag_group_btn);

		// 禁止滑动
		mViewPager.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return true;
			}
		});

		mFriendBtn.setOnClickListener(this);
		mGroupBtn.setOnClickListener(this);

		mFriendManager = new FriendManager(this, view1);
		mFriendManager.onCreateView();

		mGroupManager = new GroupManager(this, view2);
		mGroupManager.onCreateView();

		return mRootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

		PagerAdapter pagerAdapter = new PagerAdapter() {
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0 == arg1;
			}

			@Override
			public int getCount() {
				return mViews.size();
			}

			@Override
			public void destroyItem(View container, int position, Object object) {
				((ViewPager) container).removeView(mViews.get(position));
			}

			@Override
			public Object instantiateItem(View container, int position) {
				((ViewPager) container).addView(mViews.get(position));
				return mViews.get(position);
			}
		};
		mViewPager.setAdapter(pagerAdapter);

		mNativeReceiver = new NativeReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.NOTIFY_RELOAD_ROSTER_ENTRIES);
		getActivity().registerReceiver(mNativeReceiver, intentFilter);

		mFriendManager.onActivityCreated();
		mGroupManager.onActivityCreated();
	}

	public int getCurrentItem() {
		return mViewPager.getCurrentItem();
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		mFriendManager.loadFriendList();
		mGroupManager.loadGroups();

		if (mViewPager.getCurrentItem() == 0) {
			mFriendBtn.setSelected(true);
			mGroupBtn.setSelected(false);
			getMainActivity().setTitleBarRightImageBtnSrc(
					R.drawable.mm_title_btn_menu);
		} else {
			mFriendBtn.setSelected(false);
			mGroupBtn.setSelected(true);
			getMainActivity().setTitleBarRightImageBtnSrc(
					R.drawable.mm_title_btn_menu);
		}
	}

	public MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mFriendManager.onDestroy();
		getActivity().unregisterReceiver(mNativeReceiver);
		super.onDestroy();
	}

	public XmppBinder getXmppBinder() {
		try {

			YiIMApplication application = (YiIMApplication) getActivity()
					.getApplication();
			return application.getXmppService();
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		YiLog.getInstance().i("processHandlerMessage %d", msg.what);
		switch (msg.what) {
		case MSG_UPDATE_FRIENDS_LIST:
			mFriendManager.updateFriendList();
			break;
		case MSG_UPDATE_FRIENDS:
			mFriendManager.loadFriendList();
			break;
		case MSG_UPDATE_GROUPS_LIST:
			mGroupManager.updateGroups(msg.obj);
			break;
		case MSG_UPDATE_GROUPS:
			mGroupManager.loadGroups();
			break;
		default:
			break;
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.friend_frag_friend_btn:
			mFriendBtn.setSelected(true);
			mGroupBtn.setSelected(false);
			getMainActivity().setTitleBarRightImageBtnSrc(
					R.drawable.mm_title_btn_menu);
			mViewPager.setCurrentItem(0);
			break;
		case R.id.friend_frag_group_btn:
			mFriendBtn.setSelected(false);
			mGroupBtn.setSelected(true);
			getMainActivity().setTitleBarRightImageBtnSrc(
					R.drawable.mm_title_btn_menu);
			mViewPager.setCurrentItem(1);
			break;
		default:
			break;
		}
	}

	public boolean isPopupDialogIsShowing() {
		if (mPopupDialog != null && mPopupDialog.isShowing()) {
			return true;
		}
		return false;
	}

	public void onTitleBarRightImgBtnClick(View view) {
		if (!isPopupDialogIsShowing()) {
			if (mTitleRightTopToolsView == null) {
				mTitleRightTopToolsView = LayoutInflater.from(getActivity())
						.inflate(R.layout.user_info_title_btn_dialog, null);

				Button btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_btn);
				btn.setText(R.string.str_friend_search);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mPopupDialog.dismiss();

						// 搜索好友
						if (mViewPager.getCurrentItem() == 0) {
							Intent intent = new Intent(getActivity(),
									FriendAddActivity.class);
							startActivity(intent);
						} else {// 查找房间
							Intent intent = new Intent(getActivity(),
									MultiRoomDiscoverActivity.class);
							startActivity(intent);
						}
					}
				});

				btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_memo);
				btn.setText(R.string.str_group_manager);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mPopupDialog.dismiss();

						// 分组管理
						if (mViewPager.getCurrentItem() == 0) {
							Intent intent = new Intent(getActivity(),
									GroupManagerActivity.class);
							startActivity(intent);
						} else {// 创建房间
							Intent intent = new Intent(getActivity(),
									CreateRoomActivity.class);
							startActivity(intent);
						}
					}
				});

				btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_delete);
				btn.setVisibility(View.GONE);

				btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_btn_cancel);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mPopupDialog.dismiss();
					}
				});
			}

			if (mPopupDialog == null) {
				mPopupDialog = new CommonPopupDialog(getActivity(),
						android.R.style.Theme_Panel);
				mPopupDialog.setCanceledOnTouchOutside(true);
			}

			if (mViewPager.getCurrentItem() == 0) {
				// 搜索好友
				Button btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_btn);
				btn.setText(R.string.str_friend_search);

				// 分组管理
				btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_memo);
				btn.setText(R.string.str_group_manager);
			} else {
				// 查找房间
				Button btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_btn);
				btn.setText(R.string.str_find_groups);

				// 创建房间
				btn = (Button) mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_memo);
				btn.setText(R.string.title_activity_create_room);
			}

			mPopupDialog.setAnimations(android.R.style.Animation_InputMethod);
			mPopupDialog.setContentView(mTitleRightTopToolsView);

			mPopupDialog.showAtLocation(Gravity.BOTTOM
					| Gravity.CENTER_HORIZONTAL, 0, 0,
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		} else {
			mPopupDialog.dismiss();
		}
	}

	private class NativeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Const.NOTIFY_RELOAD_ROSTER_ENTRIES)) {
				YiLog.getInstance().i("recv %s", intent.getAction());
				getHandler().removeMessages(MSG_UPDATE_FRIENDS);
				getHandler().sendEmptyMessageDelayed(MSG_UPDATE_FRIENDS, 200);
			}
		}
	}
}

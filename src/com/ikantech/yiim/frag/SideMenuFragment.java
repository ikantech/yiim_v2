package com.ikantech.yiim.frag;

import za.co.immedia.pinnedheaderlistview.PinnedHeaderListView;
import za.co.immedia.pinnedheaderlistview.PinnedHeaderListView.OnItemClickListener;
import za.co.immedia.pinnedheaderlistview.SectionedBaseAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.MainActivity;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class SideMenuFragment extends YiFragment {
	private static final int MSG_LOAD_VCARD_COMPLETE = 0x01;
	private static final int MSG_RELOAD_NICK = 0x02;
	private static final int MSG_RELOAD_SIGN = 0x03;

	private PinnedHeaderListView mListView;
	private View mLeftArrow;
	private View mRightArrow;
	private String[] mSections;
	private String[] mCommonItems;
	private String[] mOtherItems;
	private String[] mSettingItems;
	private int mMode = SlidingMenu.LEFT;
	private int mSelectedSection = 0;
	private int mSelectedItem = 0;
	private LeftMenuSectionedAdapter mAdapter;
	private OnMenuStateChangeListener mMenuStateChangeListener;

	private FriendFragment mFriendFragment;
	private ConversationFragment mConversationFragment;
	private SettingsFragment mSettingsFragment;
	private UserInfoSetFragment mUserInfoSetFragment;
	private AboutFragment mAboutFragment;

	private View mAccountRootView;

	private XmppVcard mVCard;
	private ImageView mAvatariImageView;
	private TextView mSignTextView;
	private TextView mNickTextView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_side_menu, null);
		mListView = (PinnedHeaderListView) view
				.findViewById(R.id.main_menu_list);
		mLeftArrow = view.findViewById(R.id.main_menu_left_arrow);
		mRightArrow = view.findViewById(R.id.main_menu_right_arrow);

		mAvatariImageView = (ImageView) view
				.findViewById(R.id.main_menu_avatar);
		mSignTextView = (TextView) view
				.findViewById(R.id.main_menu_account_signature);
		mNickTextView = (TextView) view
				.findViewById(R.id.main_menu_account_name);

		mAccountRootView = view.findViewById(R.id.main_menu_account_root);
		mAccountRootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mUserInfoSetFragment == null) {
					mUserInfoSetFragment = new UserInfoSetFragment();
				}
				setSection(-1, -1);
				switchFragment(mUserInfoSetFragment);
				if (mMenuStateChangeListener != null) {
					mMenuStateChangeListener.onSelectionChanged(-1, -1);
				}
			}
		});

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mSections = getResources().getStringArray(R.array.left_menu_sections);
		mCommonItems = getResources().getStringArray(R.array.left_menu_commons);
		mOtherItems = getResources().getStringArray(R.array.left_menu_others);
		mSettingItems = getResources().getStringArray(
				R.array.left_menu_settings);

		if (mMode == SlidingMenu.LEFT) {
			mLeftArrow.setVisibility(View.GONE);
		} else {
			mRightArrow.setVisibility(View.GONE);
		}

		mAdapter = new LeftMenuSectionedAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onSectionClick(AdapterView<?> adapterView, View view,
					int section, int rawPosition, long id) {

			}

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int section, int position, int rawPosition, long id) {
				// 排除“退出”
				if (!(section == 1 && position == 1)) {
					setSection(section, position);
					if (mMenuStateChangeListener != null) {
						mMenuStateChangeListener.onSelectionChanged(section,
								position);
					}
				}
				onListItemClick(section, position);
			}

			@Override
			public void onHeadOrFooterClick(AdapterView<?> adapterView,
					View view, int position, long id) {

			}
		});

		mListView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
						&& mMenuStateChangeListener != null) {

					View v = mListView.getChildAt(0);
					int lvChildTop = (v == null) ? 0 : v.getTop();

					mMenuStateChangeListener.onScrollChanged(
							mListView.getFirstVisiblePosition(), lvChildTop);
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

			}
		});

		mConversationFragment = new ConversationFragment();
		switchFragment(mConversationFragment);
	}

	public void loadUserInfo(final XmppBinder xmppBinder) {
		xmppBinder.execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mVCard = new XmppVcard(xmppBinder.getServiceContext());
					mVCard.load(xmppBinder.getXmppConnection());
					getHandler().sendEmptyMessage(MSG_LOAD_VCARD_COMPLETE);
				} catch (Exception e) {
					// TODO: handle exception
					YiLog.getInstance().e(e, "load user vcard failed");
				}
			}
		});
	}

	public void setMode(int mMode) {
		this.mMode = mMode;
	}

	public void setMenuStateChangeListener(
			OnMenuStateChangeListener mMenuStateChangeListener) {
		this.mMenuStateChangeListener = mMenuStateChangeListener;
	}

	public void setScroll(int scrollX, int scrollY) {
		// mListView.scrollTo(scrollX, scrollY);
		mListView.setSelectionFromTop(scrollX, scrollY);
	}

	public void setSection(int section, int position) {
		mSelectedSection = section;
		mSelectedItem = position;
		mAdapter.notifyDataSetChanged();
	}

	public void onListItemClick(int section, int position) {
		Fragment newContent = null;
		switch (section) {
		case 0:
			switch (position) {
			case 0:
				newContent = mConversationFragment;
				break;
			case 1:
				if (mFriendFragment == null) {
					mFriendFragment = new FriendFragment();
				}
				newContent = mFriendFragment;
				break;
			default:
				break;
			}
			break;
		// case 1:
		//
		// break;
		case 1:
			switch (position) {
			case 0:
				if (mSettingsFragment == null) {
					mSettingsFragment = new SettingsFragment();
				}
				newContent = mSettingsFragment;
				break;
			case 1:
				if (getActivity() instanceof MainActivity) {
					MainActivity fca = (MainActivity) getActivity();
					fca.exit();
				}
				break;
			case 2:
				if (mAboutFragment == null) {
					mAboutFragment = new AboutFragment();
				}
				newContent = mAboutFragment;
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		if (newContent != null)
			switchFragment(newContent);
	}

	// the meat of switching the above fragment
	private void switchFragment(Fragment fragment) {
		if (getActivity() == null)
			return;

		if (getActivity() instanceof MainActivity) {
			MainActivity fca = (MainActivity) getActivity();
			fca.switchContent(fragment);
		}
	}

	private class LeftMenuSectionedAdapter extends SectionedBaseAdapter {

		@Override
		public Object getItem(int section, int position) {
			if (position == -1 && section >= 0 && section < mSections.length) {
				return mSections[section];
			}
			String[] items = null;
			switch (section) {
			case 0:
				items = mCommonItems;
				break;
			// case 1:
			// items = mOtherItems;
			// break;
			case 1:
				items = mSettingItems;
				break;
			default:
				break;
			}
			if (position >= 0 && position < items.length) {
				return items[position];
			}
			return null;
		}

		@Override
		public long getItemId(int section, int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getSectionCount() {
			return mSections.length;
		}

		@Override
		public int getCountForSection(int section) {
			switch (section) {
			case 0:
				return mCommonItems.length;
				// case 1:
				// return mOtherItems.length;
			case 1:
				return mSettingItems.length;
			default:
				return 0;
			}
		}

		public Drawable getItemDrawable(int section, int position) {
			switch (section) {
			case 0:
				switch (position) {
				// case 0:
				// return getResources().getDrawable(
				// R.drawable.side_menu_newsfeed_selector);
				case 0:
					return getResources().getDrawable(
							R.drawable.side_menu_chat_selector);

				case 1:
					return getResources().getDrawable(
							R.drawable.side_menu_friends_selector);
				case 2:
					return getResources().getDrawable(
							R.drawable.side_menu_media_selector);
				case 3:
					return getResources().getDrawable(
							R.drawable.side_menu_more_selector);
				default:
					return null;
				}
				// case 1:
				// switch (position) {
				// case 0:
				// return getResources().getDrawable(
				// R.drawable.side_menu_market_selector);
				// case 1:
				// return getResources().getDrawable(
				// R.drawable.side_menu_game_selector);
				// case 2:
				// return getResources().getDrawable(
				// R.drawable.side_menu_mall_selector);
				// default:
				// return null;
				// }
			case 1:
				switch (position) {
				case 0:
					return getResources().getDrawable(
							R.drawable.side_menu_settings_selector);
					/*
					 * case 1: return getResources().getDrawable(
					 * R.drawable.side_menu_feedback_selector);
					 */
				case 1:
					return getResources().getDrawable(
							R.drawable.side_menu_log_out_selector);
				case 2:
					return getResources().getDrawable(
							R.drawable.side_menu_about_selector);
				default:
					return null;
				}
			default:
				return null;
			}
		}

		@Override
		public View getItemView(int section, int position, View convertView,
				ViewGroup parent) {
			LinearLayout layout = null;
			if (convertView == null) {
				LayoutInflater inflator = (LayoutInflater) parent.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				layout = (LinearLayout) inflator.inflate(
						R.layout.side_menu_list_item, null);
			} else {
				layout = (LinearLayout) convertView;
			}
			String item = (String) getItem(section, position);
			if (!YiUtils.isStringInvalid(item)) {
				TextView textView = ((TextView) layout
						.findViewById(R.id.textItem));
				textView.setText(item);
				textView.setCompoundDrawablePadding(getResources()
						.getDimensionPixelSize(R.dimen.normal_padding));
				textView.setCompoundDrawablesWithIntrinsicBounds(
						getItemDrawable(section, position), null, null, null);
				if (mSelectedSection == section && mSelectedItem == position) {
					textView.setSelected(true);
				} else {
					textView.setSelected(false);
				}
			}
			return layout;
		}

		@Override
		public View getSectionHeaderView(int section, View convertView,
				ViewGroup parent) {
			LinearLayout layout = null;
			if (convertView == null) {
				LayoutInflater inflator = (LayoutInflater) parent.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				layout = (LinearLayout) inflator.inflate(
						R.layout.side_menu_section_item, null);
			} else {
				layout = (LinearLayout) convertView;
			}
			String item = (String) getItem(section, -1);
			if (!YiUtils.isStringInvalid(item)) {
				((TextView) layout.findViewById(R.id.textItem)).setText(item);
			}
			return layout;
		}
	}

	public interface OnMenuStateChangeListener {
		void onSelectionChanged(int section, int position);

		void onScrollChanged(int scrollX, int scrollY);
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
		switch (msg.what) {
		case MSG_LOAD_VCARD_COMPLETE:
			if (mVCard != null) {
				// 加载用户头像
				YiAsyncImageLoader.loadBitmapFromStore(mVCard.getUserId(),
						new YiImageLoaderListener() {
							@Override
							public void onImageLoaded(String url, Bitmap bitmap) {
								// TODO Auto-generated method stub
								mAvatariImageView.setImageBitmap(bitmap);
							}
						});

				// 加载用户的个性签名
				getHandler().sendEmptyMessage(MSG_RELOAD_SIGN);

				mNickTextView.setText(mVCard.getDisplayName());

				// 加载用户昵称
				String nick = mVCard.getNickName();
				if (YiUtils.isStringInvalid(nick)) {
					getXmppBinder().execute(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								String name = getXmppBinder()
										.getXmppConnection()
										.getAccountManager()
										.getAccountAttribute("name");
								if (!YiUtils.isStringInvalid(name)) {
									mVCard.setNickName(name);
									mVCard.save(getXmppBinder()
											.getXmppConnection());
									getHandler().sendEmptyMessage(
											MSG_RELOAD_NICK);
								}
							} catch (Exception e) {
								// TODO: handle exception
							}
						}
					});
				}
			}
			break;
		case MSG_RELOAD_NICK:
			// 加载用户昵称
			mNickTextView.setText(mVCard.getDisplayName());
			break;
		case MSG_RELOAD_SIGN:
			// 加载用户的个性签名
			String sign = mVCard.getSign();
			if (sign != null && sign.length() > 0) {
				mSignTextView.setText(sign);
			}
			break;
		}
	}
}

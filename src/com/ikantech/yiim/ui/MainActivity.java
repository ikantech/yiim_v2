package com.ikantech.yiim.ui;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ikantech.support.ui.YiFragmentBaseActivity;
import com.ikantech.support.util.YiFileUtils;
import com.ikantech.support.util.YiPrefsKeeper;
import com.ikantech.yiim.R;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.YiIMConfig;
import com.ikantech.yiim.frag.AboutFragment;
import com.ikantech.yiim.frag.ConversationFragment;
import com.ikantech.yiim.frag.FriendFragment;
import com.ikantech.yiim.frag.SettingsFragment;
import com.ikantech.yiim.frag.SideMenuFragment;
import com.ikantech.yiim.frag.UserInfoSetFragment;
import com.ikantech.yiim.frag.SideMenuFragment.OnMenuStateChangeListener;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.StringUtils;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityHelper;

public class MainActivity extends YiFragmentBaseActivity implements
		SlidingActivityBase {
	private static final int MSG_CHECK_XMPP_SERVICE = 0x01;

	private int mMenuMode = SlidingMenu.LEFT_RIGHT;

	private SlidingActivityHelper mHelper;
	private Fragment mContent;
	private SideMenuFragment mLeftMenu;
	private SideMenuFragment mRightMenu;

	private TextView mTitleTextView;
	private Button mTitleRightBtn;
	private Button mTitleBackBtn;
	private ImageButton mTitleLeftImgBtn;
	private ImageButton mTitleRightImgBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelper = new SlidingActivityHelper(this);
		mHelper.onCreate(savedInstanceState);

		mContent = null;
		if (savedInstanceState != null)
			mContent = getSupportFragmentManager().getFragment(
					savedInstanceState, "mContent");
		// if (mContent == null)
		// mContent = new ColorFragment(R.color.red);

		// set the Above View
		setContentView(R.layout.side_menu_content_frame);
		// getSupportFragmentManager().beginTransaction()
		// .replace(R.id.content_frame, mContent).commit();
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		sm.setMode(mMenuMode);

		switch (mMenuMode) {
		case SlidingMenu.LEFT:
			setupLeftMenu();
			break;
		case SlidingMenu.RIGHT:
			setupRightMenu();
			break;
		case SlidingMenu.LEFT_RIGHT:
			setupSideMenu();
			break;
		default:
			break;
		}
		if (mLeftMenu != null && mRightMenu != null) {
			mLeftMenu
					.setMenuStateChangeListener(new OnMenuStateChangeListener() {

						@Override
						public void onSelectionChanged(int section, int position) {
							// TODO Auto-generated method stub
							mRightMenu.setSection(section, position);
						}

						@Override
						public void onScrollChanged(int scrollX, int scrollY) {
							// TODO Auto-generated method stub
							mRightMenu.setScroll(scrollX, scrollY);
						}
					});
			mRightMenu
					.setMenuStateChangeListener(new OnMenuStateChangeListener() {

						@Override
						public void onSelectionChanged(int section, int position) {
							// TODO Auto-generated method stub
							mLeftMenu.setSection(section, position);
						}

						@Override
						public void onScrollChanged(int scrollX, int scrollY) {
							// TODO Auto-generated method stub
							mLeftMenu.setScroll(scrollX, scrollY);
						}
					});
		}

		YiIMConfig config = YiIMConfig.getInstance();
		config.setExited(false);
		YiPrefsKeeper.write(this, config);

		if (getXmppService() == null) {
			getHandler().sendEmptyMessageDelayed(MSG_CHECK_XMPP_SERVICE, 200);
		} else {
			startXmpp();
		}
	}

	private void startXmpp() {
		getXmppService().checkIfNeedAutoLogin();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (getXmppService() != null) {
			onXmppServiceStarted();
		}
	}

	private XmppBinder getXmppService() {
		YiIMApplication yiIMApplication = (YiIMApplication) getApplication();
		return yiIMApplication.getXmppService();
	}

	private void setupLeftMenu() {
		SlidingMenu sm = getSlidingMenu();
		setBehindContentView(R.layout.left_menu_frame);
		sm.setShadowDrawable(R.drawable.left_menu_shadow);
		mLeftMenu = new SideMenuFragment();
		mLeftMenu.setMode(SlidingMenu.LEFT);
		// set the Behind View
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.left_menu_frame, mLeftMenu).commit();
	}

	private void setupRightMenu() {
		SlidingMenu sm = getSlidingMenu();
		setBehindContentView(R.layout.right_menu_frame);
		sm.setShadowDrawable(R.drawable.right_menu_shadow);
		mRightMenu = new SideMenuFragment();
		mRightMenu.setMode(SlidingMenu.RIGHT);
		// set the Behind View
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.right_menu_frame, mRightMenu).commit();
	}

	private void setupSideMenu() {
		SlidingMenu sm = getSlidingMenu();
		setupLeftMenu();

		sm.setSecondaryMenu(R.layout.right_menu_frame);
		sm.setSecondaryShadowDrawable(R.drawable.right_menu_shadow);
		mRightMenu = new SideMenuFragment();
		mRightMenu.setMode(SlidingMenu.RIGHT);
		// set the Behind View
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.right_menu_frame, mRightMenu).commit();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mHelper.onPostCreate(savedInstanceState);
	}

	@Override
	public View findViewById(int id) {
		View v = super.findViewById(id);
		if (v != null)
			return v;
		return mHelper.findViewById(id);
	}

	public void reloadUserInfo() {
		if (mLeftMenu != null) {
			mLeftMenu.loadUserInfo(getXmppService());
		}
		if (mRightMenu != null) {
			mRightMenu.loadUserInfo(getXmppService());
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mHelper.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState, "mContent", mContent);
	}

	@Override
	public void setContentView(int id) {
		setContentView(getLayoutInflater().inflate(id, null));
	}

	@Override
	public void setContentView(View v) {
		setContentView(v, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	@Override
	public void setContentView(View v, LayoutParams params) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.setContentView(R.layout.activity_custom_title);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.layout_title_bar);

		LinearLayout l = (LinearLayout) findViewById(R.id.main_layout);
		l.addView(v, params);
		mHelper.registerAboveContentView(l, params);

		mTitleTextView = (TextView) findViewById(R.id.title_bar_title);
		mTitleBackBtn = (Button) findViewById(R.id.title_back_btn);
		mTitleRightBtn = (Button) findViewById(R.id.title_right_btn);
		mTitleRightImgBtn = (ImageButton) findViewById(R.id.title_right_img_btn);
		mTitleLeftImgBtn = (ImageButton) findViewById(R.id.title_left_img_btn);

		mTitleBackBtn.setVisibility(View.GONE);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_CHECK_XMPP_SERVICE:
			XmppBinder binder = getXmppService();
			if (binder == null) {
				getHandler().sendEmptyMessageDelayed(MSG_CHECK_XMPP_SERVICE,
						200);
			} else {
				startXmpp();
				// onXmppServiceStarted();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void setBehindContentView(View view, LayoutParams layoutParams) {
		// TODO Auto-generated method stub
		mHelper.setBehindContentView(view, layoutParams);
	}

	@Override
	public void setBehindContentView(View view) {
		// TODO Auto-generated method stub
		setBehindContentView(view, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	@Override
	public void setBehindContentView(int layoutResID) {
		// TODO Auto-generated method stub
		setBehindContentView(getLayoutInflater().inflate(layoutResID, null));
	}

	@Override
	public SlidingMenu getSlidingMenu() {
		// TODO Auto-generated method stub
		return mHelper.getSlidingMenu();
	}

	@Override
	public void toggle() {
		// TODO Auto-generated method stub
		mHelper.toggle();
	}

	@Override
	public void showContent() {
		// TODO Auto-generated method stub
		mHelper.showContent();
	}

	@Override
	public void showMenu() {
		// TODO Auto-generated method stub
		mHelper.showMenu();
	}

	@Override
	public void showSecondaryMenu() {
		// TODO Auto-generated method stub
		mHelper.showSecondaryMenu();
	}

	@Override
	public void setSlidingActionBarEnabled(boolean slidingActionBarEnabled) {
		// TODO Auto-generated method stub
		mHelper.setSlidingActionBarEnabled(slidingActionBarEnabled);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean b = mHelper.onKeyUp(keyCode, event);
		if (b)
			return b;
		return super.onKeyUp(keyCode, event);
	}

	public void switchContent(Fragment fragment) {
		if (mContent == fragment)
			return;
		mContent = fragment;

		mTitleBackBtn.setVisibility(View.GONE);
		mTitleLeftImgBtn.setVisibility(View.GONE);
		mTitleRightImgBtn.setVisibility(View.GONE);
		mTitleRightBtn.setVisibility(View.GONE);

		if (mContent instanceof ConversationFragment) {
			setTitleRes(R.string.str_chats);
		} else if (mContent instanceof FriendFragment) {
			setTitleRes(R.string.str_contacts);
		} else if (mContent instanceof SettingsFragment) {
			setTitleRes(R.string.str_settings);
		} else if (mContent instanceof UserInfoSetFragment) {
			setTitleRes(R.string.str_personal_info);
		} else if (mContent instanceof AboutFragment) {
			setTitleRes(R.string.str_about);
		}

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.content_frame, fragment).commit();
		getSlidingMenu().showContent();
	}

	public void setTitleBarRightBtnText(String lab) {
		if (lab != null && lab.length() > 1) {
			mTitleRightBtn.setText(lab);
			mTitleRightBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleRightBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarLeftBtnText(String lab) {
		if (lab != null && lab.length() > 1) {
			mTitleBackBtn.setText(lab);
			mTitleBackBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleBackBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarRightImageBtnSrc(int srcId) {
		if (srcId != -1) {
			mTitleRightImgBtn.setImageDrawable(getResources()
					.getDrawable(srcId));
			mTitleRightImgBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleRightImgBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarLeftImageBtnSrc(int srcId) {
		if (srcId != -1) {
			mTitleLeftImgBtn
					.setImageDrawable(getResources().getDrawable(srcId));
			mTitleLeftImgBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleLeftImgBtn.setVisibility(View.GONE);
		}
	}

	public void onTitleBarLeftBtnClick(View view) {
		// TODO Auto-generated method stub
	}

	public void onTitleBarLeftImgBtnClick(View view) {
		// TODO Auto-generated method stub

	}

	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub

	}

	public void onTitleBarRightImgBtnClick(View view) {
		// TODO Auto-generated method stub
		if (mContent instanceof FriendFragment) {
			((FriendFragment) mContent).onTitleBarRightImgBtnClick(view);
		}

	}

	private void setTitleRes(int res) {
		mTitleTextView.setText(res);
	}

	/**
	 * 用户自行登录
	 */
	private void manualLogin() {
		getXmppService().removeNotification();
		YiIMConfig config = YiIMConfig.getInstance();
		config.setExited(true);
		YiPrefsKeeper.write(MainActivity.this, config);

		UserInfo userInfo = UserInfo.getUserInfo(this);
		if (userInfo != null) {
			userInfo.setAutoLogin(false);
			YiPrefsKeeper.write(this, userInfo);
		}

		getXmppService().stopXmppService(null);

		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	public void exit() {
		showContent();
		showMsgDialog(null, getString(R.string.str_exit_confirm),
				getString(R.string.str_ok), getString(R.string.str_cancel),
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						getXmppService().removeNotification();
						YiIMApplication application = (YiIMApplication) getApplication();
						application.setLogin(false);
						YiIMConfig config = YiIMConfig.getInstance();
						config.setExited(true);
						YiPrefsKeeper.write(MainActivity.this, config);

						getXmppService().stopXmppService(null);
						finish();
					}
				}, null);
	}

	protected void onXmppServiceStarted() {
		if (mContent != null && mContent instanceof ConversationFragment) {
			((ConversationFragment) mContent).setXmppBinder(getXmppService());
			((ConversationFragment) mContent).loadConversations();
		}
		reloadUserInfo();
		getXmppService().updateNotification();
	}

	public void onExitLoginClick(View v) {
		manualLogin();
	}

	public void onMsgTipSetClick(View v) {
		Intent intent = new Intent(MainActivity.this,
				MsgTipSettingActivity.class);
		startActivity(intent);
	}

	public void onClearChatRecordClick(View v) {
		showMsgDialog(null, getString(R.string.tip_clear_chat_record),
				getString(R.string.str_yes), getString(R.string.str_no),
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						getXmppService().execute(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								try {
									String currentUser = UserInfo.getUserInfo(
											getXmppService()
													.getServiceContext())
											.getUser();
									String whereString = " like '"
											+ currentUser + "%'";
									getContentResolver().delete(
											ConversationColumns.CONTENT_URI,
											ConversationColumns.USER
													+ whereString, null);
									getContentResolver().delete(
											MsgColumns.CONTENT_URI,
											MsgColumns.SENDER + whereString
													+ " OR "
													+ MsgColumns.RECEIVER
													+ whereString, null);

									File file = new File(
											YiFileUtils.getStorePath()
													+ "yiim/"
													+ StringUtils
															.escapeUserHost(currentUser));
									YiFileUtils.deleteFile(file);
								} catch (Exception e) {
									// TODO: handle exception
								}
							}
						});
					}
				}, null);
	}

	public void onClearConversationClick(View v) {
		showMsgDialog(null, getString(R.string.tip_clear_conversation),
				getString(R.string.str_yes), getString(R.string.str_no),
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						getXmppService().execute(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								try {
									String currentUser = UserInfo.getUserInfo(
											getXmppService()
													.getServiceContext())
											.getUser();
									String whereString = " like '"
											+ currentUser + "%'";
									getContentResolver().delete(
											ConversationColumns.CONTENT_URI,
											ConversationColumns.USER
													+ whereString, null);
								} catch (Exception e) {
									// TODO: handle exception
								}
							}
						});
					}
				}, null);
	}
}

package com.ikantech.yiim.ui;

import java.io.File;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;
import com.ikantech.support.cache.YiStoreCache;
import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.common.XmppResult.XmppCmds;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.runnable.XmppAddEntryRunnable;
import com.ikantech.yiim.runnable.XmppDeleteEntryRunnable;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.widget.CommonPopupDialog;
import com.ikantech.yiim.widget.ViewImageDialog;

public class UserInfoActivity extends CustomTitleActivity {
	private static final int MSG_LOAD_VCARD_COMPLETE = 0x01;

	private PullToRefreshScrollView mPullToRefreshScrollView;
	private ImageView mUserAvatarImageView;
	private TextView mNickNameTextView;
	private TextView mUserIdTextView;
	private TextView mUserDistricTextView;
	private TextView mUserSignTextView;
	private TextView mUserSex;
	private TextView mUserMemoTextView;
	private View mUserMemoRootView;

	private String mUser;
	private String mWhich;
	private String mName;
	private XmppVcard mVCard;
	private Button mSendMsgBtn;
	private RosterEntry mRosterEntry;

	private CommonPopupDialog mPopupDialog;
	private ViewImageDialog mImageDialog = null;
	private View mTitleRightTopToolsView;

	private boolean mIsAdded = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_user_info);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub

		switch (msg.what) {
		case MSG_LOAD_VCARD_COMPLETE:
			mPullToRefreshScrollView.onRefreshComplete();

			if ((mRosterEntry != null || mVCard.getRosterId() != -1)
					&& !isMySelf()) {
				View view = findViewById(R.id.modify_group);
				view.setVisibility(View.VISIBLE);

				view = findViewById(R.id.view_chat_record);
				view.setVisibility(View.VISIBLE);
			}

			if (mVCard != null) {
				// 加载用户昵称
				String nick = mVCard.getNickName();
				if (!isStringInvalid(nick)) {
					mNickNameTextView.setText(nick);
				} else {
					mNickNameTextView.setText(StringUtils.escapeUserHost(mVCard
							.getUserId()));
				}

				// 加载用户地区
				String contry = mVCard.getCountry();
				if (!isStringInvalid(contry)) {
					mUserDistricTextView.setText(contry);
				}

				String province = mVCard.getProvince();
				if (!isStringInvalid(province)) {
					if (!isStringInvalid(contry)) {
						mUserDistricTextView.setText(contry + " " + province);
					} else {
						mUserDistricTextView.setText(province);
					}
				}

				String sign = mVCard.getSign();
				if (sign != null && sign.length() > 0) {
					mUserSignTextView.setText(sign);
				}

				// 加载用户性别
				String sex = mVCard.getGender();
				if (!isStringInvalid(sex)) {
					if (Const.MALE.equals(sex)) {
						mUserSex.setText(getString(R.string.str_male));
					} else {
						mUserSex.setText(getString(R.string.str_female));
					}
				}

				if (isMySelf()) {
					try {
						mUserIdTextView
								.setText(getString(R.string.input_lab_mm_no)
										+ getXmppBinder().getXmppConnection()
												.getUser()
												.replaceAll("@.+$", ""));
					} catch (Exception e) {
						// TODO: handle exception
					}
				} else {
					mUserIdTextView.setText(getString(R.string.input_lab_mm_no)
							+ mUser.replaceAll("@.+$", ""));
				}

				// 加载用户头像
				YiAsyncImageLoader.loadBitmapFromStore(mVCard.getUserId(),
						new YiImageLoaderListener() {
							@Override
							public void onImageLoaded(String url, Bitmap bitmap) {
								// TODO Auto-generated method stub
								mUserAvatarImageView.setImageBitmap(bitmap);
							}
						});
			}
			if (!isMySelf() && mRosterEntry != null) {
				mUserMemoTextView.setText(mRosterEntry.getName());
			}

			if (mVCard.getRosterId() == -1) {
				mSendMsgBtn.setVisibility(View.GONE);
				// mUserMemoRootView.setVisibility(View.GONE);
			}
			break;

		default:
			break;
		}
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub
		if (result.what.equals(XmppCmds.XMPP_DELETE_ENTRY)
				&& result.status.equals(Status.SUCCESS)) {
			finish();
		} else if (result.what.equals(XmppCmds.XMPP_ADD_ENTRY)) {
			if (result.status.equals(Status.SUCCESS)) {
				mIsAdded = true;
			} else {
				showMsgDialog(getString(R.string.err_entry_add_requested),
						getString(R.string.str_ok));
			}
		}
	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mPullToRefreshScrollView = (PullToRefreshScrollView) findViewById(R.id.user_info_scrollview);

		mUserAvatarImageView = (ImageView) findViewById(R.id.user_avatar);
		mNickNameTextView = (TextView) findViewById(R.id.user_nick);
		mUserIdTextView = (TextView) findViewById(R.id.user_id);
		mUserDistricTextView = (TextView) findViewById(R.id.user_district);
		mUserSignTextView = (TextView) findViewById(R.id.user_sign);
		mUserSex = (TextView) findViewById(R.id.user_sex);

		mUserMemoRootView = findViewById(R.id.user_memo_root);
		mUserMemoTextView = (TextView) findViewById(R.id.user_memo);

		mSendMsgBtn = (Button) findViewById(R.id.user_info_send_msg_btn);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mUser = getIntent().getStringExtra("user");
		mWhich = getIntent().getStringExtra("which");
		mName = getIntent().getStringExtra("name");

		try {
			if (isMySelf()) {
				mSendMsgBtn.setVisibility(View.GONE);
				mUserMemoRootView.setVisibility(View.GONE);
			} else {
				setTitleBarRightImageBtnSrc(R.drawable.mm_title_btn_menu);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		mPullToRefreshScrollView
				.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
					@Override
					public void onRefresh(
							PullToRefreshBase<ScrollView> refreshView) {
						// TODO Auto-generated method stub
						loadVcard(true);
					}
				});
		loadVcard(false);
	}

	private boolean isMySelf() {
		return "mine".equals(mUser)
				|| mUser.startsWith(UserInfo.getUserInfo(this).getUserName());
	}

	private void loadVcard(final boolean force) {
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mVCard = new XmppVcard(getXmppBinder().getServiceContext());
					UserInfo userInfo = UserInfo
							.getUserInfo(UserInfoActivity.this);
					String user = null;
					if (isMySelf()) {
						user = userInfo.getUser();
					} else {
						user = StringUtils.escapeUserResource(mUser);
					}
					mVCard.load(getXmppBinder().getXmppConnection(), user,
							force);

					try {
						Roster roster = getXmppBinder().getXmppConnection()
								.getRoster();
						mRosterEntry = roster.getEntry(mUser.replaceAll("/.+$",
								""));
					} catch (Exception e) {
						// TODO: handle exception
					}

					getHandler().sendEmptyMessage(MSG_LOAD_VCARD_COMPLETE);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	public void onSendMsgBtnClick(View v) {
		if (ChatActivity.class.getSimpleName().equals(mWhich)) {
			finish();
		} else {
			Intent intent = new Intent(UserInfoActivity.this,
					ChatActivity.class);
			intent.putExtra("to", mUser);
			startActivity(intent);
		}
	}

	public void onViewChatRecordClick(View v) {
		Intent intent = new Intent(UserInfoActivity.this,
				ViewChatRecordActivity.class);
		intent.putExtra("user", StringUtils.escapeUserResource(mUser));
		startActivity(intent);
	}

	public void onModifyGroupClick(View v) {
		Intent intent = new Intent(UserInfoActivity.this,
				GroupManagerActivity.class);
		intent.putExtra("mode", "modify");
		intent.putExtra("groupId", mVCard.getGroupId());
		intent.putExtra("rosterId", mVCard.getRosterId());
		startActivity(intent);
	}

	public boolean isPopupDialogIsShowing() {
		if (mPopupDialog != null && mPopupDialog.isShowing()) {
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == 1) {
			if (mRosterEntry != null) {
				mUserMemoTextView.setText(mRosterEntry.getName());
			}
		}
	}

	@Override
	public void onTitleBarRightImgBtnClick(View view) {
		// TODO Auto-generated method stub
		if (!isPopupDialogIsShowing()) {
			if (mTitleRightTopToolsView == null) {
				mTitleRightTopToolsView = LayoutInflater.from(this).inflate(
						R.layout.user_info_title_btn_dialog, null);

				// 删除该好友
				View btn = mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_delete);
				if (mRosterEntry == null) {
					btn.setVisibility(View.GONE);
				}
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();

						showMsgDialog(
								null,
								getString(R.string.str_entry_delete_req,
										mUser.replaceAll("@.+$", "")),
								getString(R.string.str_yes),
								getString(R.string.str_no),
								new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										getXmppBinder().execute(new Runnable() {
											@Override
											public void run() {
												// TODO Auto-generated
												// method stub
												if (mRosterEntry != null) {
													XmppDeleteEntryRunnable runnable = new XmppDeleteEntryRunnable(
															UserInfoActivity.this,
															mVCard.getRosterId(),
															UserInfoActivity.this);
													getXmppBinder().execute(
															runnable);
												}
											}
										});
									}
								}, null);
					}
				});

				// 加为好友
				btn = mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_btn);
				if (mRosterEntry != null || mVCard.getRosterId() != -1
						|| isMySelf()) {
					btn.setVisibility(View.GONE);
				}
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();
						if (mIsAdded) {
							showMsgDialog(
									getString(R.string.err_entry_add_requested),
									getString(R.string.str_ok));
						} else {
							showMsgDialog(null,
									getString(R.string.str_entry_add_req),
									getString(R.string.str_yes),
									getString(R.string.str_no),
									new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											XmppAddEntryRunnable runnable = new XmppAddEntryRunnable(
													UserInfoActivity.this,
													StringUtils
															.escapeUserResource(mUser),
													UserInfoActivity.this);
											// runnable.setGroupName(groupName);
											getXmppBinder().execute(runnable);
										}
									}, null);
						}
					}
				});

				// 添加备注
				btn = mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_add_memo);
				if (mRosterEntry == null) {
					btn.setVisibility(View.GONE);
				}
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();
						Intent intent = new Intent(UserInfoActivity.this,
								CommonInputActivity.class);
						intent.putExtra("what",
								CommonInputActivity.INPUT_USER_INFO_MEMO_SET);
						intent.putExtra("who", mUser);
						intent.putExtra("roster_id", mVCard.getRosterId());
						startActivityForResult(intent, 1);
					}
				});

				btn = mTitleRightTopToolsView
						.findViewById(R.id.user_info_title_btn_cancel);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();
					}
				});
			}

			if (mPopupDialog == null) {
				mPopupDialog = new CommonPopupDialog(this,
						android.R.style.Theme_Panel);
				mPopupDialog.setCanceledOnTouchOutside(true);
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

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (mImageDialog != null && mImageDialog.isShowing()) {
			mImageDialog.dismiss();
		}
		super.onDestroy();
	}

	// 当头像点击后
	public void onAvatarClick(View v) {
		File file = new File(YiStoreCache.convertToFileName(mVCard.getUserId()));
		if (!file.exists()) {
			return;
		}
		if (mImageDialog == null) {
			mImageDialog = new ViewImageDialog(this, R.style.ImageViewDialog);
		}
		mImageDialog.setBitmapPath(file.getAbsolutePath());
		mImageDialog.show();
	}
}

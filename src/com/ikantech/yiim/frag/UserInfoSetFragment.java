package com.ikantech.yiim.frag;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;
import com.ikantech.support.cache.YiStoreCache;
import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.support.util.YiImageUtil;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.CommonInputActivity;
import com.ikantech.yiim.ui.MainActivity;
import com.ikantech.yiim.widget.CommonPopupDialog;
import com.ikantech.yiim.widget.ViewImageDialog;

public class UserInfoSetFragment extends YiFragment implements
		View.OnClickListener {
	private static final int MSG_LOAD_VCARD_COMPLETE = 0x01;
	private static final int MSG_LOAD_RES = 0x02;
	private static final int MSG_RELOAD_AVATAR = 0x03;
	private static final int MSG_RELOAD_NICK = 0x04;
	private static final int MSG_RELOAD_SIGN = 0x05;
	private static final int MSG_RELOAD_DISTRICT = 0x06;
	private static final int MSG_RELOAD_SEX = 0x07;
	private static final int USERPIC_REQUEST_ID = 0x01;
	private static final int USERPIC_CAPTURE_ID = 0x02;
	private static final int USERNICK_REQUEST_ID = 0x03;
	private static final int DISTRICT_REQUEST_ID = 0x04;
	private static final int USERSIGN_REQUEST_ID = 0x05;

	private PullToRefreshScrollView mPullToRefreshScrollView;

	private ImageView mAvatariImageView;
	private TextView mNickTextView;
	private TextView mUserIdTextView;

	private TextView mSexTextView;
	private TextView mDistricTextView;
	private TextView mSignTextView;

	private CommonPopupDialog mPopupDialog;
	private View mAvatarChooseView;

	private ViewImageDialog mImageDialog = null;

	private XmppVcard mVCard;

	private View mRootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.frag_user_info_set, null);

		return mRootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		initViews();
		initDatas();
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
	public void onStop() {
		// TODO Auto-generated method stub
		if (isPopupDialogIsShowing()) {
			mPopupDialog.dismiss();
		}
		super.onStop();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		if (mImageDialog != null && mImageDialog.isShowing()) {
			mImageDialog.dismiss();
		}
		super.onDestroy();
	}

	protected void initViews() {
		// TODO Auto-generated method stub
		mPullToRefreshScrollView = (PullToRefreshScrollView) mRootView
				.findViewById(R.id.frag_user_info_set);
		mAvatariImageView = (ImageView) mRootView
				.findViewById(R.id.user_info_set_pic);
		mNickTextView = (TextView) mRootView
				.findViewById(R.id.user_info_set_nick);
		mUserIdTextView = (TextView) mRootView
				.findViewById(R.id.user_info_set_userid);

		mSexTextView = (TextView) mRootView
				.findViewById(R.id.user_info_set_sex);
		mDistricTextView = (TextView) mRootView
				.findViewById(R.id.user_info_set_district);
		mSignTextView = (TextView) mRootView
				.findViewById(R.id.user_info_set_sign);

		View v = mRootView.findViewById(R.id.user_info_set_pic_root);
		v.setOnClickListener(this);

		v = mRootView.findViewById(R.id.user_info_set_nick_root);
		v.setOnClickListener(this);

		v = mRootView.findViewById(R.id.user_info_set_district_root);
		v.setOnClickListener(this);

		v = mRootView.findViewById(R.id.user_info_set_sign_root);
		v.setOnClickListener(this);

		mAvatariImageView.setOnClickListener(this);
	}

	protected void initDatas() {
		// TODO Auto-generated method stub
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

	private void loadVcard(final boolean force) {
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mVCard = new XmppVcard(getXmppBinder().getServiceContext());
					mVCard.load(getXmppBinder().getXmppConnection(), UserInfo
							.getUserInfo(getXmppBinder().getServiceContext())
							.getUser(), force);
					getHandler().sendEmptyMessage(MSG_LOAD_VCARD_COMPLETE);
				} catch (Exception e) {
					// TODO: handle exception
					YiLog.getInstance().e(e, "load user vcard failed");
				}
			}
		});
	}

	private boolean isStringInvalid(String str) {
		return YiUtils.isStringInvalid(str);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_LOAD_VCARD_COMPLETE:
			mPullToRefreshScrollView.onRefreshComplete();
			if (mVCard != null) {
				// 加载用户头像
				getHandler().sendEmptyMessage(MSG_RELOAD_AVATAR);

				// 加载用户的个性签名
				getHandler().sendEmptyMessage(MSG_RELOAD_SIGN);

				try {
					mUserIdTextView.setText(UserInfo.getUserInfo(
							getXmppBinder().getServiceContext()).getUserName());
				} catch (Exception e) {
				}

				// 加载用户昵称
				String nick = mVCard.getNickName();
				if (!isStringInvalid(nick)) {
					mNickTextView.setText(nick);
				} else {
					getXmppBinder().execute(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								String name = getXmppBinder()
										.getXmppConnection()
										.getAccountManager()
										.getAccountAttribute("name");
								if (!isStringInvalid(name)) {
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

				// 加载用户性别
				String sex = mVCard.getGender();
				if (!isStringInvalid(sex)) {
					if (Const.MALE.equals(sex)) {
						mSexTextView.setText(getString(R.string.str_male));
					} else {
						mSexTextView.setText(getString(R.string.str_female));
					}
				}

				// 加载用户地区
				getHandler().sendEmptyMessage(MSG_RELOAD_DISTRICT);
			}
			break;
		case MSG_RELOAD_AVATAR:
			YiAsyncImageLoader.loadBitmapFromStore(mVCard.getUserId(),
					new YiImageLoaderListener() {
						@Override
						public void onImageLoaded(String url, Bitmap bitmap) {
							// TODO Auto-generated method stub
							mAvatariImageView.setImageBitmap(bitmap);
						}
					});
			break;
		case MSG_RELOAD_NICK:
			// 加载用户昵称
			String nick = mVCard.getNickName();
			if (!isStringInvalid(nick)) {
				mNickTextView.setText(nick);
			}
			break;
		case MSG_RELOAD_SIGN:
			// 加载用户的个性签名
			String sign = mVCard.getSign();
			if (sign != null && sign.length() > 0) {
				mSignTextView.setText(sign);
			}
			break;
		case MSG_RELOAD_DISTRICT:
			// 加载用户地区
			String contry = mVCard.getCountry();
			if (!isStringInvalid(contry)) {
				mDistricTextView.setText(contry);
			}

			String province = mVCard.getProvince();
			if (!isStringInvalid(province)) {
				if (!isStringInvalid(contry)) {
					mDistricTextView.setText(contry + " " + province);
				} else {
					mDistricTextView.setText(province);
				}
			}

			break;
		case MSG_RELOAD_SEX:
			// 加载用户性别
			String sex = mVCard.getGender();
			if (!isStringInvalid(sex)) {
				if (Const.MALE.equals(sex)) {
					mSexTextView.setText(getString(R.string.str_male));
				} else {
					mSexTextView.setText(getString(R.string.str_female));
				}
			}
			break;
		case MSG_LOAD_RES:
			loadRes((Intent) msg.obj, msg.arg1);
			break;
		default:
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == Activity.RESULT_OK) {
			Message msg = getHandler().obtainMessage(MSG_LOAD_RES, requestCode,
					0, data);
			getHandler().sendMessageDelayed(msg, 200);
		}
		switch (requestCode) {
		case USERNICK_REQUEST_ID:
		case DISTRICT_REQUEST_ID:
		case USERSIGN_REQUEST_ID:
			initDatas();
			((MainActivity) getActivity()).reloadUserInfo();
			break;
		default:
			break;
		}
	}

	private void loadRes(Intent data, int requestCode) {
		if (requestCode == USERPIC_REQUEST_ID) {
			Uri uri = data.getData();
			ContentResolver cr = getActivity().getContentResolver();
			try {
				String strPath = "";
				if ("content".equalsIgnoreCase(uri.getScheme())) {
					Cursor c = cr.query(uri,
							new String[] { MediaStore.Images.Media.DATA },
							null, null, null);
					if (c != null) {
						if (c.moveToFirst()) {
							strPath = c
									.getString((c
											.getColumnIndex(MediaStore.Images.Media.DATA)));
						}
						c.close();
					}
				} else {
					strPath = uri.toString();
				}

				if (strPath.length() != 0) {
					if (strPath.startsWith("file:")) {
						strPath = strPath.replaceFirst("file:", "");
					}
					if (requestCode == USERPIC_REQUEST_ID && mVCard != null) {
						// if (file.length() > mConversationPicLimit) {
						// showMessageDialog(getString(
						// R.string.err_limit_image_size,
						// file.length() / 1024));
						// } else {
						// Bitmap bitmap = BitmapFactory.decodeFile(strPath);
						// ImageView imageView = (ImageView)
						// findViewById(R.id.lawyer_portrait_imag);
						// imageView.setImageBitmap(bitmap);
						// }
						final File file = new File(strPath);
						if (file.exists()) {
							Bitmap bitmap = YiImageUtil.getImageWithSizeLimit(
									file.getAbsolutePath(), 96, 96);
							saveAvatar(bitmap);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (requestCode == USERPIC_CAPTURE_ID) {
			Bitmap bitmap = YiImageUtil.getImageWithSizeLimit((Bitmap) data
					.getExtras().get("data"), 96, 96);
			saveAvatar(bitmap);
		}
	}

	private void saveAvatar(final Bitmap bitmap) {
		if (bitmap != null && mVCard != null) {
			YiLog.getInstance().i("image w : %d, h : %d", bitmap.getWidth(),
					bitmap.getHeight());
			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
						mVCard.setAvatar(baos.toByteArray());
						getHandler().sendEmptyMessage(MSG_RELOAD_AVATAR);
						mVCard.save(getXmppBinder().getXmppConnection());
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						try {
							if (baos != null) {
								baos.close();
								baos = null;
							}
						} catch (Exception e2) {
							// TODO: handle exception
						}
					}
				}
			});
		}
	}

	public void onNickBtnClick(View view) {
		Intent intent = new Intent(getActivity(), CommonInputActivity.class);
		intent.putExtra("what", CommonInputActivity.INPUT_USER_INFO_SET_NICK);
		startActivityForResult(intent, USERNICK_REQUEST_ID);
	}

	public boolean isPopupDialogIsShowing() {
		if (mPopupDialog != null && mPopupDialog.isShowing()) {
			return true;
		}
		return false;
	}

	public void onUserPicBtnClick(View view) {
		if (!isPopupDialogIsShowing()) {
			if (mAvatarChooseView == null) {
				mAvatarChooseView = LayoutInflater.from(getActivity()).inflate(
						R.layout.avatar_choose_dialog, null);
				View btn = mAvatarChooseView.findViewById(R.id.btn_take_photo);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();
						Intent intent = new Intent(
								MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(intent, USERPIC_CAPTURE_ID);
					}
				});

				btn = mAvatarChooseView.findViewById(R.id.btn_gallery);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();

						Intent intent = new Intent();
						intent.setType("image/*");
						intent.setAction(Intent.ACTION_GET_CONTENT);
						startActivityForResult(intent, USERPIC_REQUEST_ID);
					}
				});

				btn = mAvatarChooseView.findViewById(R.id.btn_cancel);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mPopupDialog.dismiss();
					}
				});
			}

			if (mPopupDialog == null) {
				mPopupDialog = new CommonPopupDialog(getActivity(),
						android.R.style.Theme_Panel);
				mPopupDialog.setCanceledOnTouchOutside(true);
			}
			mPopupDialog.setAnimations(android.R.style.Animation_InputMethod);
			mPopupDialog.setContentView(mAvatarChooseView);

			mPopupDialog.showAtLocation(Gravity.BOTTOM
					| Gravity.CENTER_HORIZONTAL, 0, 0,
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		} else {
			mPopupDialog.dismiss();
		}
	}

	public void onAvatarClick() {
		File file = new File(YiStoreCache.convertToFileName(mVCard.getUserId()));
		if (!file.exists()) {
			return;
		}
		if (mImageDialog == null) {
			mImageDialog = new ViewImageDialog(getActivity(),
					R.style.ImageViewDialog);
		}
		mImageDialog.setBitmapPath(file.getAbsolutePath());
		mImageDialog.show();
	}

	public void onDistrictBtnClick(View view) {
		Intent intent = new Intent(getActivity(), CommonInputActivity.class);
		intent.putExtra("what",
				CommonInputActivity.INPUT_USER_INFO_SET_DISTRICT);
		startActivityForResult(intent, DISTRICT_REQUEST_ID);
	}

	public void onSignBtnClick(View view) {
		Intent intent = new Intent(getActivity(), CommonInputActivity.class);
		intent.putExtra("what", CommonInputActivity.INPUT_USER_INFO_SET_SIGN);
		startActivityForResult(intent, USERSIGN_REQUEST_ID);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.user_info_set_pic_root:
			onUserPicBtnClick(v);
			break;
		case R.id.user_info_set_nick_root:
			onNickBtnClick(v);
			break;
		case R.id.user_info_set_district_root:
			onDistrictBtnClick(v);
			break;
		case R.id.user_info_set_sign_root:
			onSignBtnClick(v);
			break;
		case R.id.user_info_set_pic:
			onAvatarClick();
			break;
		default:
			break;
		}
	}
}

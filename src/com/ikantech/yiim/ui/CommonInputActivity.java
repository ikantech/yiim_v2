package com.ikantech.yiim.ui;

import java.util.ArrayList;

import org.jivesoftware.smack.RosterEntry;

import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.EditText;

import com.ikantech.yiim.R;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.ui.base.CustomTitleActivity;

public class CommonInputActivity extends CustomTitleActivity {
	private static final int MSG_LOAD_VCARD_COMPLETE = 0x01;

	public static final String INPUT_USER_INFO_SET_NICK = "nick";
	public static final String INPUT_USER_INFO_SET_DISTRICT = "district";
	public static final String INPUT_USER_INFO_SET_SIGN = "sign";
	public static final String INPUT_USER_INFO_MEMO_SET = "memo";

	private String mWhat;
	private String mWho;

	// 修改昵称
	private View mNickRootView;
	private EditText mNickEditText;

	// 修改地区
	private View mDistrictRootView;
	private EditText mCountryEditText;
	private EditText mProvinceEditText;

	// 修改个性签名
	private View mUserSignRootView;
	private EditText mUserSignEditText;

	// 修改备注
	private View mUserMemoRootView;
	private EditText mUserMemoEditText;

	private ArrayList<View> mRootViews;

	private XmppVcard mVCard;
	private RosterEntry mRosterEntry;
	private long mRosterId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_common_input);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mNickRootView = findViewById(R.id.user_nick_input_root);
		mNickEditText = (EditText) findViewById(R.id.common_input_nick_edit);
		mDistrictRootView = findViewById(R.id.user_district_input_root);
		mCountryEditText = (EditText) findViewById(R.id.contry_edit);
		mProvinceEditText = (EditText) findViewById(R.id.district_edit);
		mUserSignRootView = findViewById(R.id.user_sign_input_root);
		mUserSignEditText = (EditText) findViewById(R.id.common_input_user_sign_edit);
		mUserMemoRootView = findViewById(R.id.user_memo_input_root);
		mUserMemoEditText = (EditText) findViewById(R.id.common_input_memo_edit);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		setTitleBarRightBtnText(getString(R.string.str_save));

		mRosterId = getIntent().getLongExtra("roster_id", -1);

		mRootViews = new ArrayList<View>();
		mRootViews.add(mNickRootView);
		mRootViews.add(mDistrictRootView);
		mRootViews.add(mUserSignRootView);
		mRootViews.add(mUserMemoRootView);

		mWhat = getIntent().getStringExtra("what");
		mWho = getIntent().getStringExtra("who");
		
		if (INPUT_USER_INFO_SET_NICK.equals(mWhat)) {
			setTitle(getString(R.string.str_modify_nickname));
			hideAllRootViews();
			mNickRootView.setVisibility(View.VISIBLE);
		} else if (INPUT_USER_INFO_SET_DISTRICT.equals(mWhat)) {
			setTitle(getString(R.string.str_modify_district));
			hideAllRootViews();
			mDistrictRootView.setVisibility(View.VISIBLE);
		} else if (INPUT_USER_INFO_SET_SIGN.equals(mWhat)) {
			setTitle(getString(R.string.str_modify_sign));
			hideAllRootViews();
			mUserSignRootView.setVisibility(View.VISIBLE);
		} else if (INPUT_USER_INFO_MEMO_SET.equals(mWhat)) {
			setTitle(getString(R.string.str_modify_memo));
			hideAllRootViews();
			mUserMemoRootView.setVisibility(View.VISIBLE);
		}

		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					if (INPUT_USER_INFO_MEMO_SET.equals(mWhat)
							&& !isStringInvalid(mWho)) {
						mRosterEntry = getXmppBinder().getXmppConnection()
								.getRoster().getEntry(mWho);
					} else {
						mVCard = new XmppVcard(getXmppBinder()
								.getServiceContext());
						mVCard.load(getXmppBinder().getXmppConnection());
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

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_LOAD_VCARD_COMPLETE:
			if (mVCard != null) {
				// 加载用户昵称
				String nick = mVCard.getNickName();
				if (!isStringInvalid(nick)) {
					mNickEditText.setText(nick);
				}

				String contry = mVCard.getCountry();
				if (!isStringInvalid(contry)) {
					mCountryEditText.setText(contry);
				}

				String province = mVCard.getProvince();
				if (!isStringInvalid(province)) {
					mProvinceEditText.setText(province);
				}

				String sign = mVCard.getSign();
				if (sign != null && sign.length() > 0) {
					mUserSignEditText.setText(sign);
				}
				
				mRosterId = mVCard.getRosterId();
			}
			if (mRosterEntry != null) {
				mUserMemoEditText.setText(mRosterEntry.getName());
			}
			break;

		default:
			break;
		}
	}

	private void hideAllRootViews() {
		for (View rootView : mRootViews) {
			rootView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub
		if (INPUT_USER_INFO_SET_DISTRICT.equals(mWhat)
				|| INPUT_USER_INFO_SET_NICK.equals(mWhat)) {
			if (mVCard == null) {
				showMsgDialog(getString(R.string.err_init_failed),
						getString(R.string.str_ok));
				return;
			}
		}

		if (INPUT_USER_INFO_SET_NICK.equals(mWhat)) {
			if (isStringInvalid(mNickEditText.getText())) {
				showMsgDialog(getString(R.string.err_empty_nick_name),
						getString(R.string.str_ok));
				return;
			}

			if (mVCard.getNickName() != null
					&& mVCard.getNickName().equals(mNickEditText.getText())) {
				showMsgDialog(getString(R.string.err_not_modify_nick_name),
						getString(R.string.str_ok));
				return;
			}

			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						// getXmppBinder().getXmppConnection().getAccountManager()
						// .changeName(mNickEditText.getText().toString());
						mVCard.setNickName(mNickEditText.getText().toString());
						mVCard.save(getXmppBinder().getXmppConnection());
						showMsgDialog(getString(R.string.str_save_success),
								getString(R.string.str_ok));
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			});
		} else if (INPUT_USER_INFO_SET_DISTRICT.equals(mWhat)) {
			if (isStringInvalid(mCountryEditText.getText())) {
				showMsgDialog(getString(R.string.err_empty_country),
						getString(R.string.str_ok));
				return;
			}

			if (isStringInvalid(mProvinceEditText.getText())) {
				showMsgDialog(getString(R.string.err_empty_province),
						getString(R.string.str_ok));
				return;
			}

			if (mCountryEditText.getText().toString()
					.equals(mVCard.getCountry())
					&& mProvinceEditText.getText().toString()
							.equals(mVCard.getProvince())) {
				showMsgDialog(
						getString(R.string.err_not_modify_country_and_province),
						getString(R.string.str_ok));
				return;
			}

			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						mVCard.setCountry(mCountryEditText.getText().toString());
						mVCard.setProvince(mProvinceEditText.getText()
								.toString());
						mVCard.save(getXmppBinder().getXmppConnection());
						showMsgDialog(getString(R.string.str_save_success),
								getString(R.string.str_ok));
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			});
		} else if (INPUT_USER_INFO_SET_SIGN.equals(mWhat)) {
			if (isStringInvalid(mUserSignEditText.getText())) {
				showMsgDialog(getString(R.string.err_empty_nick_name),
						getString(R.string.str_ok));
				return;
			}

			if (mVCard.getNickName() != null
					&& mVCard.getNickName().equals(mUserSignEditText.getText())) {
				showMsgDialog(getString(R.string.err_not_modify_sign),
						getString(R.string.str_ok));
				return;
			}

			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						mVCard.setSign(mUserSignEditText.getText().toString());
						mVCard.save(getXmppBinder().getXmppConnection());
						showMsgDialog(getString(R.string.str_save_success),
								getString(R.string.str_ok));
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			});
		} else if (INPUT_USER_INFO_MEMO_SET.equals(mWhat)) {
			if (mRosterEntry == null)
				return;
			if (isStringInvalid(mUserMemoEditText.getText())) {
				showMsgDialog(getString(R.string.err_empty_memo),
						getString(R.string.str_ok));
				return;
			}

			if (mUserMemoEditText.getText().equals(mRosterEntry.getName())) {
				showMsgDialog(getString(R.string.err_not_modify_memo),
						getString(R.string.str_ok));
				return;
			}

			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						if (mRosterId != -1) {
							ContentValues values = new ContentValues();
							values.put(RosterColumns.MEMO_NAME,
									mUserMemoEditText.getText().toString());
							getContentResolver().update(
									ContentUris.withAppendedId(
											RosterColumns.CONTENT_URI,
											mRosterId), values, null, null);
						}
						mRosterEntry.setName(mUserMemoEditText.getText()
								.toString());
						showMsgDialog(getString(R.string.str_save_success),
								getString(R.string.str_ok));
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			});
		}
	}
}

package com.ikantech.yiim.ui;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.ui.base.CustomTitleActivity;

public class RegisterActivity extends CustomTitleActivity {

	private EditText mUserNameEditText;
	private EditText mNickNameEditText;
	private EditText mPasswdEditText;
	private EditText mPasswdConfirmEditText;
	private EditText mBirthdayEditText;
	private EditText mEmailEditText;
	private EditText mContryEditText;
	private EditText mProvinceEditText;
	private String mSex = Const.FEMALE;
	private RadioGroup mSexRadioGroup;
	private Calendar mCalendar;

	private DatePickerDialog mDatePickerDialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_register);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub
		cancelProgressDialog();
		switch (result.what) {
		case XMPP_REGISTER:
			if (result.status.equals(Status.SUCCESS)) {
				getXmppBinder().login(
						mUserNameEditText.getText().toString().trim(),
						mPasswdEditText.getText().toString().trim(),
						RegisterActivity.this);
			} else {
				YiLog.getInstance().i("error: %s", (String) result.obj);
				registerFailed();
			}
			break;
		case XMPP_LOGIN:
			if (result.status.equals(Status.SUCCESS)) {
				saveVard();
			} else {
				YiLog.getInstance().i("error: %s", (String) result.obj);
				registerFailed();
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mUserNameEditText = (EditText) findViewById(R.id.register_user_name_edit);
		mNickNameEditText = (EditText) findViewById(R.id.register_nick_name_edit);
		mPasswdEditText = (EditText) findViewById(R.id.register_passwd_edit);
		mPasswdConfirmEditText = (EditText) findViewById(R.id.register_passwd_confirm_edit);
		mBirthdayEditText = (EditText) findViewById(R.id.register_birthday_edit);
		mSexRadioGroup = (RadioGroup) findViewById(R.id.register_sex);
		mEmailEditText = (EditText) findViewById(R.id.register_email_edit);
		mContryEditText = (EditText) findViewById(R.id.register_contry_edit);
		mProvinceEditText = (EditText) findViewById(R.id.register_district_edit);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		setTitleBarRightBtnText(getString(R.string.str_submit));
		mCalendar = Calendar.getInstance();
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mSexRadioGroup
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						// TODO Auto-generated method stub
						if (checkedId == R.id.register_sex_female) {
							mSex = Const.FEMALE;
						} else {
							mSex = Const.MALE;
						}
					}
				});
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	private void registerFailed() {
		showMsgDialog(getString(R.string.err_register_failed));
	}

	private void saveVard() {
		showProgressDialog();
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					XmppVcard vCard = new XmppVcard(getXmppBinder()
							.getServiceContext());
					vCard.load(getXmppBinder().getXmppConnection(),
							mUserNameEditText.getText().toString().trim() + "@"
									+ XmppConnectionUtils.getXmppHost());
					if (!isStringInvalid(mEmailEditText.getText().toString()
							.trim())) {
						vCard.setEmail(mEmailEditText.getText().toString()
								.trim());
					}
					if (!isStringInvalid(mNickNameEditText.getText().toString()
							.trim())) {
						vCard.setNickName(mNickNameEditText.getText()
								.toString().trim());
					}
					if (!isStringInvalid(mBirthdayEditText.getText().toString()
							.trim())) {
						vCard.setBirthday(mCalendar.getTimeInMillis());
					}
					if (!isStringInvalid(mContryEditText.getText().toString()
							.trim())) {
						vCard.setCountry(mContryEditText.getText().toString()
								.trim());
					}
					if (!isStringInvalid(mProvinceEditText.getText().toString()
							.trim())) {
						vCard.setProvince(mProvinceEditText.getText()
								.toString().trim());
					}
					vCard.setGender(mSex);
					vCard.save(getXmppBinder().getXmppConnection());

					// 重启服务器
					getXmppBinder().stopXmppService(null);

					showMsgDialog(getString(R.string.str_register_success),
							getString(R.string.str_ok),
							new View.OnClickListener() {

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									finish();
								}
							});
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub
		if (isStringInvalid(mUserNameEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_user_name));
			return;
		}
		if (isStringInvalid(mNickNameEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_nick_name));
			return;
		}
		if (isStringInvalid(mPasswdEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_passwd));
			return;
		}
		if (isStringInvalid(mPasswdConfirmEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_confirm_passwd));
			return;
		}
		if (!mPasswdEditText.getText().toString().trim()
				.equals(mPasswdConfirmEditText.getText().toString().trim())) {
			showMsgDialog(getString(R.string.err_noteq_passwd));
			return;
		}
		if (isStringInvalid(mBirthdayEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_birthday));
			return;
		}

		if (!mUserNameEditText.getText().toString().trim()
				.matches("^[a-z0-9]{5,}$")) {
			showMsgDialog(getString(R.string.err_illegal_username));
			return;
		}

		showProgressDialog();
		getXmppBinder().register(mUserNameEditText.getText().toString().trim(),
				mPasswdEditText.getText().toString().trim(), this);
	}

	public void onBirthdayClick(View view) {
		if (mDatePickerDialog == null) {
			mDatePickerDialog = new DatePickerDialog(RegisterActivity.this,
					new OnDateSetListener() {
						@Override
						public void onDateSet(DatePicker view, int year,
								int monthOfYear, int dayOfMonth) {
							// TODO Auto-generated method stub
							mBirthdayEditText.setText(String.format(
									"%04d-%02d-%02d", year, monthOfYear + 1,
									dayOfMonth));
							mCalendar.set(Calendar.YEAR, year);
							mCalendar.set(Calendar.MONTH, monthOfYear);
							mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						}
					}, 1980, 0, 1);
			mCalendar.set(Calendar.YEAR, 1980);
			mCalendar.set(Calendar.MONTH, 0);
			mCalendar.set(Calendar.DAY_OF_MONTH, 1);
			mDatePickerDialog.setCanceledOnTouchOutside(true);
			mDatePickerDialog
					.setTitle(getString(R.string.str_hint_select_birthday));
		}
		mDatePickerDialog.show();
	}
}

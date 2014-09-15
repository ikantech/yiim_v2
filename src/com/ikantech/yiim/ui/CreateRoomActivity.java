package com.ikantech.yiim.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.ikantech.support.util.YiHanziToPinyin;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.entity.MultiChatDesc.MultiChatIcon;
import com.ikantech.yiim.ui.base.CustomTitleActivity;

public class CreateRoomActivity extends CustomTitleActivity {
	private static final int REQ_INVITE = 0x01;

	private ImageButton mIcon1;
	private ImageButton mIcon2;
	private ImageButton mIcon3;
	private ImageButton mIcon4;

	private MultiChatIcon mSelectedIcon = MultiChatIcon.DEFAULIT_4;
	private ImageButton mLastSelectedIcon = null;

	private EditText mRoomNameEditText;
	private EditText mRoomSignEditText;

	private String[] mSelectedFriends;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_create_room);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQ_INVITE && resultCode == RESULT_OK) {
			mSelectedFriends = data.getStringArrayExtra("friends");
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mIcon1 = (ImageButton) findViewById(R.id.create_room_icon1);
		mIcon1.setSelected(false);
		mIcon2 = (ImageButton) findViewById(R.id.create_room_icon2);
		mIcon2.setSelected(true);
		mIcon3 = (ImageButton) findViewById(R.id.create_room_icon3);
		mIcon3.setSelected(true);
		mIcon4 = (ImageButton) findViewById(R.id.create_room_icon4);
		mIcon4.setSelected(true);

		mLastSelectedIcon = mIcon1;

		mRoomNameEditText = (EditText) findViewById(R.id.create_room_room_name);
		mRoomSignEditText = (EditText) findViewById(R.id.create_room_room_sign);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		setTitleBarRightBtnText(getString(R.string.str_create));
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
	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub
		if (isStringInvalid(mRoomNameEditText.getText().toString().trim())) {
			showMsgDialog(R.string.err_empty_room_name);
			return;
		}

		if (isStringInvalid(mRoomSignEditText.getText().toString().trim())) {
			showMsgDialog(R.string.err_empty_room_sign);
			return;
		}

		int ret = -1;
		try {
			String name = YiHanziToPinyin.getPinYin(mRoomNameEditText.getText()
					.toString().trim());

			MultiChatDesc desc = new MultiChatDesc();
			desc.setDesc(mRoomSignEditText.getText().toString().trim());
			desc.setName(mRoomNameEditText.getText().toString().trim());
			desc.setIcon(mSelectedIcon);

			ret = getXmppBinder().createRoom(
					UserInfo.getUserInfo(this).getUser(), name,
					desc.toString(), null);

			if (mSelectedFriends != null) {
				for (String friend : mSelectedFriends) {
					if (!YiUtils.isStringInvalid(friend)) {
						getXmppBinder().inviteFriend(
								name + "@conference."
										+ XmppConnectionUtils.getXmppHost(),
								friend, true);
					}
				}
			}
		} catch (Exception e) {
			ret = -1;
		}

		switch (ret) {
		case -2:
			showMsgDialog(R.string.err_create_room_failed_exist);
			break;
		case 0:
			showMsgDialog(getString(R.string.err_create_room_success),
					getString(R.string.str_ok), new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});
			break;
		default:
			showMsgDialog(R.string.err_create_room_failed);
			break;
		}
	}

	public void onInviteFriendClick(View v) {
		Intent intent = new Intent(CreateRoomActivity.this,
				InviteFriendActivity.class);
		startActivityForResult(intent, REQ_INVITE);
	}

	public void onIconBtnClick(View v) {
		if (mIcon1 == v) {
			mLastSelectedIcon.setSelected(true);
			mIcon1.setSelected(false);
			mLastSelectedIcon = mIcon1;
			mSelectedIcon = MultiChatIcon.DEFAULIT_4;
		} else if (mIcon2 == v) {
			mLastSelectedIcon.setSelected(true);
			mIcon2.setSelected(false);
			mLastSelectedIcon = mIcon2;
			mSelectedIcon = MultiChatIcon.DEFAULIT_1;
		} else if (mIcon3 == v) {
			mLastSelectedIcon.setSelected(true);
			mIcon3.setSelected(false);
			mLastSelectedIcon = mIcon3;
			mSelectedIcon = MultiChatIcon.DEFAULIT_2;
		} else if (mIcon4 == v) {
			mLastSelectedIcon.setSelected(true);
			mIcon4.setSelected(false);
			mLastSelectedIcon = mIcon4;
			mSelectedIcon = MultiChatIcon.DEFAULIT_3;
		}
	}

}

package com.ikantech.yiim.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.StringUtils;

public class RoomInfoActivity extends CustomTitleActivity {
	private static final int REQ_INVITE = 0x01;

	private TextView mRoomNameTextView;
	private TextView mRoomJidTextView;
	private TextView mRoomSignTextView;
	private ImageView mRoomIconImageView;

	private String mRoomJid;
	private Cursor mCursor;
	private MultiChatDesc mChatDesc;

	private String mWhich;

	private String[] mSelectedFriends;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_room_info);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQ_INVITE && resultCode == RESULT_OK) {
			mSelectedFriends = data.getStringArrayExtra("friends");
			if (mSelectedFriends != null) {
				for (String friend : mSelectedFriends) {
					if (!YiUtils.isStringInvalid(friend)) {
						getXmppBinder().inviteFriend(mRoomJid, friend, true);
					}
				}
			}
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
		mRoomNameTextView = (TextView) findViewById(R.id.room_nick);
		mRoomJidTextView = (TextView) findViewById(R.id.room_id);
		mRoomSignTextView = (TextView) findViewById(R.id.room_sign);
		mRoomIconImageView = (ImageView) findViewById(R.id.room_avatar);

	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mRoomJid = getIntent().getStringExtra("room_jid");
		mWhich = getIntent().getStringExtra("which");

		if (YiUtils.isStringInvalid(mRoomJid)) {
			finish();
			return;
		}

		String owner = UserInfo.getUserInfo(this).getUser();
		mCursor = getContentResolver().query(
				MultiChatRoomColumns.CONTENT_URI,
				new String[] { MultiChatRoomColumns.ROOM_NAME,
						MultiChatRoomColumns.ROOM_DESC },
				MultiChatRoomColumns.ROOM_JID + "='" + mRoomJid + "' and "
						+ MultiChatRoomColumns.OWNER + "='" + owner + "'",
				null, null);
		if (mCursor != null && mCursor.getCount() == 1) {
			mCursor.moveToFirst();
			String name = mCursor.getString(0);
			try {
				mChatDesc = MultiChatDesc.fromString(mCursor.getString(1));
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (YiUtils.isStringInvalid(name)) {
				mRoomNameTextView.setText(mChatDesc.getName());
			} else {
				mRoomNameTextView.setText(StringUtils.escapeUserHost(name));
			}

			mRoomIconImageView.setImageResource(mChatDesc.getIcon().getResId());
			mRoomJidTextView.setText(getString(R.string.input_lab_mm_no)
					+ StringUtils.escapeUserHost(mRoomJid));
			mRoomSignTextView.setText(mChatDesc.getDesc());
		} else {
			finish();
		}
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	public void onInviteClick(View v) {
		Intent intent = new Intent(RoomInfoActivity.this,
				InviteFriendActivity.class);
		intent.putExtra("room_jid", mRoomJid);
		startActivityForResult(intent, REQ_INVITE);
	}

	public void onMemberClick(View v) {
		Intent intent = new Intent(RoomInfoActivity.this,
				RoomMemberActivity.class);
		intent.putExtra("room_jid", mRoomJid);
		startActivity(intent);
	}

	public void onViewChatRecordClick(View v) {
		Intent intent = new Intent(RoomInfoActivity.this,
				ViewChatRecordActivity.class);
		intent.putExtra("user", StringUtils.escapeUserResource(mRoomJid));
		startActivity(intent);
	}

	public void onSendMsgBtnClick(View v) {
		if (ChatActivity.class.getSimpleName().equals(mWhich)) {
			finish();
			return;
		}
		Intent intent = new Intent(RoomInfoActivity.this, ChatActivity.class);
		intent.putExtra("to", mRoomJid);
		startActivity(intent);
	}
}

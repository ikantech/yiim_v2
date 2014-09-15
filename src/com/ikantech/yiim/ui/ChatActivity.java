package com.ikantech.yiim.ui;

import android.content.Intent;
import android.database.Cursor;
import android.view.View;

import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.ChatMsgViewAdapter;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.ui.base.BaseChatActivity;
import com.ikantech.yiim.util.YiIMUtils;

public class ChatActivity extends BaseChatActivity {
	// private Chat mChat;

	@Override
	protected void sendYiIMMessage(String msg) {
		// TODO Auto-generated method stub
		int ret = getXmppBinder().sendMessage(mUserTo, msg);
		switch (ret) {
		case -1:
			showMsgDialog(R.string.err_send_msg_failed);
			break;
		case -2:
			showMsgDialog(R.string.err_send_msg_failed_no_network);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		// try {
		// mChat.shutdown();
		// ChatManager cm = getXmppBinder().getXmppConnection()
		// .getChatManager();
		// cm.removeChat(mUserTo, mChat);
		// } catch (Exception e) {
		// // TODO: handle exception
		// }
		super.onDestroy();
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initChat() {
		if (YiIMUtils.isMultChat(mUserTo)) {
			View view = findViewById(R.id.chat_footer_image_root);
			view.setVisibility(View.INVISIBLE);

			view = findViewById(R.id.chat_footer_voice_root);
			view.setVisibility(View.INVISIBLE);

			// 设置聊天窗口标题
			try {
				MultiChatDesc desc = MultiChatDesc.fromString(YiIMUtils
						.getMultUserChatDesc(this, mUserTo));

				setTitle(desc.getName());
			} catch (Exception e) {
				// TODO: handle exception
			}
		} else {
			// 设置聊天窗口标题
			try {
				XmppVcard vCard = new XmppVcard(getXmppBinder()
						.getServiceContext());
				vCard.load(getXmppBinder().getXmppConnection(), mUserTo);

				setTitle(vCard.getDisplayName());
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	@Override
	protected void initAdapter(Object obj) {
		// TODO Auto-generated method stub
		mAdapter = new ChatMsgViewAdapter(ChatActivity.this, getXmppBinder(),
				mGifEmotionUtils, (Cursor) obj, mUser.getUser(),
				mEmotionManager);
	}

	@Override
	public void onTitleBarRightImgBtnClick(View view) {
		if (YiIMUtils.isMultChat(mUserTo)) {
			Intent intent = new Intent(ChatActivity.this,
					RoomInfoActivity.class);
			intent.putExtra("room_jid", mUserTo);
			intent.putExtra("which", ChatActivity.class.getSimpleName());
			startActivity(intent);
		} else {
			Intent intent = new Intent(ChatActivity.this,
					UserInfoActivity.class);
			intent.putExtra("user", mUserTo);
			intent.putExtra("which", ChatActivity.class.getSimpleName());
			startActivity(intent);
		}
	}
}
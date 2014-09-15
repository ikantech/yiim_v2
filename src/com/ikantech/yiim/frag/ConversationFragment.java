package com.ikantech.yiim.frag;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.ikantech.support.util.YiLog;
import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.ConversationAdater;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.runnable.XmppAddEntryRunnable;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.ChatActivity;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class ConversationFragment extends YiFragment {
	private static final int MSG_RELOAD_CONVERSATION_SUCCESS = 0x01;
	private static final int MSG_CHECK_DATA = 0x02;

	private View mRootView;
	private ListView mConversationListView;
	private TextView mTipTextView;

	private ConversationAdater mChatsAdater;

	private MsgReceivedBroadcast mMsgReceivedBroadcast;

	private XmppBinder mXmppBinder;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mRootView = inflater.inflate(R.layout.main_tab_chats, null);

		mConversationListView = (ListView) mRootView
				.findViewById(R.id.tab_chats_list);

		mTipTextView = (TextView) mRootView.findViewById(R.id.tab_chats_tip);

		return mRootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		mConversationListView.setOnItemClickListener(new ChatItemListener());
		mConversationListView
				.setOnItemLongClickListener(new ChatItemLongClickListener());

		// 关闭Cursor
		if (mChatsAdater != null && mChatsAdater.getCursor() != null) {
			mConversationListView.setAdapter(null);
			mChatsAdater.getCursor().close();
		}
		mChatsAdater = null;

		mMsgReceivedBroadcast = new MsgReceivedBroadcast();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.NOTIFY_MSG_RECEIVED_OR_SENT);
		getActivity().registerReceiver(mMsgReceivedBroadcast, intentFilter);
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		loadConversations();
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_RELOAD_CONVERSATION_SUCCESS: {
			if (mChatsAdater != null && mChatsAdater.getCursor() != null) {
				mConversationListView.setAdapter(null);
				mChatsAdater.getCursor().close();
				mChatsAdater = null;
			} else {
				YiLog.getInstance().i("conversation cursor is null");
			}

			Cursor cursor = (Cursor) msg.obj;
			mChatsAdater = new ConversationAdater(getActivity(), cursor,
					getXmppBinder());

			checkData();

			mConversationListView.setAdapter(mChatsAdater);
			mChatsAdater.notifyDataSetChanged();
			break;
		}
		case MSG_CHECK_DATA:
			checkData();
			break;
		default:
			break;
		}
	}

	public void setXmppBinder(XmppBinder binder) {
		mXmppBinder = binder;
	}

	private class ChatItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			// TODO Auto-generated method stub
			if (mChatsAdater == null || mChatsAdater.getCount() < arg2) {
				return false;
			}
			final Cursor cursor = (Cursor) mChatsAdater.getItem(arg2);
			if (cursor != null) {
				showMsgDialog(null,
						getString(R.string.str_conversation_delete_req),
						getString(R.string.str_yes),
						getString(R.string.str_no), new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								((YiIMApplication) getActivity()
										.getApplication()).getXmppService()
										.execute(new Runnable() {
											@Override
											public void run() {
												// TODO Auto-generated method
												// stub
												getActivity()
														.getContentResolver()
														.delete(ContentUris
																.withAppendedId(
																		ConversationColumns.CONTENT_URI,
																		cursor.getLong(0)),
																null, null);
												getHandler()
														.sendEmptyMessageDelayed(
																MSG_CHECK_DATA,
																200);
											}
										});
							}
						}, null);
				return true;
			}
			return false;
		}
	}

	public XmppBinder getXmppBinder() {
		if (mXmppBinder == null) {
			try {

				YiIMApplication application = (YiIMApplication) getActivity()
						.getApplication();
				return application.getXmppService();
			} catch (Exception e) {
				// TODO: handle exception
				return null;
			}
		}
		return mXmppBinder;
	}

	private void checkData() {
		Cursor cursor = (Cursor) mChatsAdater.getCursor();

		if (cursor != null && cursor.getCount() > 0) {
			mConversationListView.setVisibility(View.VISIBLE);
			mTipTextView.setVisibility(View.GONE);
		} else {
			mConversationListView.setVisibility(View.GONE);
			mTipTextView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		// 关闭Cursor
		if (mChatsAdater != null && mChatsAdater.getCursor() != null) {
			mConversationListView.setAdapter(null);
			mChatsAdater.getCursor().close();
		}
		mChatsAdater = null;

		getActivity().unregisterReceiver(mMsgReceivedBroadcast);
		super.onDestroy();
	}

	public void loadConversations() {
		if (getXmppBinder() == null)
			return;
		YiLog.getInstance().i("loadConversations");
		if (mChatsAdater == null || mChatsAdater.getCursor() == null
				|| mChatsAdater.getCount() < 1) {
			YiLog.getInstance().i("loadConversations1");
			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						Cursor cursor = getActivity().getContentResolver()
								.query(ConversationColumns.CONTENT_URI,
										new String[] { ConversationColumns._ID,
												ConversationColumns.MSG,
												ConversationColumns.SUB_MSG,
												ConversationColumns.MSG_TYPE,
												ConversationColumns.DEALT,
												ConversationColumns.MSG_DATE },
										ConversationColumns.USER
												+ " like '"
												+ UserInfo.getUserInfo(
														getActivity())
														.getUser() + "%'",
										null, null);
						Message message = getHandler().obtainMessage(
								MSG_RELOAD_CONVERSATION_SUCCESS, cursor);
						message.sendToTarget();
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			});
		}
	}

	private class ChatItemListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			if (mChatsAdater == null || mChatsAdater.getCount() < arg2) {
				return;
			}
			final Cursor model = (Cursor) mChatsAdater.getItem(arg2);
			int type = model.getInt(model
					.getColumnIndex(ConversationColumns.MSG_TYPE));
			int dealt = model.getInt(model
					.getColumnIndex(ConversationColumns.DEALT));

			if (model != null) {
				if (type == ConversationType.ENTRY_ADD_REQUEST.getCode()
						&& dealt == 0) {
					String user = model.getString(model
							.getColumnIndex(ConversationColumns.MSG));
					int res = R.string.str_entry_add_agree;
					if (YiIMUtils.isMultChat(user)) {
						res = R.string.str_room_add_agree;
					}
					showMsgDialog(null, getString(res),
							getString(R.string.str_yes),
							getString(R.string.str_no),
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									String user = model.getString(model
											.getColumnIndex(ConversationColumns.MSG));
									if (YiIMUtils.isMultChat(user)) {
										String roomJid = StringUtils
												.escapeUserResource(user);
										boolean flag = false;
										try {
											RoomInfo roomInfo = MultiUserChat
													.getRoomInfo(
															getXmppBinder()
																	.getXmppConnection(),
															roomJid);
											if (roomInfo != null) {
												MultiChatDesc desc = MultiChatDesc.fromString(roomInfo
														.getDescription());
												getXmppBinder()
														.joinMultUserChat(
																roomJid);
												YiIMUtils
														.localJoinMultiUserChat(
																getXmppBinder()
																		.getServiceContext(),
																roomJid,
																desc.getName(),
																roomInfo.getDescription());
												ContentValues values = new ContentValues();
												values.put(
														ConversationColumns.DEALT,
														1);
												getActivity()
														.getContentResolver()
														.update(ContentUris
																.withAppendedId(
																		ConversationColumns.CONTENT_URI,
																		model.getLong(0)),
																values, null,
																null);
												flag = true;
											}
										} catch (Exception e) {
											// TODO: handle exception
										}
										if (!flag) {
											showMsgDialog(R.string.err_join_invite_room_failed);
										}
									} else {
										XmppAddEntryRunnable runnable = new XmppAddEntryRunnable(
												getActivity(),
												StringUtils
														.escapeUserResource(user),
												null);
										// runnable.setGroupName(groupName);
										runnable.setRequest(false);
										getXmppBinder().execute(runnable);
									}
								}
							}, null);
				} else if (type == ConversationType.CHAT_RECORD.getCode()) {
					Intent intent = new Intent(getActivity(),
							ChatActivity.class);
					intent.putExtra(
							"to",
							model.getString(
									model.getColumnIndex(ConversationColumns.MSG))
									.replaceAll("/.+$", ""));
					startActivity(intent);
				}
			}
		}
	}

	private class MsgReceivedBroadcast extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Const.NOTIFY_MSG_RECEIVED_OR_SENT)) {
				getHandler().post(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (mChatsAdater == null
								|| mChatsAdater.getCursor() == null
								|| mChatsAdater.getCursor().getCount() < 1) {
							loadConversations();
						}
						getHandler().sendEmptyMessageDelayed(MSG_CHECK_DATA,
								200);
					}
				});
			}
		}
	}
}

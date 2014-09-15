package com.ikantech.yiim.ui;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ListView;

import com.ikantech.support.util.YiHanziToPinyin;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.TabContactsAdapter;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.entity.TabContactsModel;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.YiIMUtils;

public class MultiRoomDiscoverActivity extends CustomTitleActivity {
	private static final int MSG_UPDATE_LIST = 0x01;

	private ListView mListView;
	private TabContactsAdapter mAdapter;

	private EditText mSearchEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_multi_room_discover);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_UPDATE_LIST:
			mAdapter.setDatas((ArrayList<TabContactsModel>) msg.obj);
			mAdapter.notifyDataSetChanged();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mListView = (ListView) findViewById(R.id.multi_rooms_list);
		mSearchEditText = (EditText) findViewById(R.id.multi_rooms_search_edit);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mAdapter = new TabContactsAdapter(this,
				new ArrayList<TabContactsModel>());
		mListView.setAdapter(mAdapter);

		loadRooms();
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				final TabContactsModel model = (TabContactsModel) mAdapter
						.getItem(arg2);
				if (model != null) {
					showMsgDialog(null,
							getString(R.string.tip_join_multi_room),
							getString(R.string.str_ok),
							getString(R.string.str_cancel),
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									getXmppBinder().joinMultUserChat(
											model.getUser());
									int ret = YiIMUtils.localJoinMultiUserChat(
											MultiRoomDiscoverActivity.this,
											model.getUser(), model.getMsg(),
											model.getSubMsg());
									if (ret == -1) {
										showMsgDialog(R.string.err_join_multi_room_failed);
									} else if (ret == -2) {
										showMsgDialog(R.string.err_join_multi_room_twice);
									} else {
										showMsgDialog(R.string.tip_join_multi_room_success);
									}
								}
							}, null);
				}
			}
		});

		mSearchEditText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_GO) {
					onSearchClick(null);
				}
				return false;
			}
		});
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	public void onSearchClick(View view) {
		if (isStringInvalid(mSearchEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_search_content),
					getString(R.string.str_ok));
			return;
		}

		ArrayList<TabContactsModel> models = new ArrayList<TabContactsModel>();
		try {
			String roomJid = YiHanziToPinyin.getPinYin(mSearchEditText
					.getText().toString())
					+ "@conference."
					+ XmppConnectionUtils.getXmppHost();
			RoomInfo roomInfo = MultiUserChat.getRoomInfo(getXmppBinder()
					.getXmppConnection(), roomJid);
			if (roomInfo != null) {
				TabContactsModel model = new TabContactsModel();

				MultiChatDesc desc = MultiChatDesc.fromString(roomInfo
						.getDescription());

				if (!YiUtils.isStringInvalid(roomInfo.getSubject())) {
					model.setMsg(roomInfo.getSubject());
				} else if (!YiUtils.isStringInvalid(desc.getName())) {
					model.setMsg(desc.getName());
				} else {
					model.setMsg(roomInfo.getRoom());
				}
				model.setUser(roomInfo.getRoom());
				model.setSubMsg(roomInfo.getDescription());
				models.add(model);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			Message message = getHandler().obtainMessage(MSG_UPDATE_LIST,
					models);
			message.sendToTarget();
		}
	}

	private void loadRooms() {
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Collection<HostedRoom> hostedRooms = MultiUserChat
							.getHostedRooms(
									getXmppBinder().getXmppConnection(),
									"conference."
											+ XmppConnectionUtils.getXmppHost());
					if (!hostedRooms.isEmpty()) {
						ArrayList<TabContactsModel> models = new ArrayList<TabContactsModel>();
						for (HostedRoom hostedRoom : hostedRooms) {
							RoomInfo roomInfo = MultiUserChat.getRoomInfo(
									getXmppBinder().getXmppConnection(),
									hostedRoom.getJid());
							TabContactsModel model = new TabContactsModel();

							MultiChatDesc desc = MultiChatDesc
									.fromString(roomInfo.getDescription());

							if (!YiUtils.isStringInvalid(roomInfo.getSubject())) {
								model.setMsg(roomInfo.getSubject());
							} else if (!YiUtils.isStringInvalid(desc.getName())) {
								model.setMsg(desc.getName());
							} else {
								model.setMsg(roomInfo.getRoom());
							}
							model.setUser(roomInfo.getRoom());
							model.setSubMsg(roomInfo.getDescription());
							models.add(model);
						}
						Message message = getHandler().obtainMessage(
								MSG_UPDATE_LIST, models);
						message.sendToTarget();
					}
				} catch (Exception e) {
					// TODO: handle exception
					YiLog.getInstance().e(e, "load rooms failed");
				}
			}
		});
	}
}

package com.ikantech.yiim.ui;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.TabContactsAdapter;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.TabContactsModel;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.StringUtils;

public class RoomMemberActivity extends CustomTitleActivity {
	private static final int MSG_UPDATE_LIST = 0x01;

	private ListView mListView;

	private String mRoomJid;

	private ArrayList<TabContactsModel> mModels = null;
	private TabContactsAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_room_member);
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
		mListView = (ListView) findViewById(R.id.room_member_list);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mRoomJid = getIntent().getStringExtra("room_jid");

		if (YiUtils.isStringInvalid(mRoomJid)) {
			finish();
			return;
		}

		mModels = new ArrayList<TabContactsModel>();
		mAdapter = new TabContactsAdapter(this, mModels);
		mListView.setAdapter(mAdapter);

		getXmppBinder().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Iterator<String> members = getXmppBinder().getRoomMembers(
						mRoomJid);
				if (members != null) {
					ArrayList<TabContactsModel> models = new ArrayList<TabContactsModel>();
					while (members.hasNext()) {
						String userJid = StringUtils.getJidResouce(members
								.next())
								+ "@"
								+ XmppConnectionUtils.getXmppHost();

						TabContactsModel model = new TabContactsModel();

						XmppVcard vCard = new XmppVcard(getXmppBinder()
								.getServiceContext());
						vCard.load(getXmppBinder().getXmppConnection(), userJid);

						model.setMsg(vCard.getDisplayName());

						model.setSubMsg(vCard.getSign());

						model.setUser(userJid);

						models.add(model);
					}
					Message message = getHandler().obtainMessage(
							MSG_UPDATE_LIST, models);
					message.sendToTarget();
				}
			}
		});
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				TabContactsModel model = (TabContactsModel) mAdapter
						.getItem(arg2);
				if (model != null) {
					Intent intent = new Intent(RoomMemberActivity.this,
							UserInfoActivity.class);
					intent.putExtra("user", model.getUser());
					intent.putExtra("which",
							RoomMemberActivity.class.getSimpleName());
					startActivity(intent);
				}
			}
		});
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

}

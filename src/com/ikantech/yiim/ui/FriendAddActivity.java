package com.ikantech.yiim.ui;

import java.util.ArrayList;
import java.util.Iterator;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.FriendAddAdater;
import com.ikantech.yiim.app.YiIMApplication;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.entity.FriendAddModel;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.StringUtils;

public class FriendAddActivity extends CustomTitleActivity {
	private static final int MSG_ON_SEARCH_SUCCESS = 0x01;

	private EditText mSearchEditText;
	private FriendAddAdater mAdater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_friend_add);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onUIXmppResponse(XmppResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mSearchEditText = (EditText) findViewById(R.id.friend_add_search_edit);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mAdater = new FriendAddAdater(this, new ArrayList<FriendAddModel>());
		ListView listView = (ListView) findViewById(R.id.friend_add_list);
		listView.setAdapter(mAdater);
		listView.setOnItemClickListener(new FriendAddItemListener());
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
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

	private void searchEjaaberd() {
		// 此处一定要加上 search.
		try {
			Connection connection = getXmppBinder().getXmppConnection();
			UserSearchManager search = new UserSearchManager(connection);
			Form searchForm = search.getSearchForm("vjud."
					+ connection.getServiceName());
			Form answerForm = searchForm.createAnswerForm();
			// answerForm.setAnswer("nick", mSearchEditText.getText()
			// .toString().trim() + "*");
			answerForm.setAnswer("user", mSearchEditText.getText().toString()
					.trim() + "*");
			// answerForm.setAnswer("search", mSearchEditText.getText()
			// .toString().trim());
			ReportedData data = search.getSearchResults(answerForm, "vjud."
					+ connection.getServiceName());
			Iterator<Row> it = data.getRows();
			Row row = null;

			ArrayList<FriendAddModel> list = new ArrayList<FriendAddModel>();
			while (it.hasNext()) {
				row = it.next();
				String userId = StringUtils.escapeUserResource(row
						.getValues("jid").next().toString());
				FriendAddModel model = new FriendAddModel(userId);
				model.setMsg(userId);
				String nick = row.getValues("nick").next().toString();
				if (!YiUtils.isStringInvalid(nick)) {
					model.setName(nick);
				}

				XmppVcard vCard = new XmppVcard(getXmppBinder()
						.getServiceContext());
				vCard.load(connection, model.getMsg());

				// 加载用户的个性签名
				String sign = vCard.getSign();
				if (sign != null && sign.length() > 0) {
					if (model.getSubMsg().length() > 1) {
						sign = ' ' + sign;
					}
					model.setSubMsg(model.getSubMsg() + sign);
				}
				list.add(model);
			}
			Message message = getHandler().obtainMessage(MSG_ON_SEARCH_SUCCESS,
					list);
			message.sendToTarget();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private void searchOpenfire() {
		// 此处一定要加上 search.
		try {
			Connection connection = getXmppBinder().getXmppConnection();
			UserSearchManager search = new UserSearchManager(connection);
			Form searchForm = search.getSearchForm("search."
					+ connection.getServiceName());
			Form answerForm = searchForm.createAnswerForm();
			answerForm.setAnswer("Username", true);
			answerForm.setAnswer("search", mSearchEditText.getText().toString()
					.trim());
			ReportedData data = search.getSearchResults(answerForm, "search."
					+ connection.getServiceName());
			Iterator<Row> it = data.getRows();
			Row row = null;

			ArrayList<FriendAddModel> list = new ArrayList<FriendAddModel>();
			while (it.hasNext()) {
				row = it.next();
				String userId = row.getValues("Username").next().toString()
						+ "@" + XmppConnectionUtils.getXmppHost();
				FriendAddModel model = new FriendAddModel(userId);
				model.setMsg(userId);
				model.setName(row.getValues("Name").next().toString());

				XmppVcard vCard = new XmppVcard(getXmppBinder()
						.getServiceContext());
				vCard.load(connection, userId);

				// 加载用户的个性签名
				String sign = vCard.getSign();
				if (sign != null && sign.length() > 0) {
					if (model.getSubMsg().length() > 1) {
						sign = ' ' + sign;
					}
					model.setSubMsg(model.getSubMsg() + sign);
				}
				list.add(model);
			}
			Message message = getHandler().obtainMessage(MSG_ON_SEARCH_SUCCESS,
					list);
			message.sendToTarget();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void onSearchClick(View view) {
		if (isStringInvalid(mSearchEditText.getText())) {
			showMsgDialog(getString(R.string.err_empty_search_content),
					getString(R.string.str_ok));
			return;
		}

		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (YiIMApplication.USE_OPENIRE) {
					searchOpenfire();
				} else {
					searchEjaaberd();
				}
			}
		});
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_ON_SEARCH_SUCCESS:
			ArrayList<FriendAddModel> list = (ArrayList<FriendAddModel>) msg.obj;
			if (list != null) {
				mAdater.getDatas().clear();
				mAdater.notifyDataSetChanged();
				for (FriendAddModel friendAddModel : list) {
					mAdater.getDatas().add(friendAddModel);
					mAdater.notifyDataSetChanged();
				}
			}
			break;
		default:
			break;
		}
	}

	private class FriendAddItemListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			if (mAdater == null || mAdater.getCount() < arg2) {
				return;
			}

			final FriendAddModel model = (FriendAddModel) mAdater.getItem(arg2);
			if (model != null) {
				Intent intent = new Intent(FriendAddActivity.this,
						UserInfoActivity.class);
				intent.putExtra("user", String.format("%s@%s",
						StringUtils.escapeUserHost(model.getMsg()),
						XmppConnectionUtils.getXmppHost()));
				intent.putExtra("name", model.getName());
				intent.putExtra("which",
						FriendAddActivity.class.getSimpleName());
				startActivity(intent);
			}
		}
	}
}

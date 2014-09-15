package com.ikantech.yiim.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.YiIMUtils;

public class GroupManagerActivity extends CustomTitleActivity {
	private static final int DIALOG_MODE_ADD_GROUP = 0x01;
	private static final int DIALOG_MODE_RENAME_GROUP = 0x02;

	private ListView mListView;
	private SimpleCursorAdapter mAdapter;
	private Cursor mCursor;

	private EditText mGroupEditText;
	private Dialog mGroupDialog;

	// 用于区分是分组管理，还是变更分组
	private String mMode = null;
	private long mGroupId = -1;
	private long mSelectedGroupId = -1;
	private long mRosterId = -1;

	private long mCurrentSelectGroupId = -1;
	private String mCurrentSelectGroupName = null;

	private int mDialogMode = -1;

	private DeleteGroupListener mDeleteGroupListener;
	private RenameGroupListener mRenameGroupListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_group_manager);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		if (mGroupDialog != null && mGroupDialog.isShowing()) {
			mGroupDialog.dismiss();
		}
		super.onDestroy();
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
		mListView = (ListView) findViewById(R.id.group_manager_list);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub

		mMode = getIntent().getStringExtra("mode");
		mGroupId = getIntent().getLongExtra("groupId", -1);
		mRosterId = getIntent().getLongExtra("rosterId", -1);
		mSelectedGroupId = mGroupId;

		mDeleteGroupListener = new DeleteGroupListener();
		mRenameGroupListener = new RenameGroupListener();

		if (mMode != null && "modify".equals(mMode)) {
			View view = findViewById(R.id.group_manager_add);
			view.setVisibility(View.GONE);

			setTitle(getString(R.string.str_modify_group_title));

			setTitleBarRightBtnText(getString(R.string.str_finish));
		}

		mCursor = getContentResolver()
				.query(RosterGroupColumns.CONTENT_URI,
						new String[] { RosterGroupColumns._ID,
								RosterGroupColumns.NAME },
						RosterGroupColumns.OWNER + "='"
								+ UserInfo.getUserInfo(this).getUser() + "'", null, null);

		mAdapter = new NativeAdapter(this, R.layout.group_manager_item,
				mCursor, new String[] { RosterGroupColumns.NAME },
				new int[] { R.id.text });

		mListView.setAdapter(mAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// 变更分组
				if (mMode != null && "modify".equals(mMode)) {
					mSelectedGroupId = ((Cursor) mAdapter.getItem(arg2))
							.getLong(0);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
	}

	@Override
	protected void installListeners() {

	}

	@Override
	protected void uninstallListeners() {

	}

	public void onAddGroupBtnClick(View view) {
		mDialogMode = DIALOG_MODE_ADD_GROUP;
		showGroupDialog();
	}

	private void showGroupDialog() {
		if (mGroupEditText == null) {
			mGroupEditText = new EditText(this);
			mGroupEditText.setHint(R.string.str_group_name_hint);
		}

		if (mGroupDialog == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(mGroupEditText);
			builder.setPositiveButton(getString(R.string.str_ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (YiUtils.isStringInvalid(mGroupEditText
									.getText().toString())) {
								showMsgDialog(R.string.err_empty_group_name);
								return;
							}

							if (mDialogMode == DIALOG_MODE_ADD_GROUP) {
								int ret = YiIMUtils.addGroup(
										GroupManagerActivity.this,
										mGroupEditText.getText().toString());
								int res = -1;
								switch (ret) {
								case -1:
									res = R.string.err_multi_group_name;
									break;
								case -2:
									res = R.string.err_add_group_name_failed;
									break;
								default:
									break;
								}
								if (res > 0) {
									showMsgDialog(res);
								}
							} else if (mDialogMode == DIALOG_MODE_RENAME_GROUP
									&& mCurrentSelectGroupId > 0
									&& !YiUtils
											.isStringInvalid(mCurrentSelectGroupName)) {
								int ret = YiIMUtils.renameGroup(
										GroupManagerActivity.this,
										mCurrentSelectGroupId, mGroupEditText
												.getText().toString());
								switch (ret) {
								case -2:
									showMsgDialog(R.string.err_rename_group_default);
									break;
								case -1:
									showMsgDialog(R.string.err_rename_group_failed);
									break;
								case -3:
									showMsgDialog(R.string.err_multi_group_name);
								default:
									break;
								}
							}
							mDialogMode = -1;
							mCurrentSelectGroupId = -1;
							if (mCurrentSelectGroupName != null) {
								mGroupEditText.setText("");
							}
							mCurrentSelectGroupName = null;
						}
					});
			builder.setNegativeButton(getString(R.string.str_cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							mDialogMode = -1;
							mCurrentSelectGroupId = -1;
							if (mCurrentSelectGroupName != null) {
								mGroupEditText.setText("");
							}
							mCurrentSelectGroupName = null;
						}
					});
			mGroupDialog = builder.create();
		}

		if (mDialogMode == DIALOG_MODE_ADD_GROUP) {
			mGroupDialog.setTitle(R.string.str_add_group);
		} else if (mDialogMode == DIALOG_MODE_RENAME_GROUP) {
			mGroupDialog.setTitle(R.string.str_rename_group);
			mGroupEditText.setText(mCurrentSelectGroupName);
		}

		mGroupDialog.show();
	}

	@Override
	public void onTitleBarRightBtnClick(View view) {
		// 变更分组
		if (mMode != null && "modify".equals(mMode)) {
			int ret = -1;
			if (mSelectedGroupId != mGroupId && mRosterId != -1) {
				ret = YiIMUtils.moveToGroup(GroupManagerActivity.this,
						mRosterId, mGroupId, mSelectedGroupId);
			}
			if (ret != 0) {
				showMsgDialog(R.string.err_modify_group_failed);
			} else {
				finish();
			}
		}
	}

	private class DeleteGroupListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			try {
				final long id = (Long) v.getTag(R.id.group_id);
				final String name = (String) v.getTag(R.id.group_name);
				if ("unfiled".equals(name)) {
					showMsgDialog(R.string.err_del_group_default);
					return;
				}
				showMsgDialog(null, getString(R.string.str_del_group_tip),
						getString(R.string.str_ok),
						getString(R.string.str_cancel),
						new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								int ret = YiIMUtils.deleteGroup(
										GroupManagerActivity.this, id);
								switch (ret) {
								case -2:
									showMsgDialog(R.string.err_del_group_default);
									break;
								case -1:
									showMsgDialog(R.string.err_del_group_failed);
									break;
								default:
									break;
								}
							}
						}, null);

			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	private class RenameGroupListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			try {
				mCurrentSelectGroupId = (Long) v.getTag(R.id.group_id);
				mCurrentSelectGroupName = (String) v.getTag(R.id.group_name);
				if ("unfiled".equals(mCurrentSelectGroupName)) {
					showMsgDialog(R.string.err_rename_group_default);
					return;
				}

				mDialogMode = DIALOG_MODE_RENAME_GROUP;
				showGroupDialog();
			} catch (Exception e) {
			}
		}
	}

	private class NativeAdapter extends SimpleCursorAdapter {

		public NativeAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// TODO Auto-generated method stub
			super.bindView(view, context, cursor);

			ViewHolder holder = (ViewHolder) view.getTag();

			if ("unfiled".equals(holder.mainTextView.getText())) {
				holder.mainTextView.setText(R.string.str_my_friend);
			}

			// 变更分组
			if (mMode != null && "modify".equals(mMode)) {
				holder.delBtn.setVisibility(View.GONE);
				holder.moreBtn.setVisibility(View.GONE);

				if (cursor.getLong(0) == mSelectedGroupId) {
					holder.ratio.setVisibility(View.VISIBLE);
				} else {
					holder.ratio.setVisibility(View.GONE);
				}
			} else {
				holder.delBtn.setTag(R.id.group_id, cursor.getLong(0));
				holder.delBtn.setTag(R.id.group_name, cursor.getString(1));
				holder.moreBtn.setTag(R.id.group_id, cursor.getLong(0));
				holder.moreBtn.setTag(R.id.group_name, cursor.getString(1));

				holder.delBtn.setOnClickListener(mDeleteGroupListener);
				holder.moreBtn.setOnClickListener(mRenameGroupListener);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// TODO Auto-generated method stub
			View rootView = super.newView(context, cursor, parent);

			ViewHolder holder = new ViewHolder();
			holder.mainTextView = (TextView) rootView.findViewById(R.id.text);
			holder.delBtn = (ImageButton) rootView
					.findViewById(R.id.group_manager_del);
			holder.moreBtn = (ImageButton) rootView
					.findViewById(R.id.group_manager_more);
			holder.ratio = rootView.findViewById(R.id.ratio);

			rootView.setTag(holder);

			return rootView;
		}

		private class ViewHolder {
			TextView mainTextView;
			ImageButton delBtn;
			ImageButton moreBtn;

			View ratio;
		}
	}
}

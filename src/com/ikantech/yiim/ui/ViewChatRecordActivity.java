package com.ikantech.yiim.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.media.AudioPlayer;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.ChatRecordAdapter;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.provider.MsgManager;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.provider.XmppProvider.DatabaseHelper;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.util.YiIMUtils;
import com.ikantech.yiim.widget.ViewImageDialog;

public class ViewChatRecordActivity extends CustomTitleActivity {
	private static final int PAGE_SIZE = 15;

	private ListView mListView;
	private EditText mEditText;
	private TextView mTextView;

	private String mUser;

	private int mMaxPages = 0;
	private int mCurrentPage = 0;

	private Cursor mCursor;
	private ChatRecordAdapter mCursorAdapter;
	private DatabaseHelper mDatabaseHelper;

	private ViewImageDialog mImageDialog;

	private AudioPlayer mAudioPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.activity_view_chat_record);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		if (mImageDialog != null && mImageDialog.isShowing()) {
			mImageDialog.dismiss();
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
		mListView = (ListView) findViewById(R.id.view_chat_record_list);
		mEditText = (EditText) findViewById(R.id.view_chat_record_edit);
		mTextView = (TextView) findViewById(R.id.view_chat_record_text);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mUser = getIntent().getStringExtra("user");

		Cursor cursor = null;
		try {
			String currentUser = UserInfo.getUserInfo(this).getUser();
			cursor = getContentResolver().query(
					MsgColumns.CONTENT_URI,
					new String[] { MsgColumns._ID, MsgColumns.CONTENT },
					"(" + MsgColumns.SENDER + " like '" + currentUser
							+ "%' and " + MsgColumns.RECEIVER + " like '"
							+ mUser + "%') or (" + MsgColumns.SENDER
							+ " like '" + mUser + "%' and "
							+ MsgColumns.RECEIVER + " like '" + currentUser
							+ "%')", null, null);
			if (cursor != null) {
				mMaxPages = (int) Math.ceil(cursor.getCount() * 1.0F
						/ PAGE_SIZE);
				mCurrentPage = mMaxPages;
			}
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}

		mTextView.setText("/" + mMaxPages);
		mEditText.setText("" + mCurrentPage);
		loadPage();
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mEditText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_GO) {
					try {
						Integer page = Integer.valueOf(mEditText.getText()
								.toString());
						if (page > 0 && page <= mMaxPages) {
							mCurrentPage = page;
							loadPage();
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				return false;
			}
		});
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}

	public void onPrevClick(View v) {
		int old = mCurrentPage;
		mCurrentPage--;
		if (mCurrentPage < 1) {
			mCurrentPage = 1;
		}
		if (old != mCurrentPage) {
			loadPage();
		}
	}

	public void onNextClick(View v) {
		int old = mCurrentPage;
		mCurrentPage++;
		if (mCurrentPage > mMaxPages) {
			mCurrentPage = mMaxPages;
		}
		if (old != mCurrentPage) {
			loadPage();
		}
	}

	public void loadPage() {
		if (mMaxPages < 1)
			return;
		if (mCurrentPage < 1 || mCurrentPage > mMaxPages)
			return;

		if (mDatabaseHelper == null) {
			mDatabaseHelper = new DatabaseHelper(this);
		}

		String currentUser = UserInfo.getUserInfo(this).getUser();

		Cursor oldCursor = mCursor;

		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();

		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		builder.append(MsgColumns._ID);
		builder.append(',');
		builder.append(MsgColumns.SENDER);
		builder.append(',');
		builder.append(MsgColumns.CONTENT);
		builder.append(',');
		builder.append(MsgColumns.CREATE_DATE);
		// builder.append(',');
		// builder.append(MsgColumns.SENDER);
		builder.append(" from ");
		builder.append(MsgManager.TABLE_NAME);
		builder.append(" where ");
		builder.append("(" + MsgColumns.SENDER + " like '" + currentUser
				+ "%' and " + MsgColumns.RECEIVER + " like '" + mUser
				+ "%') or (" + MsgColumns.SENDER + " like '" + mUser
				+ "%' and " + MsgColumns.RECEIVER + " like '" + currentUser
				+ "%')");
		builder.append(" order by " + MsgColumns.LOCAL_DATE + " asc ");
		builder.append(" limit ");
		builder.append("" + PAGE_SIZE);
		builder.append(" offset ");
		builder.append("" + ((mCurrentPage - 1) * PAGE_SIZE));

		mCursor = db.rawQuery(builder.toString(), null);

		/*
		 * mCursor = getContentResolver() .query(uri, new String[] {
		 * MsgColumns._ID, MsgColumns.SENDER, MsgColumns.CONTENT,
		 * MsgColumns.CREATE_DATE }, "(" + MsgColumns.SENDER + " like '" +
		 * currentUser + "%' and " + MsgColumns.RECEIVER + " like '" + mUser +
		 * "%') or (" + MsgColumns.SENDER + " like '" + mUser + "%' and " +
		 * MsgColumns.RECEIVER + " like '" + currentUser + "%')", null,
		 * MsgColumns.LOCAL_DATE + " desc");
		 */

		if (mCursorAdapter == null) {
			mCursorAdapter = new ChatRecordAdapter(this, mCursor,
					getXmppBinder());
			mCursorAdapter
					.setOnAudioClickListener(new NativeAudioClickListener());
			mCursorAdapter.setOnImageClickListener(new ImageClickListener());
			mListView.setAdapter(mCursorAdapter);
		} else {
			mCursorAdapter.changeCursor(mCursor);
		}
		mCursorAdapter.notifyDataSetChanged();

		/*
		 * if (oldCursor != null) { oldCursor.close(); oldCursor = null; }
		 */

		mEditText.setText("" + mCurrentPage);
	}

	public void onClearChatRecordClick(View v) {
		showMsgDialog(null, getString(R.string.str_clear_chat_record_confirm),
				getString(R.string.str_ok), getString(R.string.str_cancel),
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							YiIMUtils.deleteChatRecord(
									ViewChatRecordActivity.this, mUser);
							YiIMUtils.deleteConversation(
									ViewChatRecordActivity.this, mUser);
							mCurrentPage = 0;
							mMaxPages = 0;
							mEditText.setText("0");
							mTextView.setText("/0");
							mCursorAdapter.changeCursor(null);
							mCursorAdapter.notifyDataSetChanged();
						} catch (Exception e) {
						}
					}
				}, null);
	}

	private class ImageClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			String filePath = (String) v.getTag();
			if (!YiUtils.isStringInvalid(filePath)) {
				if (mImageDialog == null) {
					mImageDialog = new ViewImageDialog(
							ViewChatRecordActivity.this,
							R.style.ImageViewDialog);
				}
				mImageDialog.setBitmapPath(filePath);
				mImageDialog.show();
			}
		}
	}

	private class NativeAudioClickListener implements View.OnClickListener {
		@Override
		public void onClick(final View v) {
			// TODO Auto-generated method stub
			getXmppBinder().execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (mAudioPlayer == null) {
						mAudioPlayer = new AudioPlayer();
					}
					if (mAudioPlayer.getMediaPlayer() != null) {
						mAudioPlayer.stopPlaying();
					}
					try {
						mAudioPlayer.startPlaying((String) v.getTag());
					} catch (Exception e) {
					}
				}
			});
		}
	}
}

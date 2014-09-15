package com.ikantech.yiim.ui.base;

import java.io.File;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.gif.GifEmotionUtils;
import com.ikantech.xmppsupport.media.AudioPlayer;
import com.ikantech.xmppsupport.media.AudioRecorder;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.adapter.ChatMsgViewAdapter;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.EmotionManager;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.MsgManager;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.ui.ChatActivity;
import com.ikantech.yiim.ui.UserInfoActivity;
import com.ikantech.yiim.util.FileUpload;
import com.ikantech.yiim.util.FileUtils;
import com.ikantech.yiim.util.MediaFile;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;
import com.ikantech.yiim.widget.ViewImageDialog;
import com.ikantech.yiim.widget.VoiceRecordDialog;
import com.ikantech.yiim.widget.VoiceRecordDialog.OnErrorListener;

public abstract class BaseChatActivity extends CustomTitleActivity implements
		OnErrorListener {

	protected static final int MSG_INIT = 0x01;
	protected static final int MSG_RECV = 0x02;
	protected static final int MSG_RECORD_READED = 0x03;
	public static final int MSG_SEND_EMOTION = 0x04;
	public static final int MSG_SEND_CLASSICAL_EMOTION = 0x05;
	public static final int MSG_SEND_AUDIO = 0x06;

	private static final int MSG_CHECK_NATWORK_TYPE = 0x07;

	// 显示最近20条记录
	private static final int LIMIT = 20;

	private EditText mEditTextContent;
	private ListView mListView;
	// 底部工具栏和表情
	private View mFooterView;

	// 工具栏
	private View mToolsView;

	// 底部pager
	private ViewPager mFooterPager;
	private View mPagerIndexPanel;
	protected EmotionManager mEmotionManager;

	protected ChatMsgViewAdapter mAdapter;

	private Button mVoiceButton;
	private View mPlanTextRootView;
	private InputMethodManager mInputMethodManager;
	private VoiceRecordDialog mVoiceRecordDialog;

	private ViewImageDialog mImageDialog;
	private AudioRecorder mAudioRecorder;

	private static final int CHOICE_PHOTO = 0; // 用于标识从用户相册选择照片
	private static final int TAKE_PHOTO = 1; // 用于标识拍照
	private static final int CHOICE_VIDEO = 2;// 选择视频文件

	private AudioPlayer mAudioPlayer;

	protected String mUserTo;

	protected UserInfo mUser;

	protected GifEmotionUtils mGifEmotionUtils;
	private MsgReceivedBroadcast mMsgReceivedBroadcast;

	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activty_chat);
		super.onCreate(savedInstanceState);
		mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		getHandler().sendEmptyMessage(MSG_INIT);
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_INIT:
			init();
			break;
		case MSG_RECV:
			clearConversationDealFlag();
			break;
		case MSG_RECORD_READED:
			if (mAdapter != null && mAdapter.getCursor() != null) {
				mListView.setAdapter(null);
				mAdapter.getCursor().close();
				mAdapter = null;
			}
			initAdapter(msg.obj);
			mAdapter.setOnAudioClickListener(new NativeAudioClickListener());
			mAdapter.setOnImageClickListener(new ImageClickListener());
			mAdapter.setOnVideoClickListener(new NativeVideoClickListener());
			mAdapter.setResendClickListener(new ResendImageClickListener());
			mAdapter.setIsMultiChat(YiIMUtils.isMultChat(mUserTo));
			mListView.setAdapter(mAdapter);
			mAdapter.notifyDataSetChanged();
			// mListView.setSelection(mAdapter.getCount() - 1);
			break;
		case MSG_SEND_EMOTION:
			try {
				YiIMMessage message = new YiIMMessage();
				message.setType(MsgType.BIG_EMOTION);
				message.setBody((String) msg.obj);
				sendYiIMMessage(message.toString());
			} catch (Exception e) {
				// TODO: handle exception
			}
			mFooterView.setVisibility(View.GONE);
			break;
		case MSG_SEND_AUDIO:
			sendAudioMsg();
			break;
		case MSG_SEND_CLASSICAL_EMOTION:
			if ("[DEL]".equals((String) msg.obj)) {
				int keyCode = KeyEvent.KEYCODE_DEL;
				KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN,
						keyCode);
				KeyEvent keyEventUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
				mEditTextContent.onKeyDown(keyCode, keyEventDown);
				mEditTextContent.onKeyUp(keyCode, keyEventUp);
			} else {
				int index = mEditTextContent.getSelectionStart();
				Editable editable = mEditTextContent.getEditableText();
				if (index < 0 || index >= editable.length()) {
					editable.append((String) msg.obj);
				} else {
					editable.insert(index, (String) msg.obj);
				}
				mGifEmotionUtils.setSpannableText(mEditTextContent,
						mEditTextContent.getText().toString(), getHandler());
			}
			break;
		case MSG_CHECK_NATWORK_TYPE:
			if (getXmppBinder().isAVCallOK()) {
				cancelProgressDialog();
			} else {
				getHandler().sendEmptyMessageDelayed(MSG_CHECK_NATWORK_TYPE,
						200);
			}
			break;
		default:
			break;
		}
	}

	protected abstract void initAdapter(Object obj);

	protected void clearConversationDealFlag() {
		getXmppBinder().executeDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				// 清除未查看标志
				Cursor cursor = null;
				try {
					cursor = getContentResolver().query(
							ConversationColumns.CONTENT_URI,
							new String[] { ConversationColumns._ID,
									ConversationColumns.DEALT },
							ConversationColumns.USER
									+ " like '"
									+ UserInfo.getUserInfo(
											BaseChatActivity.this).getUser()
									+ "%' AND " + ConversationColumns.MSG
									+ " like '" + mUserTo + "%' and "
									+ ConversationColumns.MSG_TYPE + "=?",
							new String[] { ConversationType.CHAT_RECORD
									.toString() }, null);

					if (cursor != null) {
						if (cursor.getCount() == 1) {
							cursor.moveToFirst();
							ContentValues values = new ContentValues();
							values.put(ConversationColumns.DEALT, 0);
							getContentResolver().update(
									ContentUris.withAppendedId(
											ConversationColumns.CONTENT_URI,
											cursor.getLong(0)), values, null,
									null);
							getXmppBinder().updateNotification();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					if (cursor != null) {
						cursor.close();
						cursor = null;
					}
				}
			}
		}, 200);
	}

	@Override
	protected void initViews() {
		// TODO Auto-generated method stub
		mListView = (ListView) findViewById(R.id.chat_listview);
		mEditTextContent = (EditText) findViewById(R.id.chat_msg_edit);
		mFooterView = findViewById(R.id.chat_footer);
		mFooterPager = (ViewPager) findViewById(R.id.chat_viewpager);
		mToolsView = findViewById(R.id.chat_tools);
		mPagerIndexPanel = findViewById(R.id.chat_pager_index);
		mPlanTextRootView = findViewById(R.id.chat_plantext_root);
		mVoiceButton = (Button) findViewById(R.id.chat_btn_voice);
	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub
		mAudioRecorder = new AudioRecorder();

		mUserTo = getIntent().getStringExtra("to");
		setTitle(mUserTo.replaceAll("@.+$", ""));
		setTitleBarRightImageBtnSrc(R.drawable.mm_title_btn_contact_normal);

		mEmotionManager = new EmotionManager(this, mFooterPager,
				mPagerIndexPanel, findViewById(R.id.chat_pager_emoji_toolbar),
				getHandler());

		mGifEmotionUtils = new GifEmotionUtils(this,
				EmotionManager.getClassicalEmotions(),
				EmotionManager.getClassicalEmotionDescs(), R.drawable.face);

		mUser = UserInfo.getUserInfo(this);
	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub
		mVoiceButton.setOnTouchListener(new OnTouchListener() {
			private float lastX;
			private float lastY;
			private Rect rect;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					rect = new Rect();
					mVoiceButton.getLocalVisibleRect(rect);
					lastX = event.getX();
					lastY = event.getY();
					mVoiceButton.setText(getString(R.string.str_voice_up));
					mVoiceButton.setBackgroundDrawable(getResources()
							.getDrawable(R.drawable.voice_rcd_btn_pressed));
					// 弹出voice dialog
					if (mVoiceRecordDialog == null) {
						mVoiceRecordDialog = new VoiceRecordDialog(
								BaseChatActivity.this,
								R.style.custom_dialog_transparent,
								mAudioRecorder);
						mVoiceRecordDialog
								.setOnErrorListener(BaseChatActivity.this);
					}
					mVoiceRecordDialog.show();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mVoiceButton.setText(getString(R.string.str_voice_press));
					mVoiceButton.setBackgroundDrawable(getResources()
							.getDrawable(R.drawable.voice_rcd_btn_nor));

					if (mVoiceRecordDialog != null
							&& mVoiceRecordDialog.isShowing()) {
						mVoiceRecordDialog.requestDismiss();
					}
					// 如果松开后，仍在按钮区域
					if (rect.contains((int) lastX, (int) lastY)) {
						getHandler().sendEmptyMessageDelayed(MSG_SEND_AUDIO,
								300);
					}
				} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
					lastX = event.getX();
					lastY = event.getY();

					if (rect.contains((int) lastX, (int) lastY)) {
						if (mVoiceRecordDialog != null) {
							mVoiceRecordDialog.showRecordingView();
						}
					} else {
						if (mVoiceRecordDialog != null) {
							mVoiceRecordDialog.showCancelRecordView();
						}
					}
				}
				return false;
			}
		});

		mEditTextContent.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				_onTypeSelectBtnClick(false);
				return false;
			}
		});

		mMsgReceivedBroadcast = new MsgReceivedBroadcast();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.NOTIFY_MSG_RECEIVED_OR_SENT);
		registerReceiver(mMsgReceivedBroadcast, intentFilter);
	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub
		unregisterReceiver(mMsgReceivedBroadcast);
	}

	public void onTypeSelectBtnClick(View view) {
		_onTypeSelectBtnClick(true);
	}

	private void _onTypeSelectBtnClick(boolean force) {
		mFooterPager.setVisibility(View.GONE);
		mToolsView.setVisibility(View.VISIBLE);
		mEmotionManager.destory();
		if (mFooterView.getVisibility() == View.GONE && force) {
			mInputMethodManager.hideSoftInputFromWindow(
					mFooterView.getWindowToken(), 0);
			mFooterView.setVisibility(View.VISIBLE);
		} else {
			mFooterView.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mFooterView.getVisibility() == View.VISIBLE) {
				_onTypeSelectBtnClick(false);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void sendAudioMsg() {
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					String msg = mAudioRecorder.getRecordedResource();
					MediaPlayer player = new MediaPlayer();
					player.setDataSource(mAudioRecorder.getAudioFilePath());
					player.prepare();

					YiIMMessage message = new YiIMMessage();
					message.setType(MsgType.AUDIO);
					message.setBody(msg);
					message.addParam("audio_duration",
							String.valueOf(player.getDuration()));

					sendYiIMMessage(message.toString());

					player.release();
					player = null;
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
	}

	protected abstract void sendYiIMMessage(String msg);

	public void onEmoBtnClick(View view) {
		mToolsView.setVisibility(View.GONE);
		mFooterPager.setVisibility(View.VISIBLE);
		mEmotionManager.initialize();
	}

	public void onVoiceChooseBtnClick(View view) {
		ImageView imageView = (ImageView) view;
		if (mVoiceButton.getVisibility() == View.GONE) {
			mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(),
					0);
			mPlanTextRootView.setVisibility(View.GONE);
			mVoiceButton.setVisibility(View.VISIBLE);
			imageView.setImageDrawable(getResources().getDrawable(
					R.drawable.chatting_setmode_keyboard_btn));
		} else {
			mVoiceButton.setVisibility(View.GONE);
			mPlanTextRootView.setVisibility(View.VISIBLE);
			imageView.setImageDrawable(getResources().getDrawable(
					R.drawable.chatting_setmode_voice_btn));
		}
	}

	protected abstract void initChat();

	private void init() {
		loadRecrod();
		initChat();
	}

	protected void loadRecrod() {
		YiLog.getInstance().i("loadRecrod");
		getXmppBinder().execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String currentUser = mUser.getUser();

				int limit = LIMIT;

				// 计算最大条数限制，默认LIMIT条，如果未查看的消息条数超过LIMIT，则limit等于未查看消息条数＋5
				Cursor cursor = null;
				try {
					cursor = getContentResolver().query(
							ConversationColumns.CONTENT_URI,
							new String[] { ConversationColumns._ID,
									ConversationColumns.DEALT },
							ConversationColumns.USER
									+ " like '"
									+ UserInfo.getUserInfo(
											BaseChatActivity.this).getUser()
									+ "%' AND " + ConversationColumns.MSG
									+ " like '" + mUserTo + "%' and "
									+ ConversationColumns.MSG_TYPE + "=?",
							new String[] { ConversationType.CHAT_RECORD
									.toString() }, null);
					if (cursor != null) {
						if (cursor.getCount() == 1) {
							cursor.moveToFirst();
							int count = cursor.getInt(1);
							if (count > LIMIT) {
								limit = count + 5;
							}

							// 清除未查看标志
							ContentValues values = new ContentValues();
							values.put(ConversationColumns.DEALT, 0);
							getContentResolver().update(
									ContentUris.withAppendedId(
											ConversationColumns.CONTENT_URI,
											cursor.getLong(0)), values, null,
									null);
							getXmppBinder().updateNotification();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				} finally {
					if (cursor != null) {
						cursor.close();
						cursor = null;
					}
				}

				Uri uri = MsgColumns.CONTENT_URI.buildUpon()
						.appendQueryParameter("limit", String.valueOf(limit))
						.build();

				cursor = getContentResolver().query(
						uri,
						new String[] { MsgColumns._ID, MsgColumns.SENDER,
								MsgColumns.RECEIVER, MsgColumns.CONTENT,
								MsgColumns.CREATE_DATE },
						"(" + MsgColumns.SENDER + " like '" + currentUser
								+ "%' and " + MsgColumns.RECEIVER + " like '"
								+ mUserTo + "%') or (" + MsgColumns.SENDER
								+ " like '" + mUserTo + "%' and "
								+ MsgColumns.RECEIVER + " like '" + currentUser
								+ "%')", null, MsgColumns.LOCAL_DATE + " desc");
				if (cursor != null) {
					Message message = getHandler().obtainMessage(
							MSG_RECORD_READED, cursor);
					message.sendToTarget();
				}
			}
		});
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (getXmppBinder() != null) {
			getXmppBinder().setActiveChat(
					StringUtils.escapeUserResource(mUserTo));
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		if (getXmppBinder() != null) {
			getXmppBinder().setActiveChat(null);
		}
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case CHOICE_PHOTO:
			if (data != null) {
				// 获取路径
				try {
					Uri originalUri = data.getData();
					// ContentResolver cr = this.getContentResolver();
					// Bitmap photoCaptured;
					Cursor cursor = null;
					/*
					 * BitmapFactory.Options options = new
					 * BitmapFactory.Options(); options.inSampleSize = 6;
					 * photoCaptured = BitmapFactory.decodeStream(
					 * (cr.openInputStream(originalUri)), null, options);
					 * photoImageView.setImageBitmap(photoCaptured);
					 */
					// 将照片显示的预览框中

					String[] proj = { MediaStore.Images.Media.DATA };
					cursor = managedQuery(originalUri, proj, null, null, null);
					int column_index = cursor
							.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					// 将光标移至开头 ，这个很重要，不小心很容易引起越界
					cursor.moveToFirst();
					// 最后根据索引值获取图片路径
					String photoPath = cursor.getString(column_index);

					sendFile(photoPath, MsgType.IMAGE);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			break;
		case CHOICE_VIDEO:
			if (data != null) {
				// 获取路径
				try {
					Uri originalUri = data.getData();

					String videoPath = originalUri.getPath();
					if (MediaFile.isVideoFileTypeFromPath(videoPath)) {
						sendFile(videoPath, MsgType.VIDEO);
					} else {
						Toast.makeText(this, "请选择视频文件!", Toast.LENGTH_SHORT)
								.show();
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		if (mImageDialog != null && mImageDialog.isShowing()) {
			mImageDialog.dismiss();
		}

		// 删除建立的聊天室
		try {

			if (mAdapter != null && mAdapter.getCursor() != null) {
				mListView.setAdapter(null);
				mAdapter.getCursor().close();
				mAdapter = null;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		mGifEmotionUtils.destory();
		super.onDestroy();
	}

	public void onSendBtnClick(View view) {
		String contString = mEditTextContent.getText().toString();
		if (contString.length() > 0) {
			sendMessage(contString);
			mEditTextContent.setText("");
			mListView.setSelection(mListView.getCount() - 1);
		}
	}

	public void sendMessage(String msg) {
		try {
			YiIMMessage message = new YiIMMessage();
			message.setBody(msg);
			sendYiIMMessage(message.toString());
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private class ResendImageClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Integer id = (Integer) v.getTag();

			Cursor msgCursor = null;
			try {
				final Uri uri = ContentUris.withAppendedId(
						MsgColumns.CONTENT_URI, id);
				msgCursor = getContentResolver().query(uri,
						new String[] { MsgColumns.CONTENT }, null, null, null);
				if (msgCursor != null && msgCursor.getCount() == 1) {
					msgCursor.moveToFirst();

					final YiIMMessage msg = YiIMMessage.fromString(msgCursor
							.getString(0));

					if (msg != null && msg.getBody() != null
							&& msg.getType().equals(MsgType.IMAGE)) {

						String status = msg.getParams().get("status");
						if (!isStringInvalid(status)
								&& !Status.complete.toString().equals(status)) {
							showMsgDialog(null, "重新发送图片",
									getString(R.string.str_ok),
									getString(R.string.str_cancel),
									new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											msg.putParam("status", "start");
											msg.putParam("progress", "0");
											ContentValues values = new ContentValues();
											values.put(MsgColumns.CONTENT,
													msg.toString());
											values.put(MsgColumns.LOCAL_DATE,
													System.currentTimeMillis());

											getContentResolver().update(uri,
													values, null, null);

											FileUpload
													.sendFile(
															getXmppBinder()
																	.getServiceContext(),
															getXmppBinder()
																	.getXmppConnection(),
															mUserTo, uri, msg
																	.getBody(),
															msg.getType());
										}
									}, null);
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (msgCursor != null) {
					msgCursor.close();
					msgCursor = null;
				}
			}
		}
	}

	private class ImageClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Integer id = (Integer) v.getTag();

			Cursor msgCursor = null;
			try {
				final Uri uri = ContentUris.withAppendedId(
						MsgColumns.CONTENT_URI, id);
				msgCursor = getContentResolver().query(uri,
						new String[] { MsgColumns.CONTENT }, null, null, null);
				if (msgCursor != null && msgCursor.getCount() == 1) {
					msgCursor.moveToFirst();

					final YiIMMessage msg = YiIMMessage.fromString(msgCursor
							.getString(0));

					if (msg != null && msg.getBody() != null
							&& msg.getType().equals(MsgType.IMAGE)) {

						if (mImageDialog == null) {
							mImageDialog = new ViewImageDialog(
									BaseChatActivity.this,
									R.style.ImageViewDialog);
						}
						mImageDialog.setBitmapPath(msg.getBody());
						mImageDialog.show();
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (msgCursor != null) {
					msgCursor.close();
					msgCursor = null;
				}
			}
		}
	}

	public void onChoicePhotoBtnClick(View view) {
		FileUtils.doChoicePhoto(this, CHOICE_PHOTO);
	}

	public void onChoiceVideoBtnClick(View view) {
		FileUtils.doChoiceVideo(this, CHOICE_VIDEO);
	}

	public void onAvatarClick(View v) {
		Intent intent = new Intent(BaseChatActivity.this,
				UserInfoActivity.class);
		intent.putExtra("user", (String) v.getTag());
		intent.putExtra("which", ChatActivity.class.getSimpleName());
		startActivity(intent);
	}

	public void onVoiceBtnClick(View v) {
		if (!getXmppBinder().isAVCallOK()) {
			showProgressDialog(getString(R.string.err_call_check_net));
			getHandler().sendEmptyMessageDelayed(MSG_CHECK_NATWORK_TYPE, 200);
			return;
		}

		Presence pre = getXmppBinder().getXmppConnection().getRoster()
				.getPresence(mUserTo);
		if (pre.getType() != Presence.Type.unavailable) {
			Intent intent = new Intent(
					"com.ikantech.xmppsupoort.jingle.ACTION_CALL");
			intent.setData(Uri.parse("xmpp:" + pre.getFrom()));
			intent.putExtra("isCaller", true);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} else {
			showMsgDialog(getString(R.string.err_call_user_not_online));
		}
	}

	public void onVideoCallClick(View v) {
		/*
		 * if (getXmppBinder().isSipOk()) { final String validUri =
		 * NgnUriUtils.makeValidSipUri(String.format( "sip:%s@%s",
		 * StringUtils.escapeUserHost(mUserTo),
		 * getXmppBinder().getSipDomain())); if (validUri == null) {
		 * onMakeAudioCallError(); }
		 * 
		 * if (!ScreenAV.makeCall(this, validUri, NgnMediaType.AudioVideo)) {
		 * onMakeAudioCallError(); } } else { onMakeAudioCallError(); }
		 */
	}

	private void sendFile(String filePath, MsgType msgType) {

		ContentValues values = new ContentValues();
		values.put(MsgManager.MsgColumns.SENDER, mUser.getUserName() + "@"
				+ XmppConnectionUtils.getXmppHost());
		values.put(MsgManager.MsgColumns.RECEIVER, mUserTo);

		YiIMMessage imMessage = new YiIMMessage();
		imMessage.addParam("status", "start");
		if (msgType == MsgType.IMAGE) {
			imMessage.setType(MsgType.IMAGE);
			imMessage.setBody(filePath);
		} else if (msgType == MsgType.VIDEO) {
			imMessage.setType(MsgType.VIDEO);
			imMessage.setBody(filePath);
		}

		values.put(MsgManager.MsgColumns.CONTENT, imMessage.toString());
		Uri uri = getContentResolver().insert(
				MsgManager.MsgColumns.CONTENT_URI, values);

		FileUpload.sendFile(getXmppBinder().getServiceContext(),
				getXmppBinder().getXmppConnection(), mUserTo, uri, filePath,
				msgType);
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

	private class NativeVideoClickListener implements View.OnClickListener {
		@Override
		public void onClick(final View v) {
			// TODO Auto-generated method stub
			Intent intent = new Intent("android.intent.action.VIEW");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("oneshot", 0);
			intent.putExtra("configchange", 0);
			Uri uri = Uri.fromFile(new File((String) v.getTag()));
			intent.setDataAndType(uri, "video/*");
			startActivity(intent);
		}
	}

	@Override
	public void onError(String msg) {
		// TODO Auto-generated method stub
		mVoiceRecordDialog.requestDismiss();
		showMsgDialog(msg);
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
						if (mAdapter == null || mAdapter.getCursor() == null) {
							loadRecrod();
						}
					}
				});

				String from = intent.getStringExtra("from");
				if (from != null
						&& !from.startsWith(StringUtils
								.escapeUserHost(getXmppBinder()
										.getXmppConnection().getUser()))) {
					getHandler().sendEmptyMessageDelayed(MSG_RECV, 200);
				}
			}
		}
	}
}

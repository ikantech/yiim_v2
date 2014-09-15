package com.ikantech.yiim.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;

public class FileUpload {
	private static final String TAG = FileUpload.class.getSimpleName();

	private static NativeFileUploadListener mFileUploadListener = null;

	static {
		mFileUploadListener = new NativeFileUploadListener();
	}

	private FileUpload() {

	}

	public interface FileUploadListener {
		// 传输完成
		public void transDone(Context context, String toUser, final Uri uri,
				MsgType msgType, String path, final Status status);

		public void transProgress(Context context, final Uri uri,
				String filePath, final double progress);
	}

	/**
	 * 发送文件
	 * 
	 * @param connection
	 * @param user
	 * @param toUserName
	 * @param file
	 */
	public static void sendFile(final Context context,
			final Connection connection, final String toUser, final Uri uri,
			final String filePath, final MsgType msgType) {
		new Thread() {
			public void run() {
				XMPPConnection.DEBUG_ENABLED = true;
				// AccountManager accountManager;
				try {
					// accountManager = connection.getAccountManager();
					Presence pre = connection.getRoster().getPresence(toUser);
					if (pre.getType() != Presence.Type.unavailable) {
						if (connection.isConnected()) {
							Log.d(TAG, "connection con");
						}
						// 创建文件传输管理器
//						ServiceDiscoveryManager sdm = ServiceDiscoveryManager
//								.getInstanceFor(connection);
//						if (sdm == null)
//							sdm = new ServiceDiscoveryManager(connection);
						
						FileTransferManager manager = new FileTransferManager(
								connection);
						// 创建输出的文件传输
						OutgoingFileTransfer transfer = manager
								.createOutgoingFileTransfer(pre.getFrom());
						// 发送文件
						transfer.sendFile(new File(filePath),
								msgType.toString());
						while (!transfer.isDone()) {
							if (transfer.getStatus() == FileTransfer.Status.in_progress) {
								// 可以调用transfer.getProgress();获得传输的进度　
								// Log.d(TAG,
								// "send status:" + transfer.getStatus());
								// Log.d(TAG,
								// "send progress:"
								// + transfer.getProgress());
								if (mFileUploadListener != null) {
									mFileUploadListener.transProgress(context,
											uri, filePath,
											transfer.getProgress());
								}
							}
						}
						// YiLog.getInstance().i("send file error: %s",
						// transfer.);
						Log.d(TAG, "send status 1 " + transfer.getStatus());
						if (transfer.isDone()) {
							if (mFileUploadListener != null) {
								mFileUploadListener.transDone(context, toUser,
										uri, msgType, filePath,
										transfer.getStatus());
							}
						}
					}
				} catch (Exception e) {
					Log.d(TAG, "send exception");
					if (mFileUploadListener != null) {
						mFileUploadListener.transDone(context, toUser, uri,
								msgType, filePath, Status.error);
					}
				}
			}
		}.start();
	}

	private static class NativeFileUploadListener implements FileUploadListener {
		private Map<Uri, Integer> mProgresses;

		public NativeFileUploadListener() {
			mProgresses = new HashMap<Uri, Integer>();
		}

		@Override
		public void transDone(Context context, String toUser, final Uri uri,
				MsgType msgType, String path, final Status status) {
			// TODO Auto-generated method stub
			// 发送文件完成，将文件路径按信息保存
			String imageTip = "[发送信息]";

			// ContentValues values = new ContentValues();
			// values.put(MsgManager.MsgColumns.SENDER, mUser.getUserid() + "@"
			// + XmppConnectionUtils.getXmppHost());
			// values.put(MsgManager.MsgColumns.RECEIVER, mUserTo);
			Cursor msgCursor = null;
			try {
				msgCursor = context.getContentResolver().query(uri,
						new String[] { MsgColumns.CONTENT }, null, null, null);
				if (msgCursor != null && msgCursor.getCount() == 1) {
					msgCursor.moveToFirst();
					YiIMMessage imMessage = YiIMMessage.fromString(msgCursor
							.getString(0));
					imMessage.putParam("status", status.toString());
					ContentValues values = new ContentValues();
					values.put(MsgColumns.CONTENT, imMessage.toString());
					context.getContentResolver()
							.update(uri, values, null, null);
				}
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (msgCursor != null) {
					msgCursor.close();
					msgCursor = null;
				}
			}

			mProgresses.remove(uri);

			if (!status.equals(Status.complete)) {
				return;
			}

			if (msgType == MsgType.IMAGE) {
				imageTip = "[发送图片]";
			} else if (msgType == MsgType.VIDEO) {
				imageTip = "[发送视频]";
			}

			Cursor cursor = context.getContentResolver().query(
					ConversationColumns.CONTENT_URI,
					new String[] { ConversationColumns._ID,
							ConversationColumns.DEALT },
					ConversationColumns.USER + " like '"
							+ UserInfo.getUserInfo(context).getUserName()
							+ "%' AND " + ConversationColumns.MSG + " like '"
							+ toUser.replaceAll("/.+$", "") + "%' and "
							+ ConversationColumns.MSG_TYPE + "=?",
					new String[] { ConversationType.CHAT_RECORD.toString() },
					null);

			Long now = Long.valueOf(System.currentTimeMillis());
			if (cursor != null) {
				try {
					if (cursor.getCount() == 1) {
						cursor.moveToFirst();
						ContentValues values = new ContentValues();
						values.put(ConversationColumns.DEALT, 0);
						values.put(ConversationColumns.MSG_DATE, now);
						values.put(ConversationColumns.MODIFIED_DATE, now);
						values.put(ConversationColumns.SUB_MSG, imageTip);
						context.getContentResolver().update(
								ContentUris.withAppendedId(
										ConversationColumns.CONTENT_URI,
										cursor.getLong(0)), values, null, null);
					} else {
						insertNewChatRecordConversation(context, UserInfo
								.getUserInfo(context).getUser(), toUser,
								imageTip);
					}
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					cursor.close();
					cursor = null;
				}
			} else {
				insertNewChatRecordConversation(context,
						UserInfo.getUserInfo(context).getUser(), toUser,
						imageTip);
			}
		}

		@Override
		public void transProgress(Context context, final Uri uri,
				String filePath, final double progress) {
			// TODO Auto-generated method stub
			Integer lastProgress = mProgresses.get(uri);
			if (lastProgress == null) {
				lastProgress = 0;
			}

			final int pp = (int) (progress * 100);
			if (Math.abs(pp - lastProgress) > 5) {
				mProgresses.put(uri, Integer.valueOf(pp));

				Cursor msgCursor = null;
				try {
					msgCursor = context.getContentResolver().query(uri,
							new String[] { MsgColumns.CONTENT }, null, null,
							null);
					if (msgCursor != null && msgCursor.getCount() == 1) {
						msgCursor.moveToFirst();
						YiIMMessage imMessage = YiIMMessage
								.fromString(msgCursor.getString(0));
						imMessage.putParam("progress", String.valueOf(pp));
						ContentValues values = new ContentValues();
						values.put(MsgColumns.CONTENT, imMessage.toString());
						context.getContentResolver().update(uri, values, null,
								null);
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
	}

	private static void insertNewChatRecordConversation(Context context,
			String receiver, String user, String subMsg) {
		ContentValues values = new ContentValues();
		values.put(ConversationColumns.USER, receiver);
		values.put(ConversationColumns.MSG,
				StringUtils.escapeUserResource(user));
		values.put(ConversationColumns.SUB_MSG, subMsg);
		values.put(ConversationColumns.MSG_TYPE,
				ConversationType.CHAT_RECORD.toString());
		values.put(ConversationColumns.DEALT, 0);
		context.getContentResolver().insert(ConversationColumns.CONTENT_URI,
				values);
	}
}

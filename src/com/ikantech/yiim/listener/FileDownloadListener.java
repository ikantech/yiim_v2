package com.ikantech.yiim.listener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.ikantech.support.util.YiFileUtils;
import com.ikantech.support.util.YiLog;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.MsgManager;
import com.ikantech.yiim.util.MediaFile;
import com.ikantech.yiim.util.YiIMUtils;

public class FileDownloadListener {
	private static final String TAG = FileDownloadListener.class
			.getSimpleName();

	private static String recieveFilePath = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/"; // 接收的文件的路径

	private final Connection mConnection;
	private final Context mContext;

	public static String getRecieveFilePath() {
		return recieveFilePath;
	}

	public static void setRecieveFilePath(String recieveFilePath) {
		FileDownloadListener.recieveFilePath = recieveFilePath;
	}

	public FileDownloadListener(Context context, Connection connection) {
		this.mConnection = connection;
		this.mContext = context;
		recieveFilePath = YiFileUtils.getStorePath() + "yiim/"
				+ UserInfo.getUserInfo(context).getUserName() + "/file_recv/";

		// ServiceDiscoveryManager sdm = ServiceDiscoveryManager
		// .getInstanceFor(connection);
		// if (sdm == null)
		// sdm = new ServiceDiscoveryManager(connection);
		// sdm.addFeature("http://jabber.org/protocol/disco#info");
		// sdm.addFeature("jabber:iq:privacy");
		FileTransferManager transfer = new FileTransferManager(connection);
		FileTransferNegotiator.setServiceEnabled(connection, true);
		transfer.addFileTransferListener(new RecFileTransferListener());
	}

	public class RecFileTransferListener implements FileTransferListener {

		@Override
		public void fileTransferRequest(final FileTransferRequest request) {
			// TODO Auto-generated method stub
			Log.d(TAG, "接收文件开始.....");
			final IncomingFileTransfer inTransfer = request.accept();
			final String fileName = request.getFileName();
			long length = request.getFileSize();
			Log.d(TAG, "文件大小:" + length + "  " + request.getRequestor());
			final String mimeType = request.getMimeType();
			Log.d(TAG, "文件类型：" + request.getMimeType());

			File file = new File(recieveFilePath);
			if (!file.exists()) {
				file.mkdirs();
			}

			new Thread() {
				public void run() {
					try {
						Log.d(TAG, "接受文件: " + fileName + "到" + recieveFilePath
								+ fileName);
						File file = new File(recieveFilePath + fileName);

						inTransfer.recieveFile(file);

						while (!inTransfer.isDone()) {
							// Log.d(TAG, "aaaa " + inTransfer.getProgress());
						}

						YiLog.getInstance().i(
								"file transfer status "
										+ inTransfer.getStatus());
						if (inTransfer.getStatus().equals(
								FileTransfer.Status.error)
								&& inTransfer.getException() != null) {
							YiLog.getInstance().e(inTransfer.getException(),
									"transfer file failed.");
						}

						String sender = request.getRequestor();
						String receiver = mConnection.getUser();

						ContentValues values = new ContentValues();
						values.put(MsgManager.MsgColumns.SENDER, sender);
						values.put(MsgManager.MsgColumns.RECEIVER, receiver);

						YiIMMessage imMessage = new YiIMMessage();

						if (MediaFile.isImageFileTypeFromMime(mimeType)) {
							if (inTransfer.getStatus().equals(
									FileTransfer.Status.complete)) {
								imMessage.setType(MsgType.IMAGE);
								imMessage.setBody(file.getAbsolutePath());
								imMessage.addParam("status",
										Status.complete.toString());
							} else {
								imMessage.setBody("接收图片失败");

								// 不提醒用户，默认行为，因为接收图片的过程，用户并不知道，那失败了，也不需要用户知道。
								return;
							}
						} else if (MediaFile.isVideoFileTypeFromMime(mimeType)) {
							if (inTransfer.getStatus().equals(
									FileTransfer.Status.complete)) {
								imMessage.setType(MsgType.VIDEO);
								imMessage.setBody(file.getAbsolutePath());
								imMessage.addParam("status",
										Status.complete.toString());
							} else {
								imMessage.setBody("接收视频失败");

								// 不提醒用户，默认行为，因为接收图片的过程，用户并不知道，那失败了，也不需要用户知道。
								return;
							}
						}

						values.put(MsgManager.MsgColumns.CONTENT,
								imMessage.toString());

						Long now = Long.valueOf(System.currentTimeMillis());
						String time = String.valueOf(now);
						YiIMUtils.insertMsg(mContext, values, imMessage,
								sender, sender, time);

						Map<String, String> params = new HashMap<String, String>();
						params.put("from", sender);
						YiIMUtils.broadcast(mContext,
								Const.NOTIFY_MSG_RECEIVED_OR_SENT, params);
						Log.d(TAG, "接收文件结束.....");
					} catch (Exception e2) {
						e2.printStackTrace();
					} finally {

					}
				}
			}.start();
		}
	}
}

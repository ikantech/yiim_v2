package com.ikantech.yiim.common;

import org.jivesoftware.smack.Connection;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.support.util.YiImageUtil;
import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.ui.MainActivity;
import com.ikantech.yiim.util.StringUtils;

public class NotificationManager {
	private static final int NOTIFICATION_ID = 0x01;

	private Context mContext;
	private XmppBinder mXmppBinder;
	private android.app.NotificationManager mNotificationManager;

	private XmppVcard mOwnVcard;
	private XmppVcard mOtherVcard;

	private String mOwnUser;
	private String mOtherUser;

	private String mOwnDisplayName;
	private String mOtherDisplayName;

	public NotificationManager(Context context, XmppBinder xmppBinder,
			android.app.NotificationManager notificationManager) {
		mNotificationManager = notificationManager;
		mContext = context;
		mXmppBinder = xmppBinder;
		mOwnVcard = null;
		mOtherVcard = null;
		mOtherUser = null;
		mOwnDisplayName = null;
		mOtherDisplayName = null;
	}

	public void remove() {
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

	public void update() {
		Cursor cursor = null;
		try {
			mOwnUser = UserInfo.getUserInfo(mContext).getUser();

			cursor = mContext.getContentResolver().query(
					ConversationColumns.CONTENT_URI,
					new String[] { ConversationColumns._ID,
							ConversationColumns.MSG,
							ConversationColumns.SUB_MSG,
							ConversationColumns.DEALT,
							ConversationColumns.MSG_DATE },
					ConversationColumns.USER + " like '"
							+ UserInfo.getUserInfo(mContext).getUser()
							+ "%' and " + ConversationColumns.DEALT
							+ " > 0 and " + ConversationColumns.MSG_TYPE
							+ " == " + ConversationType.CHAT_RECORD.getCode(),
					null, null);

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				String user = cursor.getString(
						cursor.getColumnIndex(ConversationColumns.MSG))
						.replaceAll("/.+$", "");

				updateNotification(user, cursor.getString(cursor
						.getColumnIndex(ConversationColumns.SUB_MSG)),
						cursor.getLong(cursor
								.getColumnIndex(ConversationColumns.MSG_DATE)));
			} else {
				String string = mContext.getString(R.string.str_online);
				Connection rawConnection = XmppConnectionUtils.getInstance()
						.getRawConnection();
				if (rawConnection == null || !rawConnection.isConnected()
						|| !rawConnection.isAuthenticated()
						|| mXmppBinder.isConnectionClosed()) {
					string = mContext.getString(R.string.str_unavailable);
				}
				updateNotification(mOwnUser, string, System.currentTimeMillis());
			}
		} catch (Exception e) {
			YiLog.getInstance().e(e, "update notification failed");
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	@SuppressLint("NewApi")
	private void updateNotification(String user, final String subMsg, long time) {
		final Notification.Builder builder = new Notification.Builder(mContext);
		builder.setSmallIcon(R.drawable.notify_icon);

		// 去掉时间
//		builder.setWhen(0);

		String esUser = StringUtils.escapeUserResource(user);
		if (esUser.equals(mOtherUser)) {
			if (mOtherVcard == null) {
				loadVcard(user);
			}
		} else if (esUser.equals(mOwnUser)) {
			if (mOwnVcard == null) {
				loadVcard(mOwnUser);
			}
		} else {
			mOtherUser = esUser;
			loadVcard(user);
		}

		builder.setContentText(subMsg);

		if (mOwnUser.equals(StringUtils.escapeUserResource(user))) {
			builder.setTicker(mOwnDisplayName);
			builder.setContentTitle(mOwnDisplayName);
			YiAsyncImageLoader.loadBitmapFromStore(mOwnUser,
					new YiImageLoaderListener() {

						@Override
						public void onImageLoaded(String url, Bitmap bitmap) {
							// TODO Auto-generated method stub
							notifi(builder, bitmap);
						}
					});
		} else {
			builder.setTicker(mOtherDisplayName);
			builder.setContentTitle(mOtherDisplayName);
			YiAsyncImageLoader.loadBitmapFromStore(mOtherUser,
					new YiImageLoaderListener() {
						@Override
						public void onImageLoaded(String url, Bitmap bitmap) {
							// TODO Auto-generated method stub
							notifi(builder, bitmap);
						}
					});
		}

		notifi(builder.getNotification());
	}

	private void notifi(Notification.Builder builder, Bitmap bitmap) {
		if (bitmap == null) {
			bitmap = BitmapFactory.decodeResource(mContext.getResources(),
					R.drawable.ic_launcher);

			builder.setLargeIcon(bitmap);
			notifi(builder.getNotification());
		} else {

			Resources res = mContext.getResources();
			int height = (int) res
					.getDimension(android.R.dimen.notification_large_icon_height) - 32;
			int width = (int) res
					.getDimension(android.R.dimen.notification_large_icon_width) - 32;

			Bitmap ret = YiImageUtil.getRoundedCornerBitmap(bitmap, 6, width,
					height);

			builder.setLargeIcon(ret);
			notifi(builder.getNotification());
		}
	}

	private void notifi(Notification notification) {
		Intent notificationIntent = new Intent(mContext, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				notificationIntent, 0);
		notification.contentIntent = contentIntent;

		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	private void loadVcard(String user) {
		try {
			final XmppVcard vCard = new XmppVcard(
					mXmppBinder.getServiceContext());

			if (mOwnUser.equals(StringUtils.escapeUserResource(user))) {
				vCard.load(mXmppBinder.getXmppConnection());
			} else {
				vCard.load(mXmppBinder.getXmppConnection(), user);
			}

			String displayName = vCard.getDisplayName();

			if (StringUtils.escapeUserResource(user).equals(mOwnUser)) {
				mOwnVcard = vCard;
				mOwnDisplayName = displayName;
			} else {
				mOtherVcard = vCard;
				mOtherDisplayName = displayName;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

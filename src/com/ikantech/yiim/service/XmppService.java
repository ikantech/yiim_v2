package com.ikantech.yiim.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;

import com.chyitech.yiim.jingle.AVCallManager;
import com.chyitech.yiim.jingle.IYiIMJingleListener;
import com.ikantech.support.proxy.YiHandlerProxy;
import com.ikantech.support.proxy.YiHandlerProxy.YiHandlerProxiable;
import com.ikantech.support.service.YiLocalService;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiPrefsKeeper;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.common.YiIMConfig;
import com.ikantech.yiim.common.XmppResult.Status;
import com.ikantech.yiim.listener.FileDownloadListener;
import com.ikantech.yiim.listener.MsgListener;
import com.ikantech.yiim.listener.MucInviteListener;
import com.ikantech.yiim.listener.PresenceListener;
import com.ikantech.yiim.listener.XmppListener;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.runnable.XmppLoadRosterRunnable;
import com.ikantech.yiim.runnable.XmppLoginRunnable;
import com.ikantech.yiim.runnable.XmppRegisterRunnable;
import com.ikantech.yiim.runnable.XmppStartRunnable;
import com.ikantech.yiim.runnable.XmppStopRunnable;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class XmppService extends YiLocalService implements XmppListener,
		YiHandlerProxiable {
	private static final int ONE_SECOND = 1000;

	private static final int MSG_TIP = 0x01;
	private static final int MSG_NO_NETWORK = 0x02;
	private static final int MSG_NETWORK_CONNECTED = 0x03;
	private static final int MSG_INIT_AFTER_LOGIN = 0x04;
	private static final int MSG_REINIT_CHAT_ROOMS = 0x05;

	public static final String MSG_SOUND = "msg_sound";

	private Connection mXmppConnection;
	private XmppBinder mXmppBinder;
	private FileDownloadListener mFileDownloadListener;
	private YiHandlerProxy mHandlerProxy;

	private MsgReceivedBroadcast mMsgReceivedBroadcast;

	private Vibrator mVibrator;
	private SoundPool mSoundPool;
	private Map<String, Integer> mSoundIds;

	private com.ikantech.yiim.common.NotificationManager mNotificationManager;
	private AVCallManager mAVCallManager;
	private NativeNetworkBroadcastReceiver mNetworkBroadcastReceiver;

	private boolean mIsConnectionClosed = true;
	private boolean mAutoLogined = false;
	private boolean mICEStarted = false;

	// MUC
	private Map<String, MultiUserChat> mMultiUserChats = null;
	private Map<String, Chat> mChats = null;

	private List<String> mNeedReInitMultiChatRooms = null;

	private Object mLocker = null;

	// 当前活跃聊天窗口
	private String mActiveChatJid = null;
	// 用于判断当前是否处于锁屏状态
	private KeyguardManager mKeyguardManager;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mXmppBinder;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mHandlerProxy = new YiHandlerProxy(this, this);
		mXmppConnection = null;
		mXmppBinder = new XmppBinder(this);

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager = new com.ikantech.yiim.common.NotificationManager(
				this, mXmppBinder, notificationManager);
		mKeyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

		mAVCallManager = new AVCallManager(this);

		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		mSoundIds = new HashMap<String, Integer>();
		mSoundIds.put(MSG_SOUND, mSoundPool.load(this, R.raw.office, 1));

		mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

		mLocker = new Object();
		mMultiUserChats = new HashMap<String, MultiUserChat>();
		mChats = new HashMap<String, Chat>();
		mNeedReInitMultiChatRooms = new ArrayList<String>();

		mMsgReceivedBroadcast = new MsgReceivedBroadcast();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Const.NOTIFY_MSG_RECEIVED_OR_SENT);
		registerReceiver(mMsgReceivedBroadcast, intentFilter);

		mNetworkBroadcastReceiver = new NativeNetworkBroadcastReceiver();
		IntentFilter netIntentFilter = new IntentFilter();
		netIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mNetworkBroadcastReceiver, netIntentFilter);

		Connection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					@Override
					public void connectionCreated(Connection connection) {
						// TODO Auto-generated method stub
						initService(connection);
					}
				});

		getHandler().removeMessages(MSG_NETWORK_CONNECTED);
		getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED, 1300);
	}

	private synchronized void autoLogin() {
		UserInfo userInfo = UserInfo.getUserInfo(this);
		if (userInfo != null && userInfo.isAutoLogin()
				&& userInfo.isRememberPasswd()
				&& !YiUtils.isStringInvalid(userInfo.getUserName())
				&& !YiUtils.isStringInvalid(userInfo.getPasswd())) {
			mXmppBinder.login(userInfo.getUserName(), userInfo.getPasswd(),
					this);
		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mXmppBinder.removeNotification();
		unregisterReceiver(mMsgReceivedBroadcast);
		unregisterReceiver(mNetworkBroadcastReceiver);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	private synchronized void initService(final Connection connection) {
		YiLog.getInstance().i("initService");
		try {
			mXmppConnection = connection;
			mIsConnectionClosed = false;

			ServiceDiscoveryManager sdm = ServiceDiscoveryManager
					.getInstanceFor(connection);
			if (sdm == null) {
				sdm = new ServiceDiscoveryManager(connection);
				sdm.addFeature("http://jabber.org/protocol/disco#info");
				sdm.addFeature("jabber:iq:privacy");
			}

			// 添加PresenceListener
			connection.addPacketListener(
					new PresenceListener(this, mXmppBinder),
					PresenceListener.PACKET_FILTER);

			// 添加 MsgListener
			MsgListener msgListener = new MsgListener(this, mXmppBinder);
			connection
					.addPacketListener(msgListener, MsgListener.PACKET_FILTER);
			connection.addPacketSendingListener(msgListener,
					MsgListener.PACKET_FILTER);
			// 添加连接监听
			connection.addConnectionListener(new ReconnectionListener());

			MultiUserChat.addInvitationListener(connection,
					new MucInviteListener(this, mXmppBinder));

		} catch (Exception e) {
			YiLog.getInstance().e(e, "initService failed.");
		}

		// restartICE();
	}

	private synchronized void initMultiUserChat(boolean callAtAfterLoginMethod) {
		String owner = UserInfo.getUserInfo(this).getUser();
		Cursor cursor = null;
		try {
			cursor = getContentResolver()
					.query(MultiChatRoomColumns.CONTENT_URI,
							new String[] { MultiChatRoomColumns._ID,
									MultiChatRoomColumns.ROOM_JID },
							MultiChatRoomColumns.OWNER + "='" + owner + "'",
							null, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					// if (joinMultUserChat(cursor.getString(1)) == -3) {
					// // if join multi user chat failed, room doesn't exist,
					// // delete.
					// }
					initMultiUserChat(cursor.getString(1),
							callAtAfterLoginMethod);
				} while (cursor.moveToNext());
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

	private synchronized MultiUserChat initMultiUserChat(String roomJid,
			boolean callAtAfterLoginMethod) {
		MultiUserChat chat = null;
		try {
			// 如果房间存在，则加入
			if (YiIMUtils.isRoomExist(roomJid)) {
				if (joinMultUserChat(roomJid) == 0) {
					synchronized (mLocker) {
						chat = mMultiUserChats.get(roomJid);
					}
				} else {
					synchronized (mLocker) {
						if (callAtAfterLoginMethod
								&& !mNeedReInitMultiChatRooms.contains(roomJid)) {
							mNeedReInitMultiChatRooms.add(roomJid);
							getHandler().removeMessages(MSG_REINIT_CHAT_ROOMS);
							getHandler().sendEmptyMessageDelayed(
									MSG_REINIT_CHAT_ROOMS, ONE_SECOND);
						}
					}
				}
			} else {// 如果房间不存在，则创建
				try {
					String desc = YiIMUtils.getMultUserChatDesc(this, roomJid);
					if (desc != null) {
						chat = YiIMUtils.createMultiUserChat(this, UserInfo
								.getUserInfo(this).getUser(), StringUtils
								.escapeUserHost(roomJid), desc, null);

						if (chat != null) {
							synchronized (mLocker) {
								mMultiUserChats.put(roomJid, chat);
								YiLog.getInstance().i("create room success");
							}
						}
					}
				} catch (Exception e) {
					YiLog.getInstance().e(e, "create room failed");
					synchronized (mLocker) {
						if (callAtAfterLoginMethod
								&& !mNeedReInitMultiChatRooms.contains(roomJid)) {
							mNeedReInitMultiChatRooms.add(roomJid);
							getHandler().removeMessages(MSG_REINIT_CHAT_ROOMS);
							getHandler().sendEmptyMessageDelayed(
									MSG_REINIT_CHAT_ROOMS, ONE_SECOND);
						}
					}
				}
			}
		} catch (Exception e) {
			YiLog.getInstance().e(e, "init room failed");
			synchronized (mLocker) {
				if (callAtAfterLoginMethod
						&& !mNeedReInitMultiChatRooms.contains(roomJid)) {
					mNeedReInitMultiChatRooms.add(roomJid);
					getHandler().removeMessages(MSG_REINIT_CHAT_ROOMS);
					getHandler().sendEmptyMessageDelayed(MSG_REINIT_CHAT_ROOMS,
							ONE_SECOND);
				}
			}
		}
		return chat;
	}

	private synchronized void reInitMultiUserChat() {
		synchronized (mLocker) {
			Iterator<String> iterator = mNeedReInitMultiChatRooms.iterator();
			while (iterator.hasNext()) {
				String roomJid = iterator.next();

				MultiUserChat chat = mMultiUserChats.get(roomJid);
				if (chat == null) {
					// 如果重新初始化成功，则移除
					chat = initMultiUserChat(roomJid, true);
					if (chat != null
							&& YiIMUtils.isMultiChatJoined(this, roomJid)) {
						iterator.remove();
					}
				}
			}
		}
	}

	private synchronized int joinMultUserChat(String jid) {
		try {
			String owner = UserInfo.getUserInfo(this).getUserName();

			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getRawConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				return -1;
			}
			RoomInfo roomInfo = null;
			try {
				roomInfo = MultiUserChat.getRoomInfo(connection, jid);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (roomInfo == null) {
				YiLog.getInstance().i("join room failed:room not found");
				return -3;
			}

			MultiUserChat chat = YiIMUtils.joinMultiUserChat(this, owner, jid,
					null);
			synchronized (mLocker) {
				mMultiUserChats.put(jid, chat);
			}
			YiLog.getInstance().i("join room success");
			return 0;
		} catch (Exception e) {
			YiLog.getInstance().e(e, "join room failed");
			return -1;
		}
	}

	public synchronized void restartICE() {
		if (mICEStarted)
			return;
		if (mXmppConnection != null && mXmppConnection.isConnected()) {
			mICEStarted = true;
			mXmppBinder.execute(new ReResolverRunnable());
		}
	}

	public synchronized void checkIfNeedAutoLogin() {
		if (mAutoLogined)
			return;

		YiLog.getInstance().i("msg auto login");
		YiIMConfig config = YiIMConfig.getInstance();
		YiPrefsKeeper.read(this, config);
		if (!config.isExited()) {
			YiLog.getInstance().i("msg auto login1");
			mAutoLogined = true;
			if (mXmppConnection == null || !mXmppConnection.isConnected()) {
				mXmppBinder.startXmppService(this);
			} else if (!mXmppConnection.isAuthenticated()) {
				autoLogin();
			}
		}
	}

	private class ReconnectionListener implements ConnectionListener {
		@Override
		public void connectionClosed() {
			// TODO Auto-generated method stub
			YiLog.getInstance().i("connectionClosed");
			mIsConnectionClosed = true;

			getHandler().removeMessages(MSG_NO_NETWORK);
			getHandler().sendEmptyMessageDelayed(MSG_NO_NETWORK, 300);

			// 如果不是用户主动退出
			YiIMConfig config = YiIMConfig.getInstance();
			YiPrefsKeeper.read(XmppService.this, config);
			if (!config.isExited()) {
				mXmppBinder.updateNotification();
				getHandler().removeMessages(MSG_NETWORK_CONNECTED);
				getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
						1300);
			}
		}

		@Override
		public void connectionClosedOnError(Exception e) {
			// TODO Auto-generated method stub
			YiLog.getInstance().i("connectionClosedOnError %s", e.getMessage());
			if (e.getMessage().contains("conflict")) {
				if (mXmppConnection.isConnected()) {
					XmppConnectionUtils.getInstance().closeConnection();
					mIsConnectionClosed = true;
				}
			}

			getHandler().removeMessages(MSG_NO_NETWORK);
			getHandler().sendEmptyMessageDelayed(MSG_NO_NETWORK, 300);

			// 如果不是用户主动退出
			YiIMConfig config = YiIMConfig.getInstance();
			YiPrefsKeeper.read(XmppService.this, config);
			if (!config.isExited()) {
				mXmppBinder.updateNotification();
				// 启动自动连接
				getHandler().removeMessages(MSG_NETWORK_CONNECTED);
				getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
						1300);
			}
		}

		@Override
		public void reconnectingIn(int seconds) {
			// TODO Auto-generated method stub
			YiLog.getInstance().i("reconnectingIn %d", seconds);
			mIsConnectionClosed = true;
		}

		@Override
		public void reconnectionSuccessful() {
			// TODO Auto-generated method stub
			YiLog.getInstance().i("reconnectionSuccessful");
			mIsConnectionClosed = false;
			mXmppBinder.updateNotification();
		}

		@Override
		public void reconnectionFailed(Exception e) {
			// TODO Auto-generated method stub
			mIsConnectionClosed = true;
			YiLog.getInstance().i(e, "reconnectionFailed");
		}
	}

	public static class XmppBinder extends YiLocalServiceBinder {
		private WeakReference<XmppService> mService;

		public XmppBinder(XmppService arg0) {
			super(arg0);
			mService = new WeakReference<XmppService>(arg0);
		}

		public Context getServiceContext() {
			return mService.get();
		}

		public boolean isXmppServiceStarted() {
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getRawConnection();
			return connection != null && connection.isConnected();
		}

		public boolean isConnectionClosed() {
			return mService.get().mIsConnectionClosed;
		}

		public boolean isAuthenticated() {
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getRawConnection();
			return isXmppServiceStarted() && connection.isAuthenticated();
		}

		public void startXmppService(XmppListener listener) {
			execute(new XmppStartRunnable(listener));
		}

		public void checkIfNeedAutoLogin() {
			mService.get().getHandler().removeMessages(MSG_NETWORK_CONNECTED);
			mService.get().getHandler()
					.sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED, 1300);
		}

		public void stopXmppService(XmppListener listener) {
			// 清理
			mService.get().resetService();
			execute(new XmppStopRunnable(listener));
		}

		public void restartXmppService(XmppListener listener) {
			stopXmppService(listener);
			startXmppService(listener);
		}

		public Connection getXmppConnection() {
			return mService.get().mXmppConnection;
		}

		public void updateNotification() {
			execute(new UpdateNotificationRunnable(mService.get()));
		}

		public void removeNotification() {
			mService.get().mNotificationManager.remove();
		}

		public void login(String user, String passwd, XmppListener listener) {
			XmppLoginRunnable runnable = new XmppLoginRunnable(listener);
			runnable.setUserName(user);
			runnable.setPasswd(passwd);
			execute(runnable);
		}

		public void register(String user, String passwd, XmppListener listener) {
			XmppRegisterRunnable runnable = new XmppRegisterRunnable(listener);
			runnable.setUserName(user);
			runnable.setPasswd(passwd);
			execute(runnable);
		}

		public void checkIfFirstLogin() {
			UserInfo userInfo = UserInfo.getUserInfo(mService.get());
			if (userInfo.isFirstLogin()) {
				execute(new XmppLoadRosterRunnable(mService.get(), null));
				userInfo.setFirstLogin(false);
				YiPrefsKeeper.write(mService.get(), userInfo);
			}
		}

		public void initAfterLogin() {
			mService.get().getHandler().removeMessages(MSG_INIT_AFTER_LOGIN);
			mService.get().getHandler()
					.sendEmptyMessageDelayed(MSG_INIT_AFTER_LOGIN, ONE_SECOND);
		}

		public void removeJingleListener(IYiIMJingleListener listener)
				throws RemoteException {
			mService.get().mAVCallManager.removeJingleListener(listener);
		}

		public void addJingleListener(IYiIMJingleListener listen)
				throws RemoteException {
			mService.get().mAVCallManager.addJingleListener(listen);
		}

		public void setSpeakerMode(boolean enable) {
			mService.get().mAVCallManager.setSpeakerMode(enable);
		}

		public void closeCall() throws RemoteException {
			mService.get().mAVCallManager.closeCall();
		}

		public void acceptCall() throws RemoteException {
			mService.get().mAVCallManager.acceptCall();
		}

		public void call(final String receiver) throws RemoteException {
			mService.get().mAVCallManager.call(receiver);
		}

		public boolean isAVCallOK() {
			return mService.get().mAVCallManager.isInitialized();
		}

		public void setActiveChat(String activeJid) {
			synchronized (mService.get().mLocker) {
				mService.get().mActiveChatJid = activeJid;
			}
		}

		public int sendMessage(String to, String msg) {
			if (YiUtils.isStringInvalid(to) || YiUtils.isStringInvalid(msg)) {
				YiLog.getInstance().i("sendMessage param illegal");
				return -1;
			}
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getRawConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				YiLog.getInstance().i("sendMessage no connect");
				return -2;
			}

			YiLog.getInstance().i("sendMessage to %s, msg %s", to, msg);
			if (to.contains("conference")) {
				YiLog.getInstance().i("sendMessage conference");
				MultiUserChat chat = null;
				synchronized (mService.get().mLocker) {
					chat = mService.get().mMultiUserChats.get(to);
				}
				if (chat == null) {
					YiLog.getInstance().i("sendMessage conference1");
					chat = mService.get().initMultiUserChat(to, false);
				}

				if (chat != null) {
					try {
						YiLog.getInstance().i("sendMessage conference2");
						chat.sendMessage(msg);
						return 0;
					} catch (Exception e) {
						chat.leave();
						synchronized (mService.get().mLocker) {
							mService.get().mChats.remove(to);
						}
						YiLog.getInstance().e(e, "send msg failed");
					}
				}
			} else {
				String jid = StringUtils.escapeUserResource(to);
				Chat chat = null;
				synchronized (mService.get().mLocker) {
					chat = mService.get().mChats.get(jid);
				}
				if (chat == null) {
					ChatManager cm = connection.getChatManager();
					chat = cm.createChat(jid, null);
					synchronized (mService.get().mLocker) {
						mService.get().mChats.put(jid, chat);
					}
				}

				if (chat != null) {
					try {
						chat.sendMessage(msg);
						return 0;
					} catch (Exception e) {
						chat.shutdown();
						synchronized (mService.get().mLocker) {
							mService.get().mChats.remove(jid);
						}
						YiLog.getInstance().e(e, "send msg failed");
					}
				}
			}
			return -1;
		}

		public int createRoom(String user, String roomName, String roomDesc,
				String password) {
			try {
				MultiUserChat chat = YiIMUtils.createMultiUserChat(
						mService.get(), user, roomName, roomDesc, password);
				if (chat != null) {
					synchronized (mService.get().mLocker) {
						mService.get().mMultiUserChats
								.put(chat.getRoom(), chat);
					}
					return 0;
				}
				return -1;
			} catch (Exception e) {
				if ("room already exist".equals(e.getMessage())) {
					return -2;
				}
				YiLog.getInstance().e(e, "create room failed");
				return -1;
			}
		}

		public Iterator<String> getRoomMembers(String roomJid) {
			MultiUserChat chat = null;
			synchronized (mService.get().mLocker) {
				chat = mService.get().mMultiUserChats.get(roomJid);
			}
			if (chat == null) {
				chat = mService.get().initMultiUserChat(roomJid, false);
			}

			if (chat != null) {
				try {
					return chat.getOccupants();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			return null;
		}

		public int deleteRoom(long roomId, String roomJid) {
			XMPPConnection connection = XmppConnectionUtils.getInstance()
					.getRawConnection();
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				return -1;
			}
			MultiUserChat chat = null;
			synchronized (mService.get().mLocker) {
				chat = mService.get().mMultiUserChats.get(roomJid);
			}
			if (chat != null) {
				try {
					chat.leave();
					synchronized (mService.get().mLocker) {
						mService.get().mMultiUserChats.remove(roomJid);
					}
				} catch (Exception e) {
					YiLog.getInstance().e(e, "delete room failed");
				}
			}

			try {
				YiIMUtils.deleteChatRecord(mService.get(), roomJid);
				YiIMUtils.deleteConversation(mService.get(), roomJid);
				mService.get()
						.getContentResolver()
						.delete(ContentUris.withAppendedId(
								MultiChatRoomColumns.CONTENT_URI, roomId),
								null, null);
				return 0;
			} catch (Exception e) {
				return -1;
			}
		}

		public void inviteFriend(String roomJid, String user, boolean autoJoin) {
			MultiUserChat chat = null;
			synchronized (mService.get().mLocker) {
				chat = mService.get().mMultiUserChats.get(roomJid);
			}

			if (chat == null && autoJoin) {
				chat = mService.get().initMultiUserChat(roomJid, false);
			}

			if (chat != null) {
				try {
					chat.invite(user, "");
				} catch (Exception e) {
				}
			}
		}

		public int joinMultUserChat(String jid) {
			return mService.get().joinMultUserChat(jid);
		}
	}

	private class MsgReceivedBroadcast extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Const.NOTIFY_MSG_RECEIVED_OR_SENT)) {
				String from = intent.getStringExtra("from");
				YiLog.getInstance().i("tip from %s", from);
				if (from != null
						&& !from.startsWith(UserInfo.getUserInfo(context)
								.getUser())) {

					// 如果当前正处于活动聊天窗口，则不提示
					if (!YiUtils.isStringInvalid(mActiveChatJid)
							&& from.startsWith(mActiveChatJid)
							&& !mKeyguardManager
									.inKeyguardRestrictedInputMode()) {
						return;
					}
					getHandler().removeMessages(MSG_TIP);
					getHandler().sendEmptyMessageDelayed(MSG_TIP, 300);
				}
			}
		}
	}

	private class MsgTipRunnable implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			UserInfo userInfo = UserInfo.getUserInfo(XmppService.this);
			if (userInfo != null && !userInfo.isMsgNoTip()) {
				if (userInfo.isMsgTipAudio()) {
					mSoundPool.play(mSoundIds.get(MSG_SOUND), 1, 1, 0, 0, 1);
				}
				if (userInfo.isMsgTipVibrator()) {
					mVibrator.vibrate(new long[] { 0, 200, 100, 200 }, -1);
				}
			}
		}
	}

	private static class UpdateNotificationRunnable implements Runnable {
		private WeakReference<XmppService> mService;

		public UpdateNotificationRunnable(XmppService arg0) {
			mService = new WeakReference<XmppService>(arg0);
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (mService.get() != null) {
				mService.get().mNotificationManager.update();
			}
		}
	}

	private synchronized void initAfterLogin() {
		YiLog.getInstance().i("initAfterLogin");

		try {
			mAutoLogined = true;
			mXmppBinder.updateNotification();
			mXmppBinder.checkIfFirstLogin();
			mFileDownloadListener = new FileDownloadListener(this,
					mXmppConnection);

			synchronized (mLocker) {
				mChats.clear();
				mMultiUserChats.clear();
			}

			initMultiUserChat(true);

			restartICE();
		} catch (Exception e) {
			// TODO: handle exception
			YiLog.getInstance().e(e, "init failed after login");
		}
	}

	@Override
	public void onXmppResonpse(XmppResult result) {
		// TODO Auto-generated method stub
		switch (result.what) {
		case XMPP_START:
			if (result.status.equals(Status.FAILED)) {
				YiLog.getInstance().e("start xmpp service failed.");
				getHandler().removeMessages(MSG_NO_NETWORK);
				getHandler().sendEmptyMessageDelayed(MSG_NO_NETWORK, 400);

				// 5-15秒内进行重连
				int randomR = new Random().nextInt(11) + 5;

				getHandler().removeMessages(MSG_NETWORK_CONNECTED);
				getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
						randomR * ONE_SECOND);
			} else {
				if (!mXmppBinder.isAuthenticated()) {
					autoLogin();
				}
			}
			break;
		case XMPP_LOGIN:
			if (result.status.equals(Status.FAILED)) {
				YiLog.getInstance().e("login failed: %s", result.obj);

				getHandler().removeMessages(MSG_NO_NETWORK);
				getHandler().sendEmptyMessageDelayed(MSG_NO_NETWORK, 400);

				// 5-15秒内进行重连
				int randomR = new Random().nextInt(11) + 5;

				getHandler().removeMessages(MSG_NETWORK_CONNECTED);
				getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
						randomR * ONE_SECOND);
			} else {
				mXmppBinder.initAfterLogin();
			}
			break;
		default:
			break;
		}
	}

	private class ReResolverRunnable implements Runnable {
		@Override
		public void run() {
			mAVCallManager.reset();
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mAVCallManager.initWhenConntected(mXmppConnection);
				}
			}).start();
		}
	}

	public class NativeNetworkBroadcastReceiver extends BroadcastReceiver {
		State wifiState = null;
		State mobileState = null;

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent
					.getAction())) {
				// 获取手机的连接服务管理器，这里是连接管理器类
				ConnectivityManager cm = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				wifiState = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.getState();
				mobileState = cm
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
						.getState();

				if (wifiState != null && mobileState != null
						&& State.CONNECTED != wifiState
						&& State.CONNECTED == mobileState) {
					YiLog.getInstance().i("net 手机网络连接成功！");
					getHandler().removeMessages(MSG_NETWORK_CONNECTED);
					getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
							400);
				} else if (wifiState != null && mobileState != null
						&& State.CONNECTED == wifiState
						&& State.CONNECTED != mobileState) {
					YiLog.getInstance().i("net 无线网络连接成功！");
					getHandler().removeMessages(MSG_NETWORK_CONNECTED);
					getHandler().sendEmptyMessageDelayed(MSG_NETWORK_CONNECTED,
							400);
				} else if (wifiState != null && mobileState != null
						&& State.CONNECTED != wifiState
						&& State.CONNECTED != mobileState) {
					YiLog.getInstance().i("net 手机没有任何网络...");
					getHandler().removeMessages(MSG_NO_NETWORK);
					getHandler().sendEmptyMessageDelayed(MSG_NO_NETWORK, 400);
				}
			}
		}

	}

	public void resetService() {
		synchronized (mLocker) {
			mAutoLogined = false;
			mICEStarted = false;
			mXmppConnection = null;
			mChats.clear();
			mMultiUserChats.clear();
			mNeedReInitMultiChatRooms.clear();
		}
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case MSG_TIP:
			mXmppBinder.execute(new MsgTipRunnable());
			mXmppBinder.updateNotification();
			break;
		case MSG_NO_NETWORK:
			mXmppBinder.stopXmppService(null);
			break;
		case MSG_NETWORK_CONNECTED:
			checkIfNeedAutoLogin();
			// restartICE();
			// initAfterLogin();
			break;
		case MSG_INIT_AFTER_LOGIN:
			initAfterLogin();
			break;
		case MSG_REINIT_CHAT_ROOMS:
			reInitMultiUserChat();
			break;
		default:
			break;
		}
	}

	@Override
	public Handler getHandler() {
		// TODO Auto-generated method stub
		return mHandlerProxy.getHandler();
	}
}

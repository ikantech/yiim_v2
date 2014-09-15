package com.ikantech.yiim.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.chyitech.yiim.jingle.AVCallManager;
import com.ikantech.support.app.YiApplication;
import com.ikantech.support.cache.YiStoreCache;
import com.ikantech.support.common.YiCrashHandler;
import com.ikantech.support.util.YiBase64;
import com.ikantech.support.util.YiFileUtils;
import com.ikantech.support.util.YiLog;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.EmotionManager;
import com.ikantech.yiim.service.XmppService;
import com.ikantech.yiim.service.XmppService.XmppBinder;

import de.javawi.jstun.test.DiscoveryTest;

public class YiIMApplication extends YiApplication {
	public static final String IP = "115.28.150.238";
	// public static final String IP = "192.168.43.136";
	// public static final String IP = "60.190.203.43";
	public static final int PORT = 5222;

	// 服务器是否OPENIFIRE，如果是Ejabberd，请将此变量值设置为false
	public static final boolean USE_OPENIRE = false;

	private boolean mIsFirstLaunch = true;
	private boolean mIsLogin = false;
	private XmppBinder mXmppService;

	private NativeServiceConnection mServiceConnection;

	@Override
	protected void initialize() {
		super.initialize();

		YiLog.ENABLE_DEBUG = true;
		YiLog.ENABLE_ERROR = true;
		YiLog.ENABLE_INFO = true;
		YiLog.ENABLE_WARN = true;
		YiLog.ENABLE_VERBOSE = true;

		YiBase64.setChars("AB678wxCDEf34ghiFGUVWXYZabcdejIJKLPQRklmnopqMNO5rstHSTuvyz0129_-");

		// NAT类型监测超时时间 ms
		DiscoveryTest.timeoutInitValue = 400;

		// 开启ICE模式
		AVCallManager.setUseIce(true);
		// NAT穿透 ICE服务器
		AVCallManager.mICEServer = "60.190.203.32";
		// NAT穿透 ICE端口
		AVCallManager.mICEServerPort = 3478;

		// 设置Xmpp host
		XmppConnectionUtils.setXmppHost(IP);
		XmppConnectionUtils.setXmppServerName(IP);
		XmppConnectionUtils.setXmppPort(PORT);

		YiCrashHandler.setLogPath(YiFileUtils.getStorePath() + "yiim/crash/");
		YiStoreCache.IMAGE_CACHE_DIR = "yiim/";

		// 初始化表情
		EmotionManager.initializeEmoji(this);
		EmotionManager.initializeClassicalEmoji(this);

		mServiceConnection = new NativeServiceConnection();
		Intent xmppServiceIntent = new Intent(getBaseContext(),
				XmppService.class);
		startService(xmppServiceIntent);
		bindService(xmppServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onTerminate() {
		unbindService(mServiceConnection);
		super.onTerminate();
	}

	@Override
	protected boolean openCrashHandler() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isFirstLaunch() {
		return mIsFirstLaunch;
	}

	public void setFirstLaunch(boolean isFirstLaunch) {
		mIsFirstLaunch = isFirstLaunch;
	}

	public boolean isLogin() {
		return mIsLogin;
	}

	public void setLogin(boolean isLogin) {
		this.mIsLogin = isLogin;
	}

	public XmppBinder getXmppService() {
		return mXmppService;
	}

	private class NativeServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXmppService = (XmppBinder) service;
			// 建立Ｘmpp连接
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXmppService = null;
		}
	}
}

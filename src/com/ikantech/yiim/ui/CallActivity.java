package com.ikantech.yiim.ui;

import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chyitech.yiim.jingle.IYiIMJingleListener;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.ui.base.CustomTitleActivity;

public class CallActivity extends CustomTitleActivity {
	private static final String TAG = "Jingle/Call";
	private ImageView mLogo;
	private TextView mCallInfo;
	private Animation mRotateAnim;
	private NativeJingleSessionListener mJingleListener = new NativeJingleSessionListener();
	private final Handler mHandler = new Handler();
	private Button mCloseCall;
	private Button mAcceptCall;
	private Intent mIntent;
	private boolean mIsCaller;

	private String mJID;
	private String mSelectedRes;

	final static long[] vibratePattern = { 0, 1000, 1000 };

	public static final int UA_STATE_IDLE = 0;
	public static final int UA_STATE_INCOMING_CALL = 1;
	public static final int UA_STATE_OUTGOING_CALL = 2;
	public static final int UA_STATE_INCALL = 3;
	public static final int UA_STATE_HOLD = 4;

	public static int call_state = UA_STATE_IDLE;
	public static int docked = -1, headset = -1;
	public static Ringtone oRingtone;
	public static Context mContext;

	private AudioManager mAudioManager;

	public static void stopRingtone() {
		android.os.Vibrator v = (Vibrator) mContext
				.getSystemService(Context.VIBRATOR_SERVICE);
		v.cancel();
		if (oRingtone != null) {
			Ringtone ringtone = oRingtone;
			oRingtone = null;
			ringtone.stop();
		}
	}

	public static void startRingtone() {
		android.os.Vibrator v = (Vibrator) mContext
				.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(vibratePattern, 1);
		oRingtone = RingtoneManager.getRingtone(mContext,
				RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
		oRingtone.play();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_call);
		super.onCreate(savedInstanceState);
		CallActivity.mContext = this;
		mIntent = this.getIntent();

		mLogo = (ImageView) findViewById(R.id.call_avatar);
		mRotateAnim = AnimationUtils.loadAnimation(this,
				R.anim.rotate_and_scale);
		mCloseCall = (Button) findViewById(R.id.call_cancel_button);
		mCloseCall.setOnClickListener(new ClickListener());
		mIsCaller = mIntent.getBooleanExtra("isCaller", false);
		if (mIsCaller) {
			mAcceptCall = (Button) findViewById(R.id.call_accept_button);
			mAcceptCall.setVisibility(View.GONE);
			call_state = UA_STATE_OUTGOING_CALL;
		} else {
			mAcceptCall = (Button) findViewById(R.id.call_accept_button);
			mAcceptCall.setOnClickListener(new ClickListener());
			call_state = UA_STATE_INCOMING_CALL;
			CallActivity.startRingtone();
		}
		mCallInfo = (TextView) findViewById(R.id.call_info);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		try {

			getXmppBinder().addJingleListener(mJingleListener);
		} catch (Exception e) {
			// TODO: handle exception
		}

		if (mIsCaller) {
			parseUri(getIntent().getData());
			try {
				getXmppBinder().call(getJIDWithRes());
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mLogo.startAnimation(mRotateAnim);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (getXmppBinder() != null) {
			try {
				getXmppBinder().removeJingleListener(mJingleListener);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public final boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_call, menu);
		return true;
	}

	/**
	 * Callback for menu item selected.
	 * 
	 * @param item
	 *            the item selected
	 * @return true on success, false otherwise
	 */
	@Override
	public final boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.call_speaker_on:
			// normal
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
			getXmppBinder().setSpeakerMode(true);
			return true;
		case R.id.call_speaker_off:
			// in call
			mAudioManager.setMode(AudioManager.MODE_IN_CALL);
			getXmppBinder().setSpeakerMode(false);
			return true;
		case R.id.call_hold_on:
			CallActivity.call_state = UA_STATE_HOLD;
			return true;
		case R.id.call_hold_off:
			CallActivity.call_state = UA_STATE_INCALL;
			return true;
		default:
			return false;
		}
	}

	/**
	 * Click event listener on cancel button.
	 */
	private class ClickListener implements OnClickListener {

		/**
		 * Constructor.
		 */
		ClickListener() {
		}

		@Override
		public void onClick(View v) {
			try {
				if (v == mCloseCall) {
					getXmppBinder().closeCall();
					stopRingtone();
					CallActivity.call_state = UA_STATE_IDLE;
					finish();
				} else if (v == mAcceptCall) {
					getXmppBinder().acceptCall();
					stopRingtone();
					CallActivity.call_state = UA_STATE_INCALL;
					mAcceptCall.setVisibility(View.GONE);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private class NativeJingleSessionListener extends IYiIMJingleListener.Stub {

		/**
		 * Refresh the call activity.
		 */
		private class RunnableChange implements Runnable {

			private String mStr;

			/**
			 * Constructor.
			 */
			public RunnableChange(String str) {
				mStr = str;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void run() {
				mCallInfo.setText(mStr);
			}
		}

		public NativeJingleSessionListener() {
		}

		@Override
		public void sessionClosed(final String reason) {
			Log.d(TAG, "sessionClosed " + reason);
			CallActivity.stopRingtone();
			call_state = UA_STATE_IDLE;
			mHandler.post(new RunnableChange(reason));
		}

		@Override
		public void sessionDeclined(final String reason) {
			Log.d(TAG, "sessionDeclined " + reason);
			CallActivity.stopRingtone();
			call_state = UA_STATE_IDLE;
			mHandler.post(new RunnableChange(reason));
		}

		@Override
		public void sessionClosedOnError(final String error) {
			Log.d(TAG, "sessionClosedOnError " + error);
			CallActivity.stopRingtone();
			call_state = UA_STATE_IDLE;
			mHandler.post(new RunnableChange(error));
		}

		@Override
		public void sessionEstablished() {
			Log.d(TAG, "sessionEstablished ");
			call_state = UA_STATE_INCALL;
			CallActivity.stopRingtone();
			mHandler.post(new RunnableChange("established"));
		}

		@Override
		public void sessionRequested(final String fromJID) {
			Log.d(TAG, "sessionRequested " + fromJID);
			mHandler.post(new RunnableChange(fromJID));
		}
	}

	public void parseUri(Uri uri) {
		if (!"xmpp".equals(uri.getScheme()))
			throw new IllegalArgumentException();
		String enduri = uri.getEncodedSchemeSpecificPart();
		mJID = StringUtils.parseBareAddress(enduri);
		String res = StringUtils.parseResource(enduri);
		mSelectedRes = res;
	}

	public String getJIDWithRes() {
		StringBuilder build = new StringBuilder(mJID);
		if (!"".equals(mSelectedRes))
			build.append('/').append(mSelectedRes);
		return build.toString();
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

	}

	@Override
	protected void initDatas() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void installListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void uninstallListeners() {
		// TODO Auto-generated method stub

	}
}

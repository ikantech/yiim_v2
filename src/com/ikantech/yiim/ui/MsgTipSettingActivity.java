package com.ikantech.yiim.ui;

import android.os.Bundle;
import android.os.Message;

import com.ikantech.support.util.YiPrefsKeeper;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.common.XmppResult;
import com.ikantech.yiim.ui.base.CustomTitleActivity;
import com.ikantech.yiim.view.SlipSwitch;
import com.ikantech.yiim.view.SlipSwitch.OnSwitchListener;

public class MsgTipSettingActivity extends CustomTitleActivity {
	private SlipSwitch mMsgNoTipSwitch;
	private SlipSwitch mMsgTipAudioSwitch;
	private SlipSwitch mMsgTipVibratorSwitch;
	private UserInfo mUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_msg_tip_setting);
		super.onCreate(savedInstanceState);

		mUser = UserInfo.getUserInfo(this);

		mMsgNoTipSwitch = (SlipSwitch) findViewById(R.id.msg_no_tip_switch);
		mMsgNoTipSwitch.setImageResource(R.drawable.bkg_switch,
				R.drawable.bkg_switch, R.drawable.btn_slip);
		mMsgNoTipSwitch.setSwitchState(!mUser.isMsgNoTip());
		mMsgNoTipSwitch.setOnSwitchListener(new OnSwitchListener() {
			@Override
			public void onSwitched(boolean isSwitchOn) {
				// TODO Auto-generated method stub
				mUser.setMsgNoTip(!isSwitchOn);
				YiPrefsKeeper.write(MsgTipSettingActivity.this, mUser);
			}
		});

		mMsgTipAudioSwitch = (SlipSwitch) findViewById(R.id.msg_tip_audio_switch);
		mMsgTipAudioSwitch.setImageResource(R.drawable.bkg_switch,
				R.drawable.bkg_switch, R.drawable.btn_slip);
		mMsgTipAudioSwitch.setSwitchState(mUser.isMsgTipAudio());
		mMsgTipAudioSwitch.setOnSwitchListener(new OnSwitchListener() {
			@Override
			public void onSwitched(boolean isSwitchOn) {
				// TODO Auto-generated method stub
				mUser.setMsgTipAudio(isSwitchOn);
				YiPrefsKeeper.write(MsgTipSettingActivity.this, mUser);
			}
		});

		mMsgTipVibratorSwitch = (SlipSwitch) findViewById(R.id.msg_tip_vibrator_switch);
		mMsgTipVibratorSwitch.setImageResource(R.drawable.bkg_switch,
				R.drawable.bkg_switch, R.drawable.btn_slip);
		mMsgTipVibratorSwitch.setSwitchState(mUser.isMsgTipVibrator());
		mMsgTipVibratorSwitch.setOnSwitchListener(new OnSwitchListener() {
			@Override
			public void onSwitched(boolean isSwitchOn) {
				// TODO Auto-generated method stub
				mUser.setMsgTipVibrator(isSwitchOn);
				YiPrefsKeeper.write(MsgTipSettingActivity.this, mUser);
			}
		});
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

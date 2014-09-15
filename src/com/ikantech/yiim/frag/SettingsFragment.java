package com.ikantech.yiim.frag;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ikantech.support.widget.YiFragment;
import com.ikantech.yiim.R;
import com.ikantech.yiim.view.SlipSwitch;

public class SettingsFragment extends YiFragment {

	private View mRootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mRootView = inflater.inflate(R.layout.main_tab_settings, null);

		SlipSwitch m3GSipSlipSwitch = (SlipSwitch) mRootView
				.findViewById(R.id.switch_3g_sip);
		m3GSipSlipSwitch.setImageResource(R.drawable.bkg_switch,
				R.drawable.bkg_switch, R.drawable.btn_slip);

		return mRootView;
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub

	}

}

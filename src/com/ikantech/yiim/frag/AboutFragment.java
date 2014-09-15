package com.ikantech.yiim.frag;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.ikantech.support.widget.YiFragment;

public class AboutFragment extends YiFragment {
	
	private WebView mWebView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.loadUrl("file:///android_asset/about.html");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mWebView = new WebView(getActivity());
		return mWebView;
	}

	@Override
	public void processHandlerMessage(Message msg) {
		// TODO Auto-generated method stub
		
	}

}

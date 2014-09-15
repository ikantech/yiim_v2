package com.ikantech.yiim.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.ikantech.yiim.R;

public abstract class CustomTitleActivity extends XmppActivity {
	private LinearLayout mMainLayout;
	private View mContentView;

	private TextView mTitleTextView;
	private Button mTitleRightBtn;
	private Button mTitleBackBtn;
	private ImageButton mTitleLeftImgBtn;
	private ImageButton mTitleRightImgBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.setContentView(R.layout.activity_custom_title);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.layout_title_bar);

		mMainLayout = (LinearLayout) findViewById(R.id.main_layout);
		mTitleTextView = (TextView) findViewById(R.id.title_bar_title);
		mTitleTextView.setText(getTitle());
		mTitleBackBtn = (Button) findViewById(R.id.title_back_btn);
		mTitleRightBtn = (Button) findViewById(R.id.title_right_btn);
		mTitleRightImgBtn = (ImageButton) findViewById(R.id.title_right_img_btn);
		mTitleLeftImgBtn = (ImageButton) findViewById(R.id.title_left_img_btn);

		if (mContentView == null) {
			throw new NullPointerException(
					"content view must be set before call super.onCreate");
		}
		mMainLayout.addView(mContentView);
		super.onCreate(savedInstanceState);
	}

	public void setContentView(int layoutId) {
		mContentView = LayoutInflater.from(this).inflate(layoutId, null);
		if (mContentView == null) {
			return;
		}
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mContentView.setLayoutParams(lp);
	}

	protected void setTitle(String title) {
		mTitleTextView.setText(title);
	}

	public void onTitleBarLeftBtnClick(View view) {
		// TODO Auto-generated method stub
		finish();
	}

	public void onTitleBarLeftImgBtnClick(View view) {
		// TODO Auto-generated method stub

	}

	public void onTitleBarRightBtnClick(View view) {
		// TODO Auto-generated method stub

	}

	public void onTitleBarRightImgBtnClick(View view) {
		// TODO Auto-generated method stub

	}

	public void setTitleBarRightBtnText(String lab) {
		if (lab != null && lab.length() > 1) {
			mTitleRightBtn.setText(lab);
			mTitleRightBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleRightBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarLeftBtnText(String lab) {
		if (lab != null && lab.length() > 1) {
			mTitleBackBtn.setText(lab);
			mTitleBackBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleBackBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarRightImageBtnSrc(int srcId) {
		if (srcId != -1) {
			mTitleRightImgBtn.setImageDrawable(getResources()
					.getDrawable(srcId));
			mTitleRightImgBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleRightImgBtn.setVisibility(View.GONE);
		}
	}

	public void setTitleBarLeftImageBtnSrc(int srcId) {
		if (srcId != -1) {
			mTitleLeftImgBtn
					.setImageDrawable(getResources().getDrawable(srcId));
			mTitleLeftImgBtn.setVisibility(View.VISIBLE);
		} else {
			mTitleLeftImgBtn.setVisibility(View.GONE);
		}
	}

	public void hideBackBtn() {
		mTitleBackBtn.setVisibility(View.GONE);
	}
}

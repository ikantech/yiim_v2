package com.ikantech.yiim.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class AccountRootView extends RelativeLayout {

	public AccountRootView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public AccountRootView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public AccountRootView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void refreshDrawableState() {
		super.refreshDrawableState();
		dispatchSetPressed(isPressed());
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			child.setPressed(pressed);
		}
	}
}

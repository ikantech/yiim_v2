package com.ikantech.yiim.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;

public class CommonPopupDialog extends Dialog {
	public CommonPopupDialog(Context context, int theme) {
		super(context, theme);
		setCanceledOnTouchOutside(true);
	}

	public void setAnimations(int anim) {
		getWindow().setWindowAnimations(anim);
	}

	public void showAtLocation(int gravity, int x, int y, int w, int h) {
		WindowManager.LayoutParams wl = getWindow().getAttributes();
		wl.x = x;
		wl.y = y;
		wl.gravity = gravity;
		wl.width = w;
		wl.height = h;
		getWindow().setAttributes(wl);

		show();
	}
}

package com.ikantech.yiim.view;

import android.content.Context;
import android.util.AttributeSet;

public class ImageViewTouch extends ImageViewTouchBase {
	public ImageViewTouch(Context context) {
		super(context);
	}

	public ImageViewTouch(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void postTranslateCenter(float dx, float dy) {
		super.postTranslate(dx, dy);
		center(true, true);
	}
}

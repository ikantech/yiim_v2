/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ikantech.yiim.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ZoomButtonsController;

import com.ikantech.support.util.YiImageUtil;
import com.ikantech.yiim.R;
import com.ikantech.yiim.view.ImageViewTouch;

// This activity can display a whole picture and navigate them in a specific
// gallery. It has two modes: normal mode and slide show mode. In normal mode
// the user view one image at a time, and can click "previous" and "next"
// button to see the previous or next image. In slide show mode it shows one
// image after another, with some transition effect.
public class ViewImageDialog extends Dialog {
	boolean mPaused = true;
	private boolean mShowControls = true;

	final GetterHandler mHandler = new GetterHandler();
	// private CacheManager mCacheManager;

	private Bitmap mFullScreenBitmap;
	public static final String KEY_IMAGE_LIST = "image_list";
	GestureDetector mGestureDetector;
	private ZoomButtonsController mZoomButtonsController;
	// The image view displayed for normal mode.
	private ImageViewTouch mImageView;
	private Context mContext;
	private final Runnable mDismissOnScreenControlRunner = new Runnable() {
		public void run() {
			hideOnScreenControls();
		}
	};

	public ViewImageDialog(Context context, int theme) {
		super(context, theme);
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	private void hideOnScreenControls() {
		mZoomButtonsController.setVisible(false);
	}

	private void showOnScreenControls() {
		if (mPaused)
			return;
		updateZoomButtonsEnabled();
		mZoomButtonsController.setVisible(true);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent m) {
		if (mPaused)
			return true;
		if (mZoomButtonsController.isVisible()) {
			scheduleDismissOnScreenControls();
		}
		return super.dispatchTouchEvent(m);
	}

	private void updateZoomButtonsEnabled() {
		ImageViewTouch imageView = mImageView;
		float scale = imageView.getScale();
		mZoomButtonsController.setZoomInEnabled(scale < imageView.mMaxZoom);
		mZoomButtonsController.setZoomOutEnabled(scale > 1);
	}

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		if (mZoomButtonsController != null) {
			mZoomButtonsController.setVisible(false);
		}
		super.dismiss();
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		if (mZoomButtonsController != null) {
			mZoomButtonsController.setVisible(false);
		}
		super.cancel();
	}

	private void scheduleDismissOnScreenControls() {
		mHandler.removeCallbacks(mDismissOnScreenControlRunner);
		mHandler.postDelayed(mDismissOnScreenControlRunner, 2000);
	}

	private void setupOnScreenControls(View rootView, View ownerView) {
		setupZoomButtonController(ownerView);
		setupOnTouchListeners(rootView);
	}

	private void setupZoomButtonController(final View ownerView) {
		mZoomButtonsController = new ZoomButtonsController(ownerView);
		mZoomButtonsController.setAutoDismissed(false);
		mZoomButtonsController.setZoomSpeed(100);
		mZoomButtonsController
				.setOnZoomListener(new ZoomButtonsController.OnZoomListener() {
					public void onVisibilityChanged(boolean visible) {
						if (visible) {
							updateZoomButtonsEnabled();
						}
					}

					public void onZoom(boolean zoomIn) {
						if (zoomIn) {
							mImageView.zoomIn();
						} else {
							mImageView.zoomOut();
						}
						mZoomButtonsController.setVisible(true);
						updateZoomButtonsEnabled();
					}
				});
	}

	private void setupOnTouchListeners(View rootView) {
		mGestureDetector = new GestureDetector(mContext,
				new MyGestureListener());

		// If the user touches anywhere on the panel (including the
		// next/prev button). We show the on-screen controls. In addition
		// to that, if the touch is not on the prev/next button, we
		// pass the event to the gesture detector to detect double tap.
		final OnTouchListener buttonListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				scheduleDismissOnScreenControls();
				return false;
			}
		};

		OnTouchListener rootListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				buttonListener.onTouch(v, event);
				mGestureDetector.onTouchEvent(event);

				// We do not use the return value of
				// mGestureDetector.onTouchEvent because we will not receive
				// the "up" event if we return false for the "down" event.
				return true;
			}
		};
		rootView.setOnTouchListener(rootListener);
	}

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if (mPaused)
				return false;
			ImageViewTouch imageView = mImageView;
			if (imageView.getScale() > 1F) {
				imageView.postTranslateCenter(-distanceX, -distanceY);
			}
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (mPaused)
				return false;
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (mPaused)
				return false;
			showOnScreenControls();
			scheduleDismissOnScreenControls();
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mPaused)
				return false;
			ImageViewTouch imageView = mImageView;

			// Switch between the original scale and 3x scale.
			if (imageView.getScale() > 2F) {
				mImageView.zoomTo(1f);
			} else {
				mImageView.zoomToPoint(3f, e.getX(), e.getY());
			}
			return true;
		}
	}

	public void setBitmap(Bitmap bitmap) {
		mFullScreenBitmap = bitmap;
	}

	public void setBitmapPath(String path) {
		DisplayMetrics displayMetrics = mContext.getResources()
				.getDisplayMetrics();
		if (mFullScreenBitmap != null && !mFullScreenBitmap.isRecycled()) {
			mFullScreenBitmap.recycle();
			mFullScreenBitmap = null;
		}
		mFullScreenBitmap = YiImageUtil.getImageWithSizeLimit(path,
				displayMetrics.widthPixels, 180);
	}

	void setImage(boolean showControls) {
		if (mFullScreenBitmap != null) {
			mImageView.setImageBitmapResetBase(mFullScreenBitmap, true);
			// mCache.put(0, mFullScreenBitmap);
			updateZoomButtonsEnabled();
		}
		if (showControls)
			showOnScreenControls();
		scheduleDismissOnScreenControls();
	}

	@Override
	public void onCreate(Bundle instanceState) {
		super.onCreate(instanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_viewimage);
		// Intent intent = getIntent();
		// String resId = intent.getStringExtra("res_indicator");
		// mCacheManager = new CacheManager();
		// String bitmapContent = mCacheManager.loadCachedBitmap(resId);
		// if (bitmapContent == null || bitmapContent.length() ==0) {
		// finish();
		// return;
		// }else {
		// byte[] datas = Base64.decode(bitmapContent, Base64.DEFAULT);
		// mFullScreenBitmap = BitmapFactory.decodeByteArray(datas, 0,
		// datas.length);
		// }
		mImageView = (ImageViewTouch) findViewById(R.id.image);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setWindowAnimations(R.style.imageDialogWindowAnim);
		setupOnScreenControls(findViewById(R.id.rootLayout), mImageView);
	}

	@Override
	public void onStart() {
		super.onStart();
		mPaused = false;
		setImage(mShowControls);
		mShowControls = false;
	}

	@Override
	public void onStop() {
		super.onStop();
		mPaused = true;
		// removing all callback in the message queue
		mHandler.removeAllGetterCallbacks();
		hideOnScreenControls();
		mImageView.clear();
		// mCache.clear();
	}

}

class GetterHandler extends Handler {
	private static final int IMAGE_GETTER_CALLBACK = 1;

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case IMAGE_GETTER_CALLBACK:
			((Runnable) message.obj).run();
			break;
		}
	}

	public void postGetterCallback(Runnable callback) {
		postDelayedGetterCallback(callback, 0);
	}

	public void postDelayedGetterCallback(Runnable callback, long delay) {
		if (callback == null) {
			throw new NullPointerException();
		}
		Message message = Message.obtain();
		message.what = IMAGE_GETTER_CALLBACK;
		message.obj = callback;
		sendMessageDelayed(message, delay);
	}

	public void removeAllGetterCallbacks() {
		removeMessages(IMAGE_GETTER_CALLBACK);
	}
}

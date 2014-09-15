package com.ikantech.yiim.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.ikantech.xmppsupport.media.AudioRecorder;
import com.ikantech.yiim.R;

public class VoiceRecordDialog extends Dialog {
	private static final int MSG_RELOAD_DB = 0x01;
	private static final int MSG_START = 0x02;
	private static final int MSG_STOP = 0x03;
	private static final int MSG_DISMISS = 0x04;

	private DigitalAverage mDigitalAverage;
	private AudioRecorder mAudioRecorder;
	private OnErrorListener mErrorListener;
	private Context mContext;

	private ImageView mVoiceLevelImageView;
	private View mRecordingAnimRootView;
	private View mCancelRecordRootView;

	private View mRecordingRootView;
	private View mLoadingRootView;
	private View mTooShortRootView;

	private long mStartTime;
	private long mEndTime;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MSG_RELOAD_DB:
				int level = getLevel();
				int resourceId = mContext.getResources().getIdentifier(
						"amp" + level, "drawable", "com.chyitech.yiim");
				Log.i("test", "level : " + level + " id: " + resourceId);
				if (resourceId > 0) {
					mVoiceLevelImageView.setBackgroundDrawable(mContext
							.getResources().getDrawable(resourceId));
				}
				mHandler.sendEmptyMessageDelayed(MSG_RELOAD_DB, 150L);
				break;
			case MSG_START:
				try {
					mAudioRecorder.startRecorder();
					showRecordingView();
					mHandler.sendEmptyMessage(MSG_RELOAD_DB);
				} catch (Exception e) {
					// TODO: handle exception
					if (mErrorListener != null) {
						mErrorListener.onError(e.getMessage());
					}
				}
				break;
			case MSG_STOP:
				mHandler.removeMessages(MSG_RELOAD_DB);
				try {
					mAudioRecorder.stopRecording();
				} catch (Exception e) {
					// TODO: handle exception
					if (mErrorListener != null) {
						mErrorListener.onError(e.getMessage());
					}
				}
				break;
			case MSG_DISMISS:
				dismiss();
				break;
			default:
				break;
			}
		}
	};

	public VoiceRecordDialog(Context context, int theme, AudioRecorder recorder) {
		super(context, theme);
		// TODO Auto-generated constructor stub
		mContext = context;
		mAudioRecorder = recorder;
		mDigitalAverage = new DigitalAverage();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voice_rcd_hint_window);

		mVoiceLevelImageView = (ImageView) findViewById(R.id.voice_rcd_hint_anim);

		mRecordingAnimRootView = findViewById(R.id.voice_rcd_hint_anim_area);
		mCancelRecordRootView = findViewById(R.id.voice_rcd_hint_cancel_area);

		mRecordingRootView = findViewById(R.id.voice_rcd_hint_rcding);
		mLoadingRootView = findViewById(R.id.voice_rcd_hint_loading);
		mTooShortRootView = findViewById(R.id.voice_rcd_hint_tooshort);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mCancelRecordRootView.setVisibility(View.GONE);
		mRecordingAnimRootView.setVisibility(View.VISIBLE);
		mRecordingRootView.setVisibility(View.GONE);
		mTooShortRootView.setVisibility(View.GONE);
		mLoadingRootView.setVisibility(View.VISIBLE);

		mStartTime = System.currentTimeMillis();
		mHandler.sendEmptyMessage(MSG_START);
		mHandler.removeMessages(MSG_RELOAD_DB);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		mHandler.sendEmptyMessage(MSG_STOP);
		mHandler.removeMessages(MSG_RELOAD_DB);
	}

	public void setOnErrorListener(OnErrorListener listener) {
		mErrorListener = listener;
	}

	public void requestDismiss() {
		mEndTime = System.currentTimeMillis();
		if (mEndTime - mStartTime < 1000) {
			mRecordingRootView.setVisibility(View.GONE);
			mLoadingRootView.setVisibility(View.GONE);
			mTooShortRootView.setVisibility(View.VISIBLE);
			mHandler.sendEmptyMessageDelayed(MSG_DISMISS, 200);
		} else {
			dismiss();
		}
	}

	public void showRecordingView() {
		mTooShortRootView.setVisibility(View.GONE);
		mLoadingRootView.setVisibility(View.GONE);
		mRecordingRootView.setVisibility(View.VISIBLE);

		mCancelRecordRootView.setVisibility(View.GONE);
		mRecordingAnimRootView.setVisibility(View.VISIBLE);
	}

	public void showCancelRecordView() {
		mTooShortRootView.setVisibility(View.GONE);
		mLoadingRootView.setVisibility(View.GONE);
		mRecordingRootView.setVisibility(View.VISIBLE);

		mRecordingAnimRootView.setVisibility(View.GONE);
		mCancelRecordRootView.setVisibility(View.VISIBLE);
	}

	public float getAmplitude() {
		float f = 0.0F;
		if (mAudioRecorder != null && mAudioRecorder.getMediaRecorder() != null)
			f = mDigitalAverage.average(mAudioRecorder.getMediaRecorder()
					.getMaxAmplitude());
		return f;
	}

	public int getLevel() {
		double splValue = (20.0D * Math.log10(getAmplitude()));
		splValue = Math.round(splValue);
		if (splValue >= 50.0 && splValue < 60.0) {
			return 2;
		} else if (splValue >= 60.0 && splValue < 70) {
			return 3;
		} else if (splValue >= 70.0 && splValue < 80) {
			return 4;
		} else if (splValue >= 80.0 && splValue < 85) {
			return 5;
		} else if (splValue >= 85.0 && splValue < 95) {
			return 6;
		} else if (splValue >= 95.0) {
			return 7;
		} else {
			return 1;
		}
	}

	private class DigitalAverage {
		float[] mLocHistory = new float[4];
		int mLocPos = 0;

		public float average(float paramFloat) {
			float f1 = 0.0F;
			float f3;
			if (paramFloat == 0.0F) {
				f3 = 0.0F;
			}
			this.mLocPos = (1 + this.mLocPos);
			if (this.mLocPos > this.mLocHistory.length - 1)
				this.mLocPos = 0;
			this.mLocHistory[this.mLocPos] = paramFloat;

			float[] arrayOfFloat = this.mLocHistory;
			int i = arrayOfFloat.length;
			for (int j = 0;; j++) {
				if (j >= i) {
					f3 = f1 / this.mLocHistory.length;
					break;
				}
				float f2 = arrayOfFloat[j];
				if (f2 == 0.0F)
					f2 = paramFloat;
				f1 += f2;
			}

			return f3;
		}
	}

	public interface OnErrorListener {
		void onError(String msg);
	}
}

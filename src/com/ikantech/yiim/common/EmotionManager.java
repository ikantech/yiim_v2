package com.ikantech.yiim.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.ikantech.yiim.R;
import com.ikantech.yiim.ui.ChatActivity;

public class EmotionManager implements View.OnClickListener {
	private static final int EMOTION = 0x01;
	private static final int EMOTION_CLASSICAL = 0x02;

	private Context mContext;
	private ViewPager mViewPager;
	private LinearLayout mPagerIndexPanel;
	private LinearLayout mEmojiToolBarView;
	private NativePagerAdapter mAdapter;
	private Map<Integer, ViewHolder> mPages;
	private Map<Integer, ViewHolder> mClassicalPages;

	private NativeOnPageChangeListener mPageChangeListener;

	private static ArrayList<Integer> mEmotions;
	private ArrayList<ImageView> mPointerImageViews;
	private static ArrayList<String> mEmotionDescs;

	private static int[] mClassicalEmotions;
	private static int[] mClassicalStaticEmotions;
	private ArrayList<ImageView> mClassicalPointerImageViews;
	private static String[] mClassicalEmotionDescs;

	private Handler mHandler;

	private int mCurrentEmotion = EMOTION_CLASSICAL;
	private int mCurrrentEmotionPage = 0;
	private int mCurrrentClassicalEmotionPage = 0;

	public EmotionManager(Context context, ViewPager viewPager,
			View pagerIndex, View emojiToolBar, Handler handler) {
		mContext = context;
		mViewPager = viewPager;
		mPagerIndexPanel = (LinearLayout) pagerIndex;
		mEmojiToolBarView = (LinearLayout) emojiToolBar;
		mPages = new HashMap<Integer, EmotionManager.ViewHolder>();
		mClassicalPages = new HashMap<Integer, EmotionManager.ViewHolder>();

		mAdapter = new NativePagerAdapter();
		mPageChangeListener = new NativeOnPageChangeListener();

		mHandler = handler;

		initializeEmoji(mContext);
		initializeClassicalEmoji(mContext);

		initializeEmoji();
		initializeClassicalEmoji();

		Button btn = (Button) mEmojiToolBarView.findViewById(R.id.emoji_btn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mCurrentEmotion = EMOTION;
				destory();
				initialize();
				mAdapter.notifyDataSetChanged();
			}
		});

		btn = (Button) mEmojiToolBarView.findViewById(R.id.emoji_classical_btn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mCurrentEmotion = EMOTION_CLASSICAL;
				destory();
				initialize();
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	private void initializeEmoji() {
		mPointerImageViews = new ArrayList<ImageView>();
		int totalpages = (int) Math.ceil(mEmotions.size() / 8.0F);
		for (int i = 0; i < totalpages; i++) {
			ImageView imageView = new ImageView(mContext);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.leftMargin = 5;
			imageView.setLayoutParams(params);
			mPointerImageViews.add(imageView);
		}
	}

	public static void initializeEmoji(Context context) {
		if (mEmotions != null && mEmotionDescs != null) {
			return;
		}

		mEmotions = new ArrayList<Integer>();
		mEmotionDescs = new ArrayList<String>();

		for (int i = 1; i < 53; i++) {
			mEmotions.add(Integer.valueOf(context.getResources().getIdentifier(
					"emo_" + i, "drawable", context.getPackageName())));
			mEmotionDescs.add(String.format(":emo:%d:", i));
		}
	}

	private void initializeClassicalEmoji() {
		mClassicalPointerImageViews = new ArrayList<ImageView>();
		int totalpages = (int) Math.ceil(mClassicalEmotions.length / 21.0F);
		for (int i = 0; i < totalpages; i++) {
			ImageView imageView = new ImageView(mContext);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.leftMargin = 5;
			imageView.setLayoutParams(params);
			mClassicalPointerImageViews.add(imageView);
		}
	}

	public static void initializeClassicalEmoji(Context context) {
		if (mClassicalEmotionDescs != null && mClassicalEmotions != null
				&& mClassicalStaticEmotions != null) {
			return;
		}

		String[] classical = context.getResources().getStringArray(
				R.array.classical_emoji);
		String[] classicalStatic = context.getResources().getStringArray(
				R.array.classical_static_emoji);

		mClassicalEmotions = new int[classical.length];
		mClassicalStaticEmotions = new int[classicalStatic.length];
		for (int i = 0; i < classicalStatic.length; i++) {
			mClassicalEmotions[i] = context.getResources().getIdentifier(
					classical[i], "drawable", context.getPackageName());
			mClassicalStaticEmotions[i] = context.getResources().getIdentifier(
					classicalStatic[i], "drawable", context.getPackageName());
		}

		mClassicalEmotionDescs = context.getResources().getStringArray(
				R.array.classical_emoji_desc);
	}

	public void initialize() {
		mEmojiToolBarView.setVisibility(View.VISIBLE);
		mViewPager.setAdapter(mAdapter);

		ArrayList<ImageView> imageViews = null;
		if (mCurrentEmotion == EMOTION) {
			imageViews = mPointerImageViews;
		} else {
			imageViews = mClassicalPointerImageViews;
		}

		for (ImageView imageView : imageViews) {
			imageView.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.page));
			mPagerIndexPanel.addView(imageView);
		}
		mViewPager.setOnPageChangeListener(mPageChangeListener);

		int page = 0;
		if (mCurrentEmotion == EMOTION) {
			page = mCurrrentEmotionPage;
		} else {
			page = mCurrrentClassicalEmotionPage;
		}

		ImageView imageView = imageViews.get(page);
		imageView.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.page_now));

		mViewPager.setCurrentItem(page);
	}

	public void destory() {
		mEmojiToolBarView.setVisibility(View.GONE);

		mViewPager.setAdapter(null);
		mViewPager.removeAllViews();
		mPagerIndexPanel.removeAllViews();
		mViewPager.setOnPageChangeListener(null);
	}

	private ViewHolder instanceView(int position, int emoji) {
		ViewHolder viewHolder = new ViewHolder();

		View rootView = null;
		int base_index = 0;
		int count = 0;
		if (emoji == EMOTION) {
			count = 8;
			base_index = position * count;

			rootView = LayoutInflater.from(mContext).inflate(
					R.layout.emotion_pager_item, null);
		} else {
			count = 21;
			base_index = position * count;

			rootView = LayoutInflater.from(mContext).inflate(
					R.layout.emotion_pager_classical_item, null);
		}

		viewHolder.emotionViews = new ArrayList<ImageButton>();

		for (int i = 0; i < count; i++) {
			ImageButton view = (ImageButton) rootView.findViewById(mContext
					.getResources().getIdentifier("emo_" + (i + 1), "id",
							mContext.getPackageName()));
			view.setOnClickListener(this);
			view.setTag(String.valueOf(base_index + i));
			viewHolder.emotionViews.add(view);
		}

		viewHolder.rootView = rootView;
		return viewHolder;
	}

	private class NativePagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if (mCurrentEmotion == EMOTION) {
				return (int) Math.ceil(mEmotions.size() / 8.0f);
			} else {
				return mClassicalEmotions.length / 21;
			}
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			Map<Integer, ViewHolder> pages = null;
			if (mCurrentEmotion == EMOTION) {
				pages = mPages;
			} else {
				pages = mClassicalPages;
			}

			ViewHolder viewHolder = pages.get(Integer.valueOf(position));
			if (viewHolder != null) {
				((ViewPager) container).removeView(viewHolder.rootView);
			}
		}

		@Override
		public Object instantiateItem(View container, int position) {
			ViewHolder viewHolder = null;
			if (mCurrentEmotion == EMOTION) {
				viewHolder = mPages.get(Integer.valueOf(position));
				if (viewHolder == null) {
					viewHolder = instanceView(position, EMOTION);
					mPages.put(Integer.valueOf(position), viewHolder);
				}

				int offset = position * 8;
				for (int i = offset; i < mEmotions.size()
						&& i < (position + 1) * 8; i++) {
					ImageButton button = viewHolder.emotionViews
							.get(i - offset);
					button.setImageDrawable(mContext.getResources()
							.getDrawable(mEmotions.get(i)));
				}
			} else {
				viewHolder = mClassicalPages.get(Integer.valueOf(position));
				if (viewHolder == null) {
					viewHolder = instanceView(position, EMOTION_CLASSICAL);
					mClassicalPages.put(Integer.valueOf(position), viewHolder);
				}

				int offset = position * 21;
				for (int i = offset; i < mClassicalEmotions.length
						&& i < (position + 1) * 21; i++) {
					ImageButton button = viewHolder.emotionViews
							.get(i - offset);

					button.setImageDrawable(mContext.getResources()
							.getDrawable(mClassicalStaticEmotions[i]));
				}
			}

			((ViewPager) container).addView(viewHolder.rootView);
			return viewHolder.rootView;
		}

	}

	public class NativeOnPageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageScrollStateChanged(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPageSelected(int arg0) {
			// TODO Auto-generated method stub
			ImageView nowImageView = null;
			ImageView beforeImageView = null;
			ImageView afterImageView = null;
			if (mCurrentEmotion == EMOTION) {
				if (arg0 < 0 || arg0 > mPointerImageViews.size() - 1) {
					return;
				}
				mCurrrentEmotionPage = arg0;
				nowImageView = mPointerImageViews.get(arg0);
				beforeImageView = arg0 > 0 ? mPointerImageViews.get(arg0 - 1)
						: null;
				afterImageView = arg0 < (mPointerImageViews.size() - 1) ? mPointerImageViews
						.get(arg0 + 1) : null;
			} else {
				if (arg0 < 0 || arg0 > mClassicalPointerImageViews.size() - 1) {
					return;
				}
				mCurrrentClassicalEmotionPage = arg0;
				nowImageView = mClassicalPointerImageViews.get(arg0);
				beforeImageView = arg0 > 0 ? mClassicalPointerImageViews
						.get(arg0 - 1) : null;
				afterImageView = arg0 < (mClassicalPointerImageViews.size() - 1) ? mClassicalPointerImageViews
						.get(arg0 + 1) : null;
			}

			nowImageView.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.page_now));

			if (beforeImageView != null) {
				beforeImageView.setImageDrawable(mContext.getResources()
						.getDrawable(R.drawable.page));
			}

			if (afterImageView != null) {
				afterImageView.setImageDrawable(mContext.getResources()
						.getDrawable(R.drawable.page));
			}
		}
	}

	private class ViewHolder {
		View rootView;
		ArrayList<ImageButton> emotionViews;
	}

	public static int getEmotionResourceId(String message) {
		if (message.startsWith(":emo:")) {
			for (int i = 0; i < mEmotionDescs.size(); i++) {
				if (mEmotionDescs.get(i).equals(message)) {
					return mEmotions.get(i);
				}
			}
		} else {
			for (int i = 0; i < mClassicalEmotionDescs.length; i++) {
				if (mClassicalEmotionDescs[i].equals(message)) {
					return mClassicalEmotions[i];
				}
			}
		}
		return -1;
	}

	public static int getEmotionStaticResourceId(String message) {
		if (message.startsWith(":emo:")) {
			for (int i = 0; i < mEmotionDescs.size(); i++) {
				if (mEmotionDescs.get(i).equals(message)) {
					return mEmotions.get(i);
				}
			}
		} else {
			for (int i = 0; i < mClassicalEmotionDescs.length; i++) {
				if (mClassicalEmotionDescs[i].equals(message)) {
					return mClassicalStaticEmotions[i];
				}
			}
		}
		return -1;
	}

	public static int[] getClassicalEmotions() {
		return mClassicalEmotions;
	}

	public static String[] getClassicalEmotionDescs() {
		return mClassicalEmotionDescs;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		String tag = (String) v.getTag();
		if (tag != null) {
			try {
				int index = Integer.parseInt(tag);
				if (mCurrentEmotion == EMOTION) {
					Message message = mHandler.obtainMessage(
							ChatActivity.MSG_SEND_EMOTION,
							mEmotionDescs.get(index));
					message.sendToTarget();
				} else {
					Message message = mHandler.obtainMessage(
							ChatActivity.MSG_SEND_CLASSICAL_EMOTION,
							mClassicalEmotionDescs[index]);
					message.sendToTarget();
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
}

package com.ikantech.yiim.adapter;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ikantech.support.util.YiImageUtil;
import com.ikantech.xmppsupport.gif.GifEmotionUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.EmotionManager;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.DateUtils;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class ChatRecordAdapter extends CursorAdapter {
	private LayoutInflater mInflater;
	private Context mContext;

	private Map<String, SoftReference<XmppVcard>> mVcards;
	private Map<String, SoftReference<Bitmap>> mCache;
	private XmppBinder mXmppBinder;

	private GifEmotionUtils mGifEmotionUtils;

	private View.OnClickListener mOnImageClickListener;
	private View.OnClickListener mOnAudioClickListener;
	
	private Handler mHandler = new Handler();

	public ChatRecordAdapter(Context context, Cursor c, XmppBinder xmppBinder) {
		super(context, c, false);
		// TODO Auto-generated constructor stub
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mVcards = new HashMap<String, SoftReference<XmppVcard>>();
		mCache = new HashMap<String, SoftReference<Bitmap>>();
		mXmppBinder = xmppBinder;

		EmotionManager.initializeEmoji(mContext);
		EmotionManager.initializeClassicalEmoji(mContext);

		mGifEmotionUtils = new GifEmotionUtils(mContext,
				EmotionManager.getClassicalEmotions(),
				EmotionManager.getClassicalEmotionDescs(), R.drawable.face);
	}

	public void setOnImageClickListener(View.OnClickListener listener) {
		mOnImageClickListener = listener;
	}

	public void setOnAudioClickListener(View.OnClickListener listener) {
		mOnAudioClickListener = listener;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = (ViewHolder) view.getTag();

		try {
			String uu = cursor.getString(1);
			if (YiIMUtils.isMultChat(uu)) {
				uu = StringUtils.getJidResouce(uu) + "@"
						+ XmppConnectionUtils.getXmppHost();
			} else {
				uu = StringUtils.escapeUserResource(uu);
			}
			final String sender = uu;

			final String dateString = DateUtils.format(mContext, new Date(
					cursor.getLong(3)));

			viewHolder.text1.setText(StringUtils.escapeUserHost(sender) + " "
					+ dateString);

			final TextView textView = viewHolder.text1;
			SoftReference<XmppVcard> softReference = mVcards.get(sender);
			if (softReference != null && softReference.get() != null) {
				XmppVcard vcard = softReference.get();
				textView.setText(vcard.getDisplayName() + " " + dateString);
			} else {
				mXmppBinder.execute(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						final XmppVcard xmppVcard = new XmppVcard(mXmppBinder
								.getServiceContext());
						xmppVcard.load(mXmppBinder.getXmppConnection(), sender);
						mVcards.put(sender, new SoftReference<XmppVcard>(
								xmppVcard));
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								textView.setText(xmppVcard.getDisplayName()
										+ " " + dateString);
							}
						});
					}
				});
			}

			YiIMMessage message = YiIMMessage.fromString(cursor.getString(2));
			dealMsg(message, viewHolder.text2);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void dealMsg(YiIMMessage message, TextView textView) {
		MsgType msgType = message.getType();
		String content = message.getBody();

		textView.setOnClickListener(null);
		textView.setTag(null);
		textView.setText("");

		if (MsgType.PLANTEXT.equals(msgType)) {
			// 处理表情
			// v1.setText(content);
			mGifEmotionUtils.setSpannableText(textView, content, mHandler);
			textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
					null);
		} else if (MsgType.BIG_EMOTION.equals(msgType)) {// 处理大表情
			int emotionId = EmotionManager.getEmotionResourceId(content);
			if (emotionId != -1) {
				mGifEmotionUtils.setSpannableText(textView, "", mHandler);
				textView.setCompoundDrawablesWithIntrinsicBounds(mContext
						.getResources().getDrawable(emotionId), null, null,
						null);
			}
		} else if (MsgType.IMAGE.equals(msgType)) { // 处理图片信息
			String filePath = message.getBody();

			Bitmap bitmap = null;
			if (mCache.get(filePath) != null
					&& mCache.get(filePath).get() != null) {
				bitmap = mCache.get(filePath).get();
			} else {
				bitmap = YiImageUtil.getImageWithSizeLimit(filePath, -1, 180);
				mCache.put(filePath, new SoftReference<Bitmap>(bitmap));
			}

			textView.setCompoundDrawablesWithIntrinsicBounds(
					new BitmapDrawable(bitmap), null, null, null);
			textView.setTag(filePath);
			textView.setOnClickListener(mOnImageClickListener);

			RelativeLayout.LayoutParams imageLayoutParams = null;
			if (bitmap != null) {
				imageLayoutParams = (RelativeLayout.LayoutParams) textView
						.getLayoutParams();
				imageLayoutParams.height = bitmap.getHeight() > 180 ? 180
						: bitmap.getHeight();
				textView.setLayoutParams(imageLayoutParams);
			}
		} else if (MsgType.AUDIO.equals(msgType)) { // 处理语音信息
			String filePath = content;

			mGifEmotionUtils.setSpannableText(textView, "", mHandler);

			int duration = -1;
			try {
				duration = Integer.valueOf(message.getParams().get(
						"audio_duration"));

				mGifEmotionUtils.setSpannableText(textView,
						String.format("%d\"", duration / 1000), mHandler);
			} catch (Exception e) {
				// TODO: handle exception
			}

			textView.setCompoundDrawablesWithIntrinsicBounds(
					mContext.getResources().getDrawable(
							R.drawable.chatto_voice_playing_r), null, null,
					null);
			textView.setTag(filePath);
			textView.setOnClickListener(mOnAudioClickListener);
		} else if (MsgType.VIDEO.equals(msgType)) { // 处理视频信息
			// String filePath = message.getBody();
			// textView.setImageDrawable(mContext.getResources().getDrawable(
			// R.drawable.chatfrom_voice_playing));
			// textView.setTag(filePath);
			// textView.setOnClickListener(mOnVideoClickListener);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub
		View convertView = mInflater.inflate(R.layout.chat_record_item, null);

		ViewHolder holder = new ViewHolder();

		holder.text1 = (TextView) convertView.findViewById(R.id.text1);
		holder.text2 = (TextView) convertView.findViewById(R.id.text2);

		convertView.setTag(holder);

		return convertView;
	}

	private class ViewHolder {
		TextView text1;
		TextView text2;
	}
}

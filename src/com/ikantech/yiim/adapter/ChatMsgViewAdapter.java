package com.ikantech.yiim.adapter;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.support.util.YiImageUtil;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.gif.GifEmotionUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.EmotionManager;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.entity.YiIMMessage;
import com.ikantech.yiim.entity.YiIMMessage.MsgType;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class ChatMsgViewAdapter extends CursorAdapter {
	private static final int MSG_TIME_SHOW_DELAY = 300000;

	private LayoutInflater mInflater;
	private String mUser;
	private EmotionManager mEmotionManager;
	private Context mContext;
	private XmppBinder mXmppBinder;
	private boolean mIsMultChat;

	private GifEmotionUtils mGifEmotionUtils;

	private View.OnClickListener mOnImageClickListener;
	private View.OnClickListener mOnAudioClickListener;
	private View.OnClickListener mOnVideoClickListener;
	private View.OnClickListener mResendClickListener;

	private Map<String, SoftReference<XmppVcard>> mVcards;
	private Map<String, SoftReference<Bitmap>> mCache;
	
	private Handler mHandler = new Handler();

	public View.OnClickListener getOnVideoClickListener() {
		return mOnVideoClickListener;
	}

	public void setOnVideoClickListener(
			View.OnClickListener mOnVideoClickListener) {
		this.mOnVideoClickListener = mOnVideoClickListener;
	}

	public void setResendClickListener(View.OnClickListener listener) {
		mResendClickListener = listener;
	}

	public ChatMsgViewAdapter(Context context, XmppBinder xmppBinder,
			GifEmotionUtils gifEmotionUtils, Cursor c, String currentUser,
			EmotionManager emotionManager) {
		super(context, c, true);
		// TODO Auto-generated constructor stub
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mUser = currentUser;
		mEmotionManager = emotionManager;

		mGifEmotionUtils = gifEmotionUtils;

		mVcards = new HashMap<String, SoftReference<XmppVcard>>();
		mCache = new HashMap<String, SoftReference<Bitmap>>();
		mXmppBinder = xmppBinder;
		mIsMultChat = false;
	}

	public void setOnImageClickListener(View.OnClickListener listener) {
		mOnImageClickListener = listener;
	}

	public void setOnAudioClickListener(View.OnClickListener listener) {
		mOnAudioClickListener = listener;
	}

	public void setIsMultiChat(boolean v) {
		mIsMultChat = v;
	}

	private int transfer(int position) {
		return getCount() - position - 1;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return super.getDropDownView(transfer(position), convertView, parent);
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		position = transfer(position);
		if (position < 0 || position > getCursor().getCount() - 1) {
			return null;
		}
		return super.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return super.getItemId(transfer(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return super.getView(transfer(position), convertView, parent);
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = (ViewHolder) arg0.getTag();
		if (viewHolder != null) {
			String uu = arg2.getString(arg2.getColumnIndex(MsgColumns.SENDER));
			if (YiIMUtils.isMultChat(uu)) {
				uu = StringUtils.getJidResouce(uu) + "@"
						+ XmppConnectionUtils.getXmppHost();
			} else {
				uu = StringUtils.escapeUserResource(uu);
			}
			final String sender = uu;
			boolean isComMsg = !sender.startsWith(mUser);

			long date = arg2.getLong(arg2
					.getColumnIndex(MsgColumns.CREATE_DATE));
			viewHolder.systemTip.setText(DateFormat.format(
					"yyyy-MM-dd hh:mm:ss", new Date(date)));

			viewHolder.systemTip.setVisibility(View.GONE);

			int position = arg2.getPosition();

			Cursor cursor = (Cursor) getItem(arg2.getCount()
					- arg2.getPosition() - 2);
			if (cursor != null) {
				long date2 = cursor.getLong(cursor
						.getColumnIndex(MsgColumns.CREATE_DATE));
				if (Math.abs(date2 - date) > MSG_TIME_SHOW_DELAY) {
					viewHolder.systemTip.setVisibility(View.VISIBLE);
				}
			}

			arg2.moveToPosition(position);

			if (arg2.getPosition() == arg2.getCount() - 1) {
				viewHolder.systemTip.setVisibility(View.VISIBLE);
			}

			String content = arg2.getString(arg2
					.getColumnIndex(MsgColumns.CONTENT));
			YiLog.getInstance().i("msg %s", content);
			YiIMMessage message = null;
			try {
				message = YiIMMessage.fromString(content);
			} catch (Exception e) {
				// TODO: handle exception
				YiLog.getInstance().e(e, "create YiIMMessage failed. %s",
						content);
				return;
			}

			if (!mIsMultChat) {
				viewHolder.leftUserName.setVisibility(View.GONE);
				viewHolder.rightUserName.setVisibility(View.GONE);
			}

			TextView userTextView = null;
			ImageView userImageView = null;
			// 如果消息是对方发给我的
			if (isComMsg) {
				viewHolder.rightView.setVisibility(View.GONE);
				viewHolder.leftView.setVisibility(View.VISIBLE);

				userTextView = viewHolder.leftUserName;
				userImageView = viewHolder.leftHead;
				dealMsg(arg2.getInt(0), viewHolder.leftContent,
						viewHolder.leftImageRoot, viewHolder.leftImage,
						viewHolder.leftImageProgress, viewHolder.leftImageMask,
						viewHolder.leftAddon, message, isComMsg);
			} else {
				viewHolder.leftView.setVisibility(View.GONE);
				viewHolder.rightView.setVisibility(View.VISIBLE);

				userTextView = viewHolder.rightUserName;
				userImageView = viewHolder.rightHead;

				dealMsg(arg2.getInt(0), viewHolder.rightContent,
						viewHolder.rightImageRoot, viewHolder.rightImage,
						viewHolder.rightImageProgress,
						viewHolder.rightImageMask, viewHolder.rightAddon,
						message, isComMsg);
			}

			userTextView.setText(StringUtils.escapeUserHost(sender));

			final TextView textView = userTextView;
			SoftReference<XmppVcard> softReference = mVcards.get(sender);
			if (softReference != null && softReference.get() != null) {
				XmppVcard vcard = softReference.get();
				userTextView.setText(vcard.getDisplayName());
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
						textView.post(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								textView.setText(xmppVcard.getDisplayName());
							}
						});
					}
				});
			}

			userImageView.setImageResource(R.drawable.mini_avatar_shadow);
			userImageView.setTag(sender);
			final ImageView imageView = userImageView;
			YiAsyncImageLoader.loadBitmapFromStore(sender,
					new YiImageLoaderListener() {

						@Override
						public void onImageLoaded(String url, Bitmap bitmap) {
							// TODO Auto-generated method stub
							imageView.setImageBitmap(bitmap);
						}
					});
		}
	}

	private void dealMsg(int id, TextView contentView, View imageRoot,
			ImageView imageView, TextView imageProgress, View imageMask,
			ImageView addon, YiIMMessage message, boolean isComMsg) {
		String content = message.getBody();
		MsgType msgType = message.getType();

		contentView.setOnClickListener(null);
		contentView.setTag(null);
		addon.setVisibility(View.GONE);
		contentView.setVisibility(View.GONE);
		imageRoot.setVisibility(View.GONE);

		addon.setTag(null);
		addon.setVisibility(View.GONE);
		addon.setOnClickListener(null);

		if (MsgType.PLANTEXT.equals(msgType)) {
			// 处理表情
			// v1.setText(content);
			mGifEmotionUtils.setSpannableText(contentView, content, mHandler);
			contentView.setVisibility(View.VISIBLE);
			contentView.setCompoundDrawablesWithIntrinsicBounds(null, null,
					null, null);
		} else if (MsgType.BIG_EMOTION.equals(msgType)) {// 处理大表情
			int emotionId = EmotionManager.getEmotionResourceId(content);
			if (emotionId != -1) {
				mGifEmotionUtils.setSpannableText(contentView, "", mHandler);
				contentView.setVisibility(View.VISIBLE);
				contentView.setCompoundDrawablesWithIntrinsicBounds(mContext
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

			imageRoot.setVisibility(View.VISIBLE);

			imageMask.setVisibility(View.VISIBLE);
			imageView.setImageBitmap(bitmap);
			imageView.setTag(Integer.valueOf(id));
			imageView.setOnClickListener(mOnImageClickListener);

			RelativeLayout.LayoutParams imageLayoutParams = null;
			if (bitmap != null) {
				imageLayoutParams = (RelativeLayout.LayoutParams) imageView
						.getLayoutParams();
				imageLayoutParams.height = bitmap.getHeight() > 180 ? 180
						: bitmap.getHeight();
				imageView.setLayoutParams(imageLayoutParams);
			}

			String status = message.getParams().get("status");
			if (!YiUtils.isStringInvalid(status) && "start".equals(status)
					&& imageLayoutParams != null) {
				imageProgress.setVisibility(View.VISIBLE);
				int pr = 0;
				try {
					pr = Integer.parseInt(message.getParams().get("progress"));
				} catch (Exception e) {
					// TODO: handle exception
				}
				imageProgress.setText(pr + "%");

				RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) imageMask
						.getLayoutParams();
				layoutParams.height = (int) ((1.0F - (pr / 100.0F)) * imageLayoutParams.height);
				imageMask.setLayoutParams(layoutParams);
			} else if (org.jivesoftware.smackx.filetransfer.FileTransfer.Status.complete
					.toString().equals(status)) {
				imageProgress.setVisibility(View.GONE);
				imageMask.setVisibility(View.GONE);
			} else {
				imageProgress.setVisibility(View.GONE);
				imageMask.setVisibility(View.GONE);
				addon.setVisibility(View.VISIBLE);
				addon.setTag(Integer.valueOf(id));
				addon.setImageResource(R.drawable.btn_style_resend);
				addon.setOnClickListener(mResendClickListener);
			}
		} else if (MsgType.AUDIO.equals(msgType)) { // 处理语音信息
			String filePath = content;

			contentView.setVisibility(View.VISIBLE);

			mGifEmotionUtils.setSpannableText(contentView, "", mHandler);

			int duration = -1;
			try {
				duration = Integer.valueOf(message.getParams().get(
						"audio_duration"));

				mGifEmotionUtils.setSpannableText(contentView,
						String.format("%d\"", duration / 1000), mHandler);
			} catch (Exception e) {
				// TODO: handle exception
			}

			if (isComMsg) {
				contentView.setCompoundDrawablesWithIntrinsicBounds(
						mContext.getResources().getDrawable(
								R.drawable.chatfrom_voice_playing), null, null,
						null);
			} else {
				contentView.setCompoundDrawablesWithIntrinsicBounds(
						null,
						null,
						mContext.getResources().getDrawable(
								R.drawable.chatto_voice_playing), null);
			}
			contentView.setTag(filePath);
			contentView.setOnClickListener(mOnAudioClickListener);
		} else if (MsgType.VIDEO.equals(msgType)) { // 处理视频信息
			String filePath = message.getBody();
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.chatfrom_voice_playing));
			imageView.setTag(filePath);
			imageView.setOnClickListener(mOnVideoClickListener);
		}
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		View convertView = mInflater.inflate(R.layout.chatting_item_msg_text,
				null);

		ViewHolder viewHolder = new ViewHolder();

		viewHolder.systemTip = (TextView) convertView
				.findViewById(R.id.chatting_item_system);
		viewHolder.leftView = convertView.findViewById(R.id.chatting_item_left);
		viewHolder.leftHead = (ImageView) convertView
				.findViewById(R.id.chatting_item_left_userhead);
		viewHolder.leftUserName = (TextView) convertView
				.findViewById(R.id.chatting_item_left_username);
		viewHolder.leftAddon = (ImageView) convertView
				.findViewById(R.id.chatting_item_left_addon);
		viewHolder.leftContent = (TextView) convertView
				.findViewById(R.id.chatting_item_left_content);
		viewHolder.leftImage = (ImageView) convertView
				.findViewById(R.id.content_left_image);
		viewHolder.leftImageRoot = convertView
				.findViewById(R.id.content_left_image_root);
		viewHolder.leftImageProgress = (TextView) convertView
				.findViewById(R.id.content_left_image_progress);
		viewHolder.leftImageMask = convertView
				.findViewById(R.id.content_left_image_mask);

		viewHolder.rightView = convertView
				.findViewById(R.id.chatting_item_right);
		viewHolder.rightHead = (ImageView) convertView
				.findViewById(R.id.chatting_item_right_userhead);
		viewHolder.rightUserName = (TextView) convertView
				.findViewById(R.id.chatting_item_right_username);
		viewHolder.rightAddon = (ImageView) convertView
				.findViewById(R.id.chatting_item_right_addon);
		viewHolder.rightContent = (TextView) convertView
				.findViewById(R.id.chatting_item_right_content);
		viewHolder.rightImage = (ImageView) convertView
				.findViewById(R.id.content_right_image);
		viewHolder.rightImageRoot = convertView
				.findViewById(R.id.content_right_image_root);
		viewHolder.rightImageProgress = (TextView) convertView
				.findViewById(R.id.content_right_image_progress);
		viewHolder.rightImageMask = convertView
				.findViewById(R.id.content_right_image_mask);

		convertView.setTag(viewHolder);
		return convertView;
	}

	private class ViewHolder {
		TextView systemTip;

		View leftView;
		TextView leftUserName;
		TextView leftContent;
		ImageView leftAddon;
		View leftImageRoot;
		TextView leftImageProgress;
		View leftImageMask;
		ImageView leftImage;
		ImageView leftHead;

		View rightView;
		ImageView rightHead;
		ImageView rightAddon;
		TextView rightUserName;
		TextView rightContent;
		View rightImageRoot;
		TextView rightImageProgress;
		View rightImageMask;
		ImageView rightImage;
	}
}

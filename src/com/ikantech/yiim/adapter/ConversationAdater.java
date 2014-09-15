package com.ikantech.yiim.adapter;

import java.sql.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.entity.MultiChatDesc;
import com.ikantech.yiim.entity.XmppVcard;
import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.ConversationManager.ConversationType;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.service.XmppService.XmppBinder;
import com.ikantech.yiim.util.DateUtils;
import com.ikantech.yiim.util.StringUtils;
import com.ikantech.yiim.util.YiIMUtils;

public class ConversationAdater extends CursorAdapter {
	private Context mContext;
	private XmppBinder mXmppServiceBinder;

	public ConversationAdater(Context context, Cursor cursor,
			XmppBinder xmppServiceBinder) {
		// TODO Auto-generated constructor stub
		super(context, cursor, true);
		mContext = context;
		mXmppServiceBinder = xmppServiceBinder;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		final ViewHolder holder = (ViewHolder) view.getTag();
		if (holder != null && cursor != null) {
			// if (item.getIconBitmap() != null) {
			// holder.mIconView.setImageBitmap(item.getIconBitmap());
			// } else {
			// }
			String type = cursor.getString(cursor
					.getColumnIndex(ConversationColumns.MSG_TYPE));

			String uu = cursor.getString(cursor
					.getColumnIndex(ConversationColumns.MSG));

			if (ConversationType.ENTRY_ADD_REQUEST.toString().equals(type)
					&& YiIMUtils.isMultChat(uu)) {
				uu = StringUtils.getJidResouce(uu) + "@"
						+ XmppConnectionUtils.getXmppHost();
			} else {
				uu = StringUtils.escapeUserResource(uu);
			}
			holder.mMsgView.setText(uu.replaceAll("@.+$", ""));

			final String user = uu;

			if (YiIMUtils.isMultChat(user)) {
				holder.mIconView
						.setImageResource(MultiChatDesc.MultiChatIcon.DEFAULIT_4
								.getResId());
				loadMultiChat(holder.mMsgView, holder.mIconView, user);
			} else {
				holder.mIconView
						.setImageResource(R.drawable.mini_avatar_shadow);

				mXmppServiceBinder.execute(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							final XmppVcard vCard = new XmppVcard(
									mXmppServiceBinder.getServiceContext());
							vCard.load(mXmppServiceBinder.getXmppConnection(),
									user);
							// 加载显示名
							holder.mMsgView.post(new Runnable() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									holder.mMsgView.setText(vCard
											.getDisplayName());
								}
							});

							// 加载用户头像
							YiAsyncImageLoader.loadBitmapFromStore(user,
									new YiImageLoaderListener() {

										@Override
										public void onImageLoaded(String url,
												final Bitmap bitmap) {
											holder.mIconView
													.post(new Runnable() {
														@Override
														public void run() {
															// TODO
															// Auto-generated
															// method
															// stub
															holder.mIconView
																	.setImageBitmap(bitmap);
														}
													});
										}
									});
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				});
			}
			holder.mSubMsgView.setText(cursor.getString(cursor
					.getColumnIndex(ConversationColumns.SUB_MSG)));

			Date msg_date = new Date(cursor.getLong(cursor
					.getColumnIndex(ConversationColumns.MSG_DATE)));
			holder.mDateView.setText(DateUtils.format(mContext, msg_date));
			int dealt = cursor.getInt(cursor
					.getColumnIndex(ConversationColumns.DEALT));

			if (dealt == 0
					&& ConversationType.ENTRY_ADD_REQUEST.toString().equals(
							type)) {
				holder.mActiveView
						.setBackgroundResource(R.drawable.friendactivity_newnotice);
				holder.mActiveView.setText("");
			} else {
				if (ConversationType.CHAT_RECORD.toString().equals(type)
						&& dealt > 0) {
					holder.mActiveView
							.setBackgroundResource(R.drawable.icon_unread);
					holder.mActiveView.setText(String.valueOf(dealt));
				} else {
					holder.mActiveView.setBackgroundDrawable(null);
					holder.mActiveView.setText("");
				}
			}
		}
	}

	private void loadMultiChat(TextView textView, ImageView imageView,
			String roomJid) {
		String owner = UserInfo.getUserInfo(mContext).getUser();

		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(
					MultiChatRoomColumns.CONTENT_URI,
					new String[] { MultiChatRoomColumns.ROOM_NAME,
							MultiChatRoomColumns.ROOM_DESC },
					MultiChatRoomColumns.ROOM_JID + "='" + roomJid + "' and "
							+ MultiChatRoomColumns.OWNER + "='" + owner + "'",
					null, null);
			if (cursor != null && cursor.getCount() == 1) {
				cursor.moveToFirst();
				textView.setText(StringUtils.escapeUserHost(cursor.getString(0)));
				MultiChatDesc multiChatDesc = MultiChatDesc.fromString(cursor
						.getString(1));
				imageView.setImageResource(multiChatDesc.getIcon().getResId());
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub
		View convertView = LayoutInflater.from(mContext).inflate(
				R.layout.main_tab_chats_item, null);
		ViewHolder holder = new ViewHolder();
		holder.mIconView = (ImageView) convertView
				.findViewById(R.id.tab_chats_head);
		holder.mMsgView = (TextView) convertView
				.findViewById(R.id.tab_chats_msg);
		holder.mSubMsgView = (TextView) convertView
				.findViewById(R.id.tab_chats_sub_msg);
		holder.mDateView = (TextView) convertView
				.findViewById(R.id.tab_chats_date);
		holder.mActiveView = (TextView) convertView
				.findViewById(R.id.tab_chats_active);
		convertView.setTag(holder);
		return convertView;
	}

	private class ViewHolder {
		ImageView mIconView;
		TextView mMsgView;
		TextView mSubMsgView;
		TextView mDateView;
		TextView mActiveView;
	}
}

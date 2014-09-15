package com.ikantech.yiim.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;
import com.ikantech.yiim.entity.MultiChatDesc;

public class MultiRoomAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public MultiRoomAdapter(Context context, Cursor c) {
		super(context, c, true);
		// TODO Auto-generated constructor stub
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		ViewHolder holder = (ViewHolder) view.getTag();

		String roomJid = cursor.getString(1);
		String roomName = cursor.getString(2);
		String roomDesc = cursor.getString(3);

		MultiChatDesc desc = MultiChatDesc.fromString(roomDesc);

		if (YiUtils.isStringInvalid(roomName)) {
			holder.mMsgView.setText(roomJid.replaceAll("@.+$", ""));
		} else {
			holder.mMsgView.setText(roomName.replaceAll("@.+$", ""));
		}

		holder.mSubMsgView.setText(desc.getDesc());
		holder.mIconView.setImageResource(desc.getIcon().getResId());
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub
		View convertView = mInflater
				.inflate(R.layout.main_tab_chats_item, null);

		ViewHolder holder = new ViewHolder();
		holder.mIconView = (ImageView) convertView
				.findViewById(R.id.tab_chats_head);
		holder.mMsgView = (TextView) convertView
				.findViewById(R.id.tab_chats_msg);
		holder.mSubMsgView = (TextView) convertView
				.findViewById(R.id.tab_chats_sub_msg);
		holder.mDateView = (TextView) convertView
				.findViewById(R.id.tab_chats_date);
		holder.mTimeView = (TextView) convertView
				.findViewById(R.id.tab_chats_active);

		convertView.setTag(holder);

		return convertView;
	}

	private class ViewHolder {
		ImageView mIconView;
		TextView mMsgView;
		TextView mSubMsgView;
		TextView mDateView;
		TextView mTimeView;
	}
}

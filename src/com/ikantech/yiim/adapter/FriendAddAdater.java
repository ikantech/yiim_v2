package com.ikantech.yiim.adapter;

import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ikantech.support.listener.YiImageLoaderListener;
import com.ikantech.support.util.YiAsyncImageLoader;
import com.ikantech.yiim.R;
import com.ikantech.yiim.entity.FriendAddModel;

public class FriendAddAdater extends BaseAdapter {
	private List<FriendAddModel> mDatas;
	private Context mContext;
	private SimpleDateFormat mDateFormat;
	private SimpleDateFormat mTimeFormat;

	public FriendAddAdater(Context context, List<FriendAddModel> datas) {
		// TODO Auto-generated constructor stub
		if (datas == null) {
			throw new NullPointerException("datas non-null");
		}
		mDatas = datas;
		mContext = context;
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		mTimeFormat = new SimpleDateFormat("HH:mm:ss");
	}

	public List<FriendAddModel> getDatas() {
		return mDatas;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mDatas.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		if (position < 0 || (position > mDatas.size() - 1)) {
			return null;
		}
		return mDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (position < 0 || (position > mDatas.size() - 1)) {
			return null;
		}

		ViewHolder holder = null;
		FriendAddModel item = mDatas.get(position);
		if (null == convertView) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.main_tab_chats_item, null);
			holder = new ViewHolder();
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
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.mIconView.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.mini_avatar_shadow));
		final ImageView imageView = holder.mIconView;
		YiAsyncImageLoader.loadBitmapFromStore(item.getUserId(),
				new YiImageLoaderListener() {
					@Override
					public void onImageLoaded(String url, Bitmap bitmap) {
						// TODO Auto-generated method stub
						imageView.setImageBitmap(bitmap);
					}
				});
		holder.mMsgView.setText(item.getMsg().replaceAll("@.+$", ""));
		holder.mSubMsgView.setText(item.getSubMsg());

		// holder.mDateView.setText(mDateFormat.format(item.getDateTime()));
		// holder.mTimeView.setText(mTimeFormat.format(item.getDateTime()));

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

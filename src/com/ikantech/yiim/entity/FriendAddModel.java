package com.ikantech.yiim.entity;

import java.util.Date;

public class FriendAddModel {
	private String mMsg;
	private String mSubMsg;
	private String mName;
	private Date mDateTime;
	private String mUserId;

	public FriendAddModel(String userId) {
		mUserId = userId;
		mMsg = "";
		mSubMsg = "";
		mName = "";
		mDateTime = new Date();
	}

	public String getUserId() {
		return mUserId;
	}

	public void setUserId(String userId) {
		this.mUserId = userId;
	}

	public String getMsg() {
		return mMsg;
	}

	public void setMsg(String mMsg) {
		this.mMsg = mMsg;
	}

	public String getSubMsg() {
		return mSubMsg;
	}

	public void setSubMsg(String mSubMsg) {
		this.mSubMsg = mSubMsg;
	}

	public Date getDateTime() {
		return mDateTime;
	}

	public void setDateTime(Date mDateTime) {
		this.mDateTime = mDateTime;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}
}

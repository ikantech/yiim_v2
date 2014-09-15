package com.ikantech.yiim.entity;

import java.util.Date;

import org.jivesoftware.smack.packet.RosterPacket.ItemType;

public class TabContactsModel {
	private String mMsg;
	private String mUser;
	private String mSubMsg;
	private Date mDateTime;
	private String mRosterType;
	private int mRosterId;
	private String mPresence;

	public TabContactsModel() {
		mMsg = "";
		mSubMsg = "";
		mRosterType = ItemType.none.toString();
		mDateTime = new Date();
		mRosterId = -1;
	}

	public int getRosterId() {
		return mRosterId;
	}

	public void setRosterId(int rosterId) {
		this.mRosterId = rosterId;
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

	public String getUser() {
		return mUser;
	}

	public void setUser(String mUser) {
		this.mUser = mUser;
	}

	public String getRosterType() {
		return mRosterType;
	}

	public void setRosterType(String mRosterType) {
		this.mRosterType = mRosterType;
	}

	public String getPresence() {
		return mPresence;
	}

	public void setPresence(String presence) {
		this.mPresence = presence;
	}
}

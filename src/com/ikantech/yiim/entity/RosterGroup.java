package com.ikantech.yiim.entity;

public class RosterGroup {
	private int mId;
	private String mName;
	private int mEntryCount;
	private int mOnlineCount;

	public RosterGroup() {
		mId = -1;
		mName = "";
		mEntryCount = 0;
		mOnlineCount = 0;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public int getEntryCount() {
		return mEntryCount;
	}

	public void setEntryCount(int entryCount) {
		this.mEntryCount = entryCount;
	}

	public int getOnlineCount() {
		return mOnlineCount;
	}

	public void setOnlineCount(int onlineCount) {
		this.mOnlineCount = onlineCount;
	}

	public void addOnlineCount() {
		mOnlineCount++;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return mName;
	}

}

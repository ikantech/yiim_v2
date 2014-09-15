package com.ikantech.yiim.entity;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smackx.packet.VCard;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.ikantech.support.cache.YiStoreCache;
import com.ikantech.support.util.YiLog;
import com.ikantech.support.util.YiUtils;
import com.ikantech.xmppsupport.util.XmppConnectionUtils;
import com.ikantech.yiim.common.Const;
import com.ikantech.yiim.common.UserInfo;
import com.ikantech.yiim.provider.VCardManager;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.provider.VCardManager.VCardColumns;
import com.ikantech.yiim.util.StringUtils;

public class XmppVcard {
	private Context mContext;

	private long mVcardId;
	private String mUserId;
	private String mNickName;
	private String mPresence;
	private String mCountry;
	private String mProvince;
	private String mAddress;
	private String mGender;
	private String mSign;
	private long mBirthday;
	private long mSecondBirthday;
	private long mOnlineTime;
	private String mRealName;
	private String mBloodGroup;
	private String mPhone;
	private String mOccupation;
	private String mEmail;

	private byte[] mAvatar;

	// 跟账号相关的属性
	private String mOwnUserId;
	private String mMemoName;
	private long mRosterId;
	private long mGroupId;
	private String mRosterType;
	private String mGroupName;

	public XmppVcard(Context context) {
		mContext = context;
		mUserId = null;
		mOwnUserId = null;
		mNickName = null;
		mPresence = null;
		mCountry = null;
		mProvince = null;
		mAddress = null;
		mGender = null;
		mSign = null;
		mBirthday = System.currentTimeMillis();
		mSecondBirthday = System.currentTimeMillis();
		mOnlineTime = 0;
		mRealName = null;
		mBloodGroup = null;
		mPhone = null;
		mOccupation = null;
		mEmail = null;
		mVcardId = -1;
		mAvatar = null;

		mMemoName = null;
		mRosterId = -1;
		mGroupId = -1;
		mRosterType = ItemType.none.toString();
	}

	public void load(Connection connection) {
		try {
			load(connection, UserInfo.getUserInfo(mContext).getUser());
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void load(Connection connection, String user, boolean force) {
		mUserId = StringUtils.escapeUserResource(user);
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(
					VCardColumns.CONTENT_URI,
					new String[] { VCardColumns.NICKNAME,
							VCardColumns.PRESENCE, VCardColumns.COUNTRY,
							VCardColumns.PROVINCE, VCardColumns.ADDRESS,
							VCardColumns.GENDER, VCardColumns.SIGN,
							VCardColumns.BIRTHDAY,
							VCardColumns.BIRTHDAY_SECOND,
							VCardColumns.ONLINE_TIME, VCardColumns.REAL_NAME,
							VCardColumns.BLOOD_GROUP, VCardColumns.PHONE,
							VCardColumns.OCCUPATION, VCardColumns.EMAIL,
							VCardColumns._ID },
					VCardColumns.USERID + "='" + mUserId + "'", null, null);
			boolean flag = force;
			if (connection == null || !connection.isConnected()
					|| !connection.isAuthenticated()) {
				flag = false;
			}
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				if (flag) {
					mContext.getContentResolver().delete(
							ContentUris.withAppendedId(
									VCardManager.VCardColumns.CONTENT_URI,
									cursor.getLong(15)), null, null);
				} else {
					YiLog.getInstance().i("load from database");
					mNickName = cursor.getString(0);
					mPresence = cursor.getString(1);
					mCountry = cursor.getString(2);
					mProvince = cursor.getString(3);
					mAddress = cursor.getString(4);
					mGender = cursor.getString(5);
					mSign = cursor.getString(6);
					mBirthday = cursor.getLong(7);
					mSecondBirthday = cursor.getLong(8);
					mOnlineTime = cursor.getLong(9);
					mRealName = cursor.getString(10);
					mBloodGroup = cursor.getString(11);
					mPhone = cursor.getString(12);
					mOccupation = cursor.getString(13);
					mEmail = cursor.getString(14);
					mVcardId = cursor.getLong(15);
				}
			} else {
				flag = true;
			}

			if (flag) {
				YiLog.getInstance().i("load from vcard");
				loadByVcard(mContext, connection, mUserId, this);
				saveToDatabase();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}

		Cursor rosterCursor = null;
		try {
			mOwnUserId = UserInfo.getUserInfo(mContext).getUser();
			rosterCursor = mContext.getContentResolver()
					.query(RosterColumns.CONTENT_URI,
							new String[] { RosterColumns._ID,
									RosterColumns.MEMO_NAME,
									RosterColumns.ROSTER_TYPE,
									RosterColumns.GROUP_ID },
							String.format("%s='%s' and %s='%s'",
									RosterColumns.USERID, mUserId,
									RosterColumns.OWNER, mOwnUserId), null,
							null);
			if (rosterCursor != null && rosterCursor.getCount() == 1) {
				rosterCursor.moveToFirst();
				mRosterId = rosterCursor.getLong(0);
				mMemoName = rosterCursor.getString(1);
				mRosterType = rosterCursor.getString(2);
				mGroupId = rosterCursor.getLong(3);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rosterCursor != null) {
				rosterCursor.close();
				rosterCursor = null;
			}
		}

		Cursor groupCursor = null;
		try {
			if (mGroupId < 0) {
				return;
			}
			groupCursor = mContext.getContentResolver().query(
					ContentUris.withAppendedId(RosterGroupColumns.CONTENT_URI,
							mGroupId),
					new String[] { RosterGroupColumns.NAME }, null, null, null);
			if (groupCursor != null && groupCursor.getCount() == 1) {
				groupCursor.moveToFirst();
				mGroupName = groupCursor.getString(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
				groupCursor = null;
			}
		}
	}

	public void load(Connection connection, String user) {
		load(connection, user, false);
	}

	public static XmppVcard loadByVcard(Context context, Connection connection,
			String user, XmppVcard xmppVcard) {
		XmppVcard ret = null;
		if (xmppVcard != null) {
			ret = xmppVcard;
		} else {
			ret = new XmppVcard(context);
		}
		try {
			VCard vCard = new VCard();
			vCard.load(connection, user);

			Roster roster = connection.getRoster();

			ret.setNickName(vCard.getNickName());

			// 在线状态获取
			Presence presence = roster.getPresence(user);
			if (presence.getType() != Type.unavailable) {
				ret.setPresence("online");
			} else {
				ret.setPresence("unavailable");
			}

			// 加载用户头像
			byte[] avatar = vCard.getAvatar();
			if (avatar != null) {
				YiStoreCache.cacheRawData(user, avatar);
			}

			ret.setGender(vCard.getField(Const.SEX));
			ret.setSign(vCard.getField(Const.SIGN));
			ret.setCountry(vCard.getField(Const.COUNTRY));
			ret.setProvince(vCard.getField(Const.PROVINCE));
			ret.setAddress(vCard.getField(Const.ADDRESS));
			ret.setBirthday(Long.valueOf(vCard.getField(Const.BIRTHDAY)));
			ret.setSecondBirthday(Long.valueOf(vCard
					.getField(Const.SECOND_BIRTHDAY)));
			ret.setOnlineTime(Long.valueOf(vCard.getField(Const.ONLINETIME)));
			ret.setRealName(vCard.getField(Const.REALNAME));
			ret.setBloodGroup(vCard.getField(Const.BLOOD_GROUP));
			ret.setPhone(vCard.getField(Const.PHONE));
			ret.setOccupation(vCard.getField(Const.OCCUPATION));
			ret.setEmail(vCard.getField(Const.EMAIL));

			//通知重新加载好友列表
			Intent intent = new Intent(Const.NOTIFY_RELOAD_ROSTER_ENTRIES);
			context.sendBroadcast(intent);
		} catch (Exception e) {
		}

		return ret;
	}

	public void save(Connection connection) {
		try {
			VCard vCard = new VCard();
			vCard.load(connection, mUserId);

			if (!YiUtils.isStringInvalid(mNickName)) {
				vCard.setNickName(mNickName);
			}

			if (!YiUtils.isStringInvalid(mGender)) {
				vCard.setField(Const.SEX, mGender);
			} else {
				vCard.setField(Const.SEX, Const.FEMALE);
			}

			if (!YiUtils.isStringInvalid(mSign)) {
				vCard.setField(Const.SIGN, mSign);
			}

			if (!YiUtils.isStringInvalid(mCountry)) {
				vCard.setField(Const.COUNTRY, mCountry);
			}

			if (!YiUtils.isStringInvalid(mProvince)) {
				vCard.setField(Const.PROVINCE, mProvince);
			}

			if (!YiUtils.isStringInvalid(mAddress)) {
				vCard.setField(Const.ADDRESS, mAddress);
			}

			vCard.setField(Const.BIRTHDAY, String.valueOf(mBirthday));
			vCard.setField(Const.SECOND_BIRTHDAY,
					String.valueOf(mSecondBirthday));
			vCard.setField(Const.ONLINETIME, String.valueOf(mOnlineTime));

			if (!YiUtils.isStringInvalid(mRealName)) {
				vCard.setField(Const.REALNAME, mRealName);
			}

			if (!YiUtils.isStringInvalid(mBloodGroup)) {
				vCard.setField(Const.BLOOD_GROUP, mBloodGroup);
			}

			if (!YiUtils.isStringInvalid(mPhone)) {
				vCard.setField(Const.PHONE, mPhone);
			}

			if (!YiUtils.isStringInvalid(mOccupation)) {
				vCard.setField(Const.OCCUPATION, mOccupation);
			}

			if (!YiUtils.isStringInvalid(mEmail)) {
				vCard.setField(Const.EMAIL, mEmail);
			}

			if (mAvatar != null && mAvatar.length > 0) {
				vCard.setAvatar(mAvatar);
				YiStoreCache.cacheRawData(mUserId, mAvatar);
			}

			vCard.save(connection);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		saveToDatabase();
	}

	protected void saveToDatabase() {
		try {
			ContentValues values = new ContentValues();
			if (!YiUtils.isStringInvalid(mUserId)) {
				values.put(VCardColumns.USERID, mUserId);
			}
			if (!YiUtils.isStringInvalid(mNickName)) {
				values.put(VCardColumns.NICKNAME, mNickName);
			}
			if (!YiUtils.isStringInvalid(mPresence)) {
				values.put(VCardColumns.PRESENCE, mPresence);
			}
			if (!YiUtils.isStringInvalid(mCountry)) {
				values.put(VCardColumns.COUNTRY, mCountry);
			}
			if (!YiUtils.isStringInvalid(mProvince)) {
				values.put(VCardColumns.PROVINCE, mProvince);
			}

			if (!YiUtils.isStringInvalid(mAddress)) {
				values.put(VCardColumns.ADDRESS, mAddress);
			}
			if (!YiUtils.isStringInvalid(mGender)) {
				values.put(VCardColumns.GENDER, mGender);
			}
			if (!YiUtils.isStringInvalid(mSign)) {
				values.put(VCardColumns.SIGN, mSign);
			}

			values.put(VCardColumns.BIRTHDAY, mBirthday);
			values.put(VCardColumns.BIRTHDAY_SECOND, mSecondBirthday);
			values.put(VCardColumns.ONLINE_TIME, mOnlineTime);

			if (!YiUtils.isStringInvalid(mRealName)) {
				values.put(VCardColumns.REAL_NAME, mRealName);
			}
			if (!YiUtils.isStringInvalid(mBloodGroup)) {
				values.put(VCardColumns.BLOOD_GROUP, mBloodGroup);
			}
			if (!YiUtils.isStringInvalid(mPhone)) {
				values.put(VCardColumns.PHONE, mPhone);
			}
			if (!YiUtils.isStringInvalid(mOccupation)) {
				values.put(VCardColumns.OCCUPATION, mOccupation);
			}
			if (!YiUtils.isStringInvalid(mEmail)) {
				values.put(VCardColumns.EMAIL, mEmail);
			}

			if (mVcardId != -1) {
				mContext.getContentResolver().update(
						ContentUris.withAppendedId(VCardColumns.CONTENT_URI,
								mVcardId), values, null, null);
			} else {
				Uri uri = mContext.getContentResolver().insert(
						VCardColumns.CONTENT_URI, values);
				mVcardId = ContentUris.parseId(uri);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void updatePresence() {
		try {
			Connection connection = XmppConnectionUtils.getInstance()
					.getConnection();
			if (connection != null && connection.isConnected()
					&& connection.isAuthenticated()) {
				// 在线状态获取
				Presence presence = connection.getRoster().getPresence(mUserId);
				if (presence.getType() != Type.unavailable) {
					setPresence("online");
				} else {
					setPresence("unavailable");
				}
				saveToDatabase();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public long getVcardId() {
		return mVcardId;
	}

	public void setVcardId(long vcardId) {
		this.mVcardId = vcardId;
	}

	public byte[] getAvatar() {
		return mAvatar;
	}

	public void setAvatar(byte[] avatar) {
		this.mAvatar = avatar;
	}

	public String getDisplayName() {
		if (!YiUtils.isStringInvalid(mMemoName)) {
			return mMemoName;
		} else if (!YiUtils.isStringInvalid(mNickName)) {
			return mNickName;
		} else {
			return StringUtils.escapeUserHost(mUserId);
		}
	}

	public String getUserId() {
		return mUserId;
	}

	public void setUserId(String userId) {
		this.mUserId = userId;
	}

	public String getNickName() {
		return mNickName;
	}

	public void setNickName(String nickName) {
		this.mNickName = nickName;
	}

	public String getPresence() {
		return mPresence;
	}

	public void setPresence(String presence) {
		this.mPresence = presence;
	}

	public String getCountry() {
		return mCountry;
	}

	public void setCountry(String country) {
		this.mCountry = country;
	}

	public String getProvince() {
		return mProvince;
	}

	public void setProvince(String province) {
		this.mProvince = province;
	}

	public String getAddress() {
		return mAddress;
	}

	public void setAddress(String address) {
		this.mAddress = address;
	}

	public String getGender() {
		return mGender;
	}

	public void setGender(String gender) {
		this.mGender = gender;
	}

	public String getSign() {
		return mSign;
	}

	public void setSign(String sign) {
		this.mSign = sign;
	}

	public long getBirthday() {
		return mBirthday;
	}

	public void setBirthday(long birthday) {
		this.mBirthday = birthday;
	}

	public long getSecondBirthday() {
		return mSecondBirthday;
	}

	public void setSecondBirthday(long secondBirthday) {
		this.mSecondBirthday = secondBirthday;
	}

	public long getOnlineTime() {
		return mOnlineTime;
	}

	public void setOnlineTime(long onlineTime) {
		this.mOnlineTime = onlineTime;
	}

	public String getRealName() {
		return mRealName;
	}

	public void setRealName(String realName) {
		this.mRealName = realName;
	}

	public String getBloodGroup() {
		return mBloodGroup;
	}

	public void setBloodGroup(String bloodGroup) {
		this.mBloodGroup = bloodGroup;
	}

	public String getPhone() {
		return mPhone;
	}

	public void setPhone(String phone) {
		this.mPhone = phone;
	}

	public String getOccupation() {
		return mOccupation;
	}

	public void setOccupation(String occupation) {
		this.mOccupation = occupation;
	}

	public String getEmail() {
		return mEmail;
	}

	public void setEmail(String email) {
		this.mEmail = email;
	}

	public String getMemoName() {
		return mMemoName;
	}

	public void setMemoName(String memoName) {
		this.mMemoName = memoName;
	}

	public long getRosterId() {
		return mRosterId;
	}

	public void setRosterId(long rosterId) {
		this.mRosterId = rosterId;
	}

	public long getGroupId() {
		return mGroupId;
	}

	public void setGroupId(long groupId) {
		this.mGroupId = groupId;
	}

	public String getRosterType() {
		return mRosterType;
	}

	public void setRosterType(String rosterType) {
		this.mRosterType = rosterType;
	}

	public String getGroupName() {
		return mGroupName;
	}

	public void setGroupName(String groupName) {
		this.mGroupName = groupName;
	}
}

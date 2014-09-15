package com.ikantech.yiim.provider;

import java.util.HashMap;

import org.jivesoftware.smack.packet.Presence;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.text.TextUtils;

import com.ikantech.yiim.common.Const;

public class VCardManager  implements AbsManager {
	public static final String TABLE_NAME = "a05";

	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class VCardColumns implements BaseColumns {
		private VCardColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chyitech.yiim."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chyitech.yiim."
				+ TABLE_NAME;

		// 用户ID
		public static final String USERID = TABLE_NAME + "01";
		// 昵称
		public static final String NICKNAME = TABLE_NAME + "02";
		// 在线状态
		public static final String PRESENCE = TABLE_NAME + "03";
		// 国家
		public static final String COUNTRY = TABLE_NAME + "04";
		// 省（州）
		public static final String PROVINCE = TABLE_NAME + "05";
		// 地址
		public static final String ADDRESS = TABLE_NAME + "06";
		// 性别
		public static final String GENDER = TABLE_NAME + "07";
		// 个性签名
		public static final String SIGN = TABLE_NAME + "08";
		// 出生年月
		public static final String BIRTHDAY = TABLE_NAME + "09";
		// 保留出生年月
		public static final String BIRTHDAY_SECOND = TABLE_NAME + "10";
		// 在线时长
		public static final String ONLINE_TIME = TABLE_NAME + "11";
		// 真实姓名
		public static final String REAL_NAME = TABLE_NAME + "12";
		// 血型
		public static final String BLOOD_GROUP = TABLE_NAME + "13";
		// 手机号
		public static final String PHONE = TABLE_NAME + "14";
		// 职业
		public static final String OCCUPATION = TABLE_NAME + "15";
		// Email
		public static final String EMAIL = TABLE_NAME + "16";

		public static final String DEFAULT_SORT_ORDER = USERID + " ASC";
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(VCardColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(VCardColumns.USERID + " TEXT,");
		builder.append(VCardColumns.NICKNAME + " TEXT,");
		builder.append(VCardColumns.PRESENCE + " TEXT,");
		builder.append(VCardColumns.COUNTRY + " TEXT,");
		builder.append(VCardColumns.PROVINCE + " TEXT,");
		builder.append(VCardColumns.ADDRESS + " TEXT,");
		builder.append(VCardColumns.GENDER + " TEXT,");
		builder.append(VCardColumns.SIGN + " TEXT,");
		builder.append(VCardColumns.BIRTHDAY + " INTEGER,");
		builder.append(VCardColumns.BIRTHDAY_SECOND + " INTEGER,");
		builder.append(VCardColumns.ONLINE_TIME + " INTEGER,");
		builder.append(VCardColumns.REAL_NAME + " TEXT,");
		builder.append(VCardColumns.BLOOD_GROUP + " TEXT,");
		builder.append(VCardColumns.PHONE + " TEXT,");
		builder.append(VCardColumns.OCCUPATION + " TEXT,");
		builder.append(VCardColumns.EMAIL + " TEXT");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(VCardColumns._ID, VCardColumns._ID);
		mProjectionMap.put(VCardColumns.USERID, VCardColumns.USERID);
		mProjectionMap.put(VCardColumns.NICKNAME, VCardColumns.NICKNAME);
		mProjectionMap.put(VCardColumns.PRESENCE, VCardColumns.PRESENCE);
		mProjectionMap.put(VCardColumns.COUNTRY, VCardColumns.COUNTRY);
		mProjectionMap
				.put(VCardColumns.PROVINCE, VCardColumns.PROVINCE);
		mProjectionMap.put(VCardColumns.ADDRESS, VCardColumns.ADDRESS);
		mProjectionMap.put(VCardColumns.GENDER, VCardColumns.GENDER);
		mProjectionMap.put(VCardColumns.SIGN, VCardColumns.SIGN);
		mProjectionMap.put(VCardColumns.BIRTHDAY, VCardColumns.BIRTHDAY);
		mProjectionMap.put(VCardColumns.BIRTHDAY_SECOND,
				VCardColumns.BIRTHDAY_SECOND);
		mProjectionMap
				.put(VCardColumns.ONLINE_TIME, VCardColumns.ONLINE_TIME);
		mProjectionMap.put(VCardColumns.REAL_NAME, VCardColumns.REAL_NAME);
		mProjectionMap
				.put(VCardColumns.BLOOD_GROUP, VCardColumns.BLOOD_GROUP);
		mProjectionMap.put(VCardColumns.PHONE, VCardColumns.PHONE);
		mProjectionMap.put(VCardColumns.OCCUPATION, VCardColumns.OCCUPATION);
		mProjectionMap.put(VCardColumns.EMAIL, VCardColumns.EMAIL);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, VCardColumns._ID
				+ " AS " + LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME, VCardColumns.USERID
				+ " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.VCARD.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.VCARD_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(
					TABLE_NAME,
					values,
					VCardColumns._ID
							+ "="
							+ noteId
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return count;
	}

	// 删除数据
	public int delete(SQLiteDatabase db, int type, Uri uri, String where,
			String[] whereArgs) {
		int count;
		if (type == UriType.VCARD.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.VCARD_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(
					TABLE_NAME,
					VCardColumns._ID
							+ "="
							+ noteId
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return count;
	}

	// 插入数据
	public Uri insert(SQLiteDatabase db, int type, Uri uri,
			ContentValues initialValues) {

		if (type != UriType.VCARD.getCode()) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

//		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set if
		if (values.containsKey(VCardColumns.USERID) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", userid should be point.");
		}

		if (values.containsKey(VCardColumns.NICKNAME) == false) {
			values.put(VCardColumns.NICKNAME, "");
		}

		if (values.containsKey(VCardColumns.COUNTRY) == false) {
			values.put(VCardColumns.COUNTRY, "");
		}

		if (values.containsKey(VCardColumns.PRESENCE) == false) {
			values.put(VCardColumns.PRESENCE, Presence.Type.unavailable.name());
		}

		if (values.containsKey(VCardColumns.PROVINCE) == false) {
			values.put(VCardColumns.PROVINCE, "");
		}

		if (values.containsKey(VCardColumns.ADDRESS) == false) {
			values.put(VCardColumns.ADDRESS, "");
		}

		if (values.containsKey(VCardColumns.GENDER) == false) {
			values.put(VCardColumns.GENDER, Const.FEMALE);
		}

		if (values.containsKey(VCardColumns.SIGN) == false) {
			values.put(VCardColumns.SIGN, "");
		}

		if (values.containsKey(VCardColumns.BIRTHDAY) == false) {
			values.put(VCardColumns.BIRTHDAY, -1);
		}

		if (values.containsKey(VCardColumns.BIRTHDAY_SECOND) == false) {
			values.put(VCardColumns.BIRTHDAY_SECOND, -1);
		}

		if (values.containsKey(VCardColumns.ONLINE_TIME) == false) {
			values.put(VCardColumns.ONLINE_TIME, 0);
		}

		if (values.containsKey(VCardColumns.REAL_NAME) == false) {
			values.put(VCardColumns.REAL_NAME, "");
		}

		if (values.containsKey(VCardColumns.BLOOD_GROUP) == false) {
			values.put(VCardColumns.BLOOD_GROUP, "");
		}

		if (values.containsKey(VCardColumns.PHONE) == false) {
			values.put(VCardColumns.PHONE, "");
		}

		if (values.containsKey(VCardColumns.OCCUPATION) == false) {
			values.put(VCardColumns.OCCUPATION, "");
		}

		if (values.containsKey(VCardColumns.EMAIL) == false) {
			values.put(VCardColumns.EMAIL, "");
		}

		long rowId = db.insert(TABLE_NAME, VCardColumns.ADDRESS, values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(VCardColumns.CONTENT_URI,
					rowId);
			return ret;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	// 查询数据
	public Cursor query(SQLiteDatabase db, int type, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_NAME);

		if (type == UriType.VCARD.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.VCARD_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(VCardColumns._ID + "="
					+ uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_VCARD.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = VCardColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		return c;
	}
}

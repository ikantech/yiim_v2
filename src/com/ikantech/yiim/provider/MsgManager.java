package com.ikantech.yiim.provider;

import java.util.HashMap;

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

public class MsgManager implements AbsManager {
	public static final String TABLE_NAME = "a04";
	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class MsgColumns implements BaseColumns {
		private MsgColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chyitech.yiim."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chyitech.yiim."
				+ TABLE_NAME;

		// 发送者
		public static final String SENDER = TABLE_NAME + "01";
		// 接收者
		public static final String RECEIVER = TABLE_NAME + "02";
		// 创建时间
		public static final String CREATE_DATE = TABLE_NAME + "03";
		// 内容
		public static final String CONTENT = TABLE_NAME + "04";

		// 本地时间
		public static final String LOCAL_DATE = TABLE_NAME + "05";

		public static final String DEFAULT_SORT_ORDER = LOCAL_DATE + " ASC";
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(MsgColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(MsgColumns.SENDER + " TEXT,");
		builder.append(MsgColumns.RECEIVER + " TEXT,");
		builder.append(MsgColumns.CONTENT + " TEXT,");
		builder.append(MsgColumns.CREATE_DATE + " INTEGER,");
		builder.append(MsgColumns.LOCAL_DATE + " INTEGER");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(MsgColumns._ID, MsgColumns._ID);
		mProjectionMap.put(MsgColumns.SENDER, MsgColumns.SENDER);
		mProjectionMap.put(MsgColumns.RECEIVER, MsgColumns.RECEIVER);
		mProjectionMap.put(MsgColumns.CONTENT, MsgColumns.CONTENT);
		mProjectionMap.put(MsgColumns.CREATE_DATE, MsgColumns.CREATE_DATE);
		mProjectionMap.put(MsgColumns.LOCAL_DATE, MsgColumns.LOCAL_DATE);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, MsgColumns._ID + " AS "
				+ LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME, MsgColumns.CONTENT
				+ " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.MSG.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.MSG_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(TABLE_NAME, values,
					MsgColumns._ID
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
		if (type == UriType.MSG.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.MSG_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_NAME,
					MsgColumns._ID
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

		if (type != UriType.MSG.getCode()) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set if
		if (values.containsKey(MsgColumns.CREATE_DATE) == false) {
			values.put(MsgColumns.CREATE_DATE, now);
		}

		if (values.containsKey(MsgColumns.CONTENT) == false) {
			values.put(MsgColumns.CONTENT, "");
		}

		if (values.containsKey(MsgColumns.SENDER) == false) {
			values.put(MsgColumns.SENDER, "");
		}

		if (values.containsKey(MsgColumns.RECEIVER) == false) {
			values.put(MsgColumns.RECEIVER, 1);
		}
		
		values.put(MsgColumns.LOCAL_DATE, now);

		long rowId = db.insert(TABLE_NAME, MsgColumns.CONTENT, values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(MsgColumns.CONTENT_URI, rowId);
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

		if (type == UriType.MSG.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.MSG_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(MsgColumns._ID + "=" + uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_MSG.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = MsgColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy, uri.getQueryParameter("limit"));

		return c;
	}
}

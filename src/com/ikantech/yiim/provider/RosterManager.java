package com.ikantech.yiim.provider;

import java.util.HashMap;

import org.jivesoftware.smack.packet.RosterPacket.ItemType;

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

public class RosterManager implements AbsManager {
	public static final String TABLE_NAME = "a03";

	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class RosterColumns implements BaseColumns {
		private RosterColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chyitech.yiim."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chyitech.yiim."
				+ TABLE_NAME;

		// 用户ID
		public static final String USERID = TABLE_NAME + "01";
		// 备注名称
		public static final String MEMO_NAME = TABLE_NAME + "02";
		// 好友关系
		public static final String ROSTER_TYPE = TABLE_NAME + "03";
		// 所在分组
		public static final String GROUP_ID = TABLE_NAME + "04";
		//拥有者
		public static final String OWNER = TABLE_NAME + "05";

		public static final String DEFAULT_SORT_ORDER = USERID + " ASC";
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(RosterColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(RosterColumns.USERID + " TEXT,");
		builder.append(RosterColumns.MEMO_NAME + " TEXT,");
		builder.append(RosterColumns.ROSTER_TYPE + " TEXT,");
		builder.append(RosterColumns.OWNER + " TEXT,");
		builder.append(RosterColumns.GROUP_ID + " INTEGER");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(RosterColumns._ID, RosterColumns._ID);
		mProjectionMap.put(RosterColumns.USERID, RosterColumns.USERID);
		mProjectionMap.put(RosterColumns.MEMO_NAME, RosterColumns.MEMO_NAME);
		mProjectionMap
				.put(RosterColumns.ROSTER_TYPE, RosterColumns.ROSTER_TYPE);
		mProjectionMap.put(RosterColumns.GROUP_ID, RosterColumns.GROUP_ID);
		mProjectionMap.put(RosterColumns.OWNER, RosterColumns.OWNER);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, RosterColumns._ID
				+ " AS " + LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME, RosterColumns.USERID
				+ " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.ROSTER.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.ROSTER_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(
					TABLE_NAME,
					values,
					RosterColumns._ID
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
		if (type == UriType.ROSTER.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.ROSTER_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(
					TABLE_NAME,
					RosterColumns._ID
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

		if (type != UriType.ROSTER.getCode()) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		// Make sure that the fields are all set if
		if (values.containsKey(RosterColumns.USERID) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", userid should be point.");
		}
		
		if (values.containsKey(RosterColumns.OWNER) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", owner should be point.");
		}

		if (values.containsKey(RosterColumns.MEMO_NAME) == false) {
			values.put(RosterColumns.MEMO_NAME, "");
		}

		if (values.containsKey(RosterColumns.ROSTER_TYPE) == false) {
			values.put(RosterColumns.ROSTER_TYPE, ItemType.none.toString());
		}

		if (values.containsKey(RosterColumns.GROUP_ID) == false) {
			values.put(RosterColumns.GROUP_ID, -1);
		}

		long rowId = db.insert(TABLE_NAME, RosterColumns.MEMO_NAME, values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(RosterColumns.CONTENT_URI,
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

		if (type == UriType.ROSTER.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.ROSTER_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(RosterColumns._ID + "="
					+ uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_ROSTER.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = RosterColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		return c;
	}
}

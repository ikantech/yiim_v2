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

public class RosterGroupManager implements AbsManager {
	public static final String TABLE_NAME = "a02";
	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class RosterGroupColumns implements BaseColumns {
		private RosterGroupColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chyitech.yiim."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chyitech.yiim."
				+ TABLE_NAME;

		// 名称
		public static final String NAME = TABLE_NAME + "01";
		public static final String OWNER = TABLE_NAME + "02";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(RosterGroupColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(RosterGroupColumns.NAME + " TEXT,");
		builder.append(RosterGroupColumns.OWNER + " TEXT");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(RosterGroupColumns._ID, RosterGroupColumns._ID);
		mProjectionMap.put(RosterGroupColumns.NAME, RosterGroupColumns.NAME);
		mProjectionMap.put(RosterGroupColumns.OWNER, RosterGroupColumns.OWNER);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, RosterGroupColumns._ID
				+ " AS " + LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME, RosterGroupColumns.NAME
				+ " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.ROSTER_GROUP.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.ROSTER_GROUP_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(
					TABLE_NAME,
					values,
					RosterGroupColumns._ID
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
		if (type == UriType.ROSTER_GROUP.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.ROSTER_GROUP_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(
					TABLE_NAME,
					RosterGroupColumns._ID
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

		if (type != UriType.ROSTER_GROUP.getCode()) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		// Make sure that the fields are all set if
		if (values.containsKey(RosterGroupColumns.NAME) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", name should be point.");
		}
		
		if (values.containsKey(RosterGroupColumns.OWNER) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", owner should be point.");
		}

		long rowId = db.insert(TABLE_NAME, null, values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(
					RosterGroupColumns.CONTENT_URI, rowId);
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

		if (type == UriType.ROSTER_GROUP.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.ROSTER_GROUP_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(RosterGroupColumns._ID + "="
					+ uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_ROSTER_GROUP.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = RosterGroupColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		return c;
	}
}

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

public class MultiChatRoomManager implements AbsManager {
	public static final String TABLE_NAME = "a06";

	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class MultiChatRoomColumns implements BaseColumns {
		private MultiChatRoomColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.chyitech.yiim."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.chyitech.yiim."
				+ TABLE_NAME;

		// 房间JID
		public static final String ROOM_JID = TABLE_NAME + "01";
		// 房间名称
		public static final String ROOM_NAME = TABLE_NAME + "02";
		// 房间描述
		public static final String ROOM_DESC = TABLE_NAME + "03";
		// 拥有者
		public static final String OWNER = TABLE_NAME + "04";
		// 最后一条消息接收时间
		public static final String LAST_MSG_TIME = TABLE_NAME + "05";

		public static final String DEFAULT_SORT_ORDER = ROOM_JID + " ASC";
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(MultiChatRoomColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(MultiChatRoomColumns.ROOM_JID + " TEXT,");
		builder.append(MultiChatRoomColumns.ROOM_NAME + " TEXT,");
		builder.append(MultiChatRoomColumns.ROOM_DESC + " TEXT,");
		builder.append(MultiChatRoomColumns.OWNER + " TEXT,");
		builder.append(MultiChatRoomColumns.LAST_MSG_TIME + " INTEGER");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(MultiChatRoomColumns._ID, MultiChatRoomColumns._ID);
		mProjectionMap.put(MultiChatRoomColumns.ROOM_JID,
				MultiChatRoomColumns.ROOM_JID);
		mProjectionMap.put(MultiChatRoomColumns.ROOM_NAME,
				MultiChatRoomColumns.ROOM_NAME);
		mProjectionMap.put(MultiChatRoomColumns.ROOM_DESC,
				MultiChatRoomColumns.ROOM_DESC);
		mProjectionMap.put(MultiChatRoomColumns.OWNER,
				MultiChatRoomColumns.OWNER);
		mProjectionMap.put(MultiChatRoomColumns.LAST_MSG_TIME,
				MultiChatRoomColumns.LAST_MSG_TIME);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, MultiChatRoomColumns._ID
				+ " AS " + LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME,
				MultiChatRoomColumns.ROOM_JID + " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.MULTI_ROOM.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.MULTI_ROOM_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(
					TABLE_NAME,
					values,
					MultiChatRoomColumns._ID
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
		if (type == UriType.MULTI_ROOM.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.MULTI_ROOM_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(
					TABLE_NAME,
					MultiChatRoomColumns._ID
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

		if (type != UriType.MULTI_ROOM.getCode()) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		// Make sure that the fields are all set if
		if (values.containsKey(MultiChatRoomColumns.ROOM_JID) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", room jid should be point.");
		}

		if (values.containsKey(MultiChatRoomColumns.OWNER) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", owner should be point.");
		}

		if (values.containsKey(MultiChatRoomColumns.ROOM_NAME) == false) {
			values.put(MultiChatRoomColumns.ROOM_NAME, "");
		}

		if (values.containsKey(MultiChatRoomColumns.ROOM_DESC) == false) {
			values.put(MultiChatRoomColumns.ROOM_DESC, "");
		}

		if (values.containsKey(MultiChatRoomColumns.LAST_MSG_TIME) == false) {
			values.put(MultiChatRoomColumns.LAST_MSG_TIME,
					System.currentTimeMillis());
		}

		long rowId = db.insert(TABLE_NAME, MultiChatRoomColumns.ROOM_NAME,
				values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(
					MultiChatRoomColumns.CONTENT_URI, rowId);
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

		if (type == UriType.MULTI_ROOM.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.MULTI_ROOM_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(MultiChatRoomColumns._ID + "="
					+ uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_MULTI_ROOM.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = MultiChatRoomColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		return c;
	}
}

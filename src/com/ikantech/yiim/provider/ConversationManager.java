package com.ikantech.yiim.provider;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.text.TextUtils;

public class ConversationManager implements AbsManager {
	public static final String TABLE_NAME = "a01";
	public static final String CREATE_SQL;

	private static HashMap<String, String> mProjectionMap;
	private static HashMap<String, String> mLiveFolderProjectionMap;

	public static final class ConversationColumns implements BaseColumns {
		private ConversationColumns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XmppProvider.AUTHORITY + "/" + TABLE_NAME);

		static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ikantech.xmppsupport."
				+ TABLE_NAME;
		static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ikantech.xmppsupport."
				+ TABLE_NAME;

		// 会话属主
		public static final String USER = TABLE_NAME + "01";
		// 会话内容
		public static final String MSG = TABLE_NAME + "02";
		// 会话子内容
		public static final String SUB_MSG = TABLE_NAME + "03";
		// 会话类型
		public static final String MSG_TYPE = TABLE_NAME + "04";
		// 会话是否已经处理
		public static final String DEALT = TABLE_NAME + "05";
		// 会话时间，创建时间、最后一条聊天内容的时间
		public static final String MSG_DATE = TABLE_NAME + "06";
		// 会话修改时间，用于排序
		public static final String MODIFIED_DATE = TABLE_NAME + "07";

		public static final String DEFAULT_SORT_ORDER = MODIFIED_DATE + " DESC";
	}

	public enum ConversationType {
		ENTRY_ADD_REQUEST(0), CHAT_RECORD(1), MSG(2);
		private int index;

		private ConversationType(int index) {
			this.index = index;
		}

		public int getCode() {
			return index;
		}

		public String toString() {
			return index + "";
		}
	}

	static {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(TABLE_NAME);
		builder.append(" (");
		builder.append(ConversationColumns._ID + " INTEGER PRIMARY KEY,");
		builder.append(ConversationColumns.USER + " TEXT,");
		builder.append(ConversationColumns.MSG + " TEXT,");
		builder.append(ConversationColumns.SUB_MSG + " TEXT,");
		builder.append(ConversationColumns.MSG_TYPE + " INTEGER,");
		builder.append(ConversationColumns.DEALT + " INTEGER,");
		builder.append(ConversationColumns.MSG_DATE + " INTEGER,");
		builder.append(ConversationColumns.MODIFIED_DATE + " INTEGER");
		builder.append(");");
		CREATE_SQL = builder.toString();

		mProjectionMap = new HashMap<String, String>();
		mProjectionMap.put(ConversationColumns._ID, ConversationColumns._ID);
		mProjectionMap.put(ConversationColumns.USER, ConversationColumns.USER);
		mProjectionMap.put(ConversationColumns.MSG, ConversationColumns.MSG);
		mProjectionMap.put(ConversationColumns.SUB_MSG,
				ConversationColumns.SUB_MSG);
		mProjectionMap.put(ConversationColumns.MSG_TYPE,
				ConversationColumns.MSG_TYPE);
		mProjectionMap
				.put(ConversationColumns.DEALT, ConversationColumns.DEALT);
		mProjectionMap.put(ConversationColumns.MSG_DATE,
				ConversationColumns.MSG_DATE);
		mProjectionMap.put(ConversationColumns.MODIFIED_DATE,
				ConversationColumns.MODIFIED_DATE);

		mLiveFolderProjectionMap = new HashMap<String, String>();
		mLiveFolderProjectionMap.put(LiveFolders._ID, ConversationColumns._ID
				+ " AS " + LiveFolders._ID);
		mLiveFolderProjectionMap.put(LiveFolders.NAME, ConversationColumns.MSG
				+ " AS " + LiveFolders.NAME);
	}

	// 更新数据库
	public int update(SQLiteDatabase db, int type, Uri uri,
			ContentValues values, String where, String[] whereArgs) {
		int count;
		if (type == UriType.CONVERSATION.getCode()) {
			count = db.update(TABLE_NAME, values, where, whereArgs);
		} else if (type == UriType.CONVERSATION_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.update(
					TABLE_NAME,
					values,
					ConversationColumns._ID
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
		if (type == UriType.CONVERSATION.getCode()) {
			count = db.delete(TABLE_NAME, where, whereArgs);
		} else if (type == UriType.CONVERSATION_ID.getCode()) {
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(
					TABLE_NAME,
					ConversationColumns._ID
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

		if (type != UriType.CONVERSATION.getCode()) {
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
		if (values.containsKey(ConversationColumns.MSG_DATE) == false) {
			values.put(ConversationColumns.MSG_DATE, now);
		}

		if (values.containsKey(ConversationColumns.MODIFIED_DATE) == false) {
			values.put(ConversationColumns.MODIFIED_DATE, now);
		}

		if (values.containsKey(ConversationColumns.MSG) == false) {
			Resources r = Resources.getSystem();
			values.put(ConversationColumns.MSG,
					r.getString(android.R.string.untitled));
		}

		if (values.containsKey(ConversationColumns.SUB_MSG) == false) {
			values.put(ConversationColumns.SUB_MSG, "");
		}

		if (values.containsKey(ConversationColumns.USER) == false) {
			throw new SQLException("Failed to insert row into " + uri
					+ ", user should be point.");
		}

		if (values.containsKey(ConversationColumns.DEALT) == false) {
			values.put(ConversationColumns.DEALT, 1);
		}

		if (values.containsKey(ConversationColumns.MSG_TYPE) == false) {
			values.put(ConversationColumns.MSG_TYPE,
					ConversationType.MSG.getCode());
		}

		long rowId = db.insert(TABLE_NAME, ConversationColumns.SUB_MSG, values);
		if (rowId > 0) {
			Uri ret = ContentUris.withAppendedId(
					ConversationColumns.CONTENT_URI, rowId);
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

		if (type == UriType.CONVERSATION.getCode()) {
			qb.setProjectionMap(mProjectionMap);
		} else if (type == UriType.CONVERSATION_ID.getCode()) {
			qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(ConversationColumns._ID + "="
					+ uri.getPathSegments().get(1));
		} else if (type == UriType.LIVE_FOLDER_CONVERSATION.getCode()) {
			qb.setProjectionMap(mLiveFolderProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = ConversationColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		return c;
	}
}

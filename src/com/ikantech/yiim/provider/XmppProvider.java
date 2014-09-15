package com.ikantech.yiim.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.ikantech.yiim.provider.ConversationManager.ConversationColumns;
import com.ikantech.yiim.provider.MsgManager.MsgColumns;
import com.ikantech.yiim.provider.MultiChatRoomManager.MultiChatRoomColumns;
import com.ikantech.yiim.provider.RosterGroupManager.RosterGroupColumns;
import com.ikantech.yiim.provider.RosterManager.RosterColumns;
import com.ikantech.yiim.provider.VCardManager.VCardColumns;

public class XmppProvider extends ContentProvider {
	public static final String AUTHORITY = "com.chyitech.yiim.provider.XmppProvider";
	private static final String DATABASE_NAME = "com_chyitech_yiim.db";
	private static final int DATABASE_VERSION = 1;

	private ConversationManager mConversationManager;
	private RosterGroupManager mRosterGroupManager;
	private RosterManager mRosterManager;
	private MsgManager mMsgManager;
	private VCardManager mVCardManager;
	private MultiChatRoomManager mMultiChatRoomManager;

	private static final UriMatcher mUriMatcher;

	private DatabaseHelper mDatabaseHelper;

	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, ConversationManager.TABLE_NAME,
				UriType.CONVERSATION.getCode());
		mUriMatcher.addURI(AUTHORITY, ConversationManager.TABLE_NAME + "/#",
				UriType.CONVERSATION_ID.getCode());
		mUriMatcher.addURI(AUTHORITY, "live_folders/"
				+ ConversationManager.TABLE_NAME,
				UriType.LIVE_FOLDER_CONVERSATION.getCode());

		mUriMatcher.addURI(AUTHORITY, RosterGroupManager.TABLE_NAME,
				UriType.ROSTER_GROUP.getCode());
		mUriMatcher.addURI(AUTHORITY, RosterGroupManager.TABLE_NAME + "/#",
				UriType.ROSTER_GROUP_ID.getCode());
		mUriMatcher.addURI(AUTHORITY, "live_folders/"
				+ RosterGroupManager.TABLE_NAME,
				UriType.LIVE_FOLDER_ROSTER_GROUP.getCode());

		mUriMatcher.addURI(AUTHORITY, RosterManager.TABLE_NAME,
				UriType.ROSTER.getCode());
		mUriMatcher.addURI(AUTHORITY, RosterManager.TABLE_NAME + "/#",
				UriType.ROSTER_ID.getCode());
		mUriMatcher.addURI(AUTHORITY, "live_folders/"
				+ RosterManager.TABLE_NAME,
				UriType.LIVE_FOLDER_ROSTER.getCode());

		// 添加Msg表的Uri
		mUriMatcher.addURI(AUTHORITY, MsgManager.TABLE_NAME,
				UriType.MSG.getCode());
		mUriMatcher.addURI(AUTHORITY, MsgManager.TABLE_NAME + "/#",
				UriType.MSG_ID.getCode());
		mUriMatcher.addURI(AUTHORITY, "live_folders/" + MsgManager.TABLE_NAME,
				UriType.LIVE_FOLDER_MSG.getCode());

		mUriMatcher.addURI(AUTHORITY, VCardManager.TABLE_NAME,
				UriType.VCARD.getCode());
		mUriMatcher.addURI(AUTHORITY, VCardManager.TABLE_NAME + "/#",
				UriType.VCARD_ID.getCode());
		mUriMatcher.addURI(AUTHORITY,
				"live_folders/" + VCardManager.TABLE_NAME,
				UriType.LIVE_FOLDER_VCARD.getCode());

		mUriMatcher.addURI(AUTHORITY, MultiChatRoomManager.TABLE_NAME,
				UriType.MULTI_ROOM.getCode());
		mUriMatcher.addURI(AUTHORITY, MultiChatRoomManager.TABLE_NAME + "/#",
				UriType.MULTI_ROOM_ID.getCode());
		mUriMatcher.addURI(AUTHORITY, "live_folders/"
				+ MultiChatRoomManager.TABLE_NAME,
				UriType.LIVE_FOLDER_MULTI_ROOM.getCode());
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		mConversationManager = new ConversationManager();
		mRosterGroupManager = new RosterGroupManager();
		mRosterManager = new RosterManager();
		mMsgManager = new MsgManager();
		mVCardManager = new VCardManager();
		mMultiChatRoomManager = new MultiChatRoomManager();
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	protected AbsManager getManager(int code) {
		if (code <= UriType.LIVE_FOLDER_CONVERSATION.getCode()) {
			return mConversationManager;
		} else if (code > UriType.LIVE_FOLDER_CONVERSATION.getCode()
				&& code <= UriType.LIVE_FOLDER_ROSTER_GROUP.getCode()) {
			return mRosterGroupManager;
		} else if (code > UriType.LIVE_FOLDER_ROSTER_GROUP.getCode()
				&& code <= UriType.LIVE_FOLDER_ROSTER.getCode()) {
			return mRosterManager;
		} else if (code > UriType.LIVE_FOLDER_ROSTER.getCode()
				&& code <= UriType.LIVE_FOLDER_MSG.getCode()) {
			return mMsgManager;
		} else if (code > UriType.LIVE_FOLDER_MSG.getCode()
				&& code <= UriType.LIVE_FOLDER_VCARD.getCode()) {
			return mVCardManager;
		} else if (code > UriType.LIVE_FOLDER_VCARD.getCode()
				&& code <= UriType.LIVE_FOLDER_MULTI_ROOM.getCode()) {
			return mMultiChatRoomManager;
		} else {
			return null;
		}
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		int code = mUriMatcher.match(uri);
		if (code == UriType.CONVERSATION.getCode()
				|| code == UriType.LIVE_FOLDER_CONVERSATION.getCode()) {
			return ConversationColumns.CONTENT_TYPE;
		} else if (code == UriType.CONVERSATION_ID.getCode()) {
			return ConversationColumns.CONTENT_ITEM_TYPE;
		} else if (code == UriType.MSG.getCode()
				|| code == UriType.LIVE_FOLDER_MSG.getCode()) {
			return MsgColumns.CONTENT_TYPE;
		} else if (code == UriType.MSG_ID.getCode()) {
			return MsgColumns.CONTENT_ITEM_TYPE;
		} else if (code == UriType.ROSTER_GROUP.getCode()
				|| code == UriType.LIVE_FOLDER_ROSTER_GROUP.getCode()) {
			return RosterGroupColumns.CONTENT_TYPE;
		} else if (code == UriType.ROSTER_GROUP_ID.getCode()) {
			return RosterGroupColumns.CONTENT_ITEM_TYPE;
		} else if (code == UriType.ROSTER.getCode()
				|| code == UriType.LIVE_FOLDER_ROSTER.getCode()) {
			return RosterColumns.CONTENT_TYPE;
		} else if (code == UriType.ROSTER_ID.getCode()) {
			return RosterColumns.CONTENT_ITEM_TYPE;
		} else if (code == UriType.VCARD.getCode()
				|| code == UriType.LIVE_FOLDER_VCARD.getCode()) {
			return VCardColumns.CONTENT_TYPE;
		} else if (code == UriType.VCARD_ID.getCode()) {
			return VCardColumns.CONTENT_ITEM_TYPE;
		} else if (code == UriType.MULTI_ROOM.getCode()
				|| code == UriType.LIVE_FOLDER_MULTI_ROOM.getCode()) {
			return MultiChatRoomColumns.CONTENT_TYPE;
		} else if (code == UriType.MULTI_ROOM_ID.getCode()) {
			return MultiChatRoomColumns.CONTENT_ITEM_TYPE;
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count = 0;
		int code = mUriMatcher.match(uri);
		AbsManager manager = getManager(code);
		if (manager != null) {
			count = manager.delete(db, code, uri, selection, selectionArgs);
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		Uri ret = null;
		int code = mUriMatcher.match(uri);
		AbsManager manager = getManager(code);
		if (manager != null) {
			ret = manager.insert(db, code, uri, values);
		}
		if (ret != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		Cursor cursor = null;

		int type = mUriMatcher.match(uri);
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		AbsManager manager = getManager(type);
		if (manager != null) {
			cursor = manager.query(db, type, uri, projection, selection,
					selectionArgs, sortOrder);
		}

		if (cursor != null) {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count = 0;
		int code = mUriMatcher.match(uri);
		AbsManager manager = getManager(code);
		if (manager != null) {
			count = manager.update(db, code, uri, values, selection,
					selectionArgs);
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	public static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(ConversationManager.CREATE_SQL);
			db.execSQL(RosterGroupManager.CREATE_SQL);
			db.execSQL(RosterManager.CREATE_SQL);
			db.execSQL(MsgManager.CREATE_SQL);
			db.execSQL(VCardManager.CREATE_SQL);
			db.execSQL(MultiChatRoomManager.CREATE_SQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + ConversationManager.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + RosterGroupManager.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + RosterManager.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + MsgManager.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + VCardManager.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS "
					+ MultiChatRoomManager.TABLE_NAME);
			onCreate(db);
		}
	}
}

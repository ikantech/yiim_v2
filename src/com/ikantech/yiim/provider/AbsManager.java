package com.ikantech.yiim.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public interface AbsManager {
	int update(SQLiteDatabase db, int type, Uri uri, ContentValues values,
			String where, String[] whereArgs);

	int delete(SQLiteDatabase db, int type, Uri uri, String where,
			String[] whereArgs);

	Uri insert(SQLiteDatabase db, int type, Uri uri, ContentValues initialValues);

	Cursor query(SQLiteDatabase db, int type, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder);
}

package com.mediatek.HobbyDB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CustomHobbyDB extends SQLiteOpenHelper {

	public CustomHobbyDB(Context context) {
		super(context,CustomHobbyUtils.DB_NAME, null, CustomHobbyUtils.DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(" CREATE TABLE IF NOT EXISTS " + CustomHobbyUtils.TABLE_NAME
				+ " ( " + CustomHobbyUtils.ID + " integer primary key autoincrement , "
						+ CustomHobbyUtils.PARENT_TITLE + " integer , "
				+ CustomHobbyUtils.CONTENT + " integer , "
					+CustomHobbyUtils.LINK + " varchar , "
					+CustomHobbyUtils.NUM + " int , "
				+ CustomHobbyUtils.COMMENT + " varchar );"
				);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS" + CustomHobbyUtils.TABLE_NAME);
		onCreate(db);
	}

}

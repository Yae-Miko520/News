package com.java.zhangbinwei.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.java.zhangbinwei.entity.FavoriteInfo;

import java.util.ArrayList;
import java.util.List;

public class FavoriteDbHelper extends SQLiteOpenHelper {
    private static FavoriteDbHelper sHelper;
    private static final String DB_NAME = "favorite.db";
    private static final int VERSION = 1;

    public FavoriteDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public synchronized static FavoriteDbHelper getInstance(Context context) {
        if (null == sHelper) {
            sHelper = new FavoriteDbHelper(context, DB_NAME, null, VERSION);
        }
        return sHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table favorite_table(favorite_id integer primary key autoincrement, " +
                "uniquekey text," +
                "username text," +
                "new_json text" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}

    public int addFavorite(String username, String uniquekey, String new_json) {
        if (!isFavorite(uniquekey)) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("uniquekey", uniquekey);
            values.put("new_json", new_json);
            return (int) db.insert("favorite_table", null, values);
        }
        return 0;
    }

    public int deleteFavorite(String uniquekey) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("favorite_table", "uniquekey=?", new String[]{uniquekey});
    }

    @SuppressLint("Range")
    public boolean isFavorite(String uniquekey) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select favorite_id from favorite_table where uniquekey=?";
        Cursor cursor = db.rawQuery(sql, new String[]{uniquekey});
        boolean result = cursor.moveToNext();
        cursor.close();
        return result;
    }

    @SuppressLint("Range")
    public List<FavoriteInfo> queryFavoriteListData(String username) {
        SQLiteDatabase db = getReadableDatabase();
        List<FavoriteInfo> list = new ArrayList<>();
        String sql = "select favorite_id, uniquekey, username, new_json from favorite_table ORDER BY favorite_id DESC";
        Cursor cursor = db.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            int favorite_id = cursor.getInt(cursor.getColumnIndex("favorite_id"));
            String uniquekey = cursor.getString(cursor.getColumnIndex("uniquekey"));
            String userName = cursor.getString(cursor.getColumnIndex("username"));
            String new_json = cursor.getString(cursor.getColumnIndex("new_json"));
            list.add(new FavoriteInfo(favorite_id, uniquekey, userName, new_json));
        }
        cursor.close();
        return list;
    }
}
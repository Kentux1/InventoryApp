package com.kentux.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import com.kentux.inventoryapp.data.ProductContract.ProductEntry;
/**
 * Created by Tiago Gomes on 14/07/2017.
 */

public class ProductDbHelper extends SQLiteOpenHelper {

    public static final String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductEntry.TABLE_NAME + " ("
            + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
            + ProductEntry.COLUMN_PRODUCT_STOCK + " INTEGER NOT NULL DEFAULT 0, "
            + ProductEntry.COLUMN_PRODUCT_PRICE + " REAL NOT NULL, "
            + ProductEntry.COLUMN_PRODUCT_SUPPLIER + " TEXT NOT NULL DEFAULT UNKNOWN, "
            + ProductEntry.COLUMN_PRODUCT_SALES + " REAL NOT NULL DEFAULT 0.0, "
            + ProductEntry.COLUMN_PRODUCT_IMAGE + " BLOB NOT NULL);";

    private static final String DATABASE_NAME = "store.db";

    private static final int DATABASE_VERSION = 1;

    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.v("Table entries: ", SQL_CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(db);
    }

}

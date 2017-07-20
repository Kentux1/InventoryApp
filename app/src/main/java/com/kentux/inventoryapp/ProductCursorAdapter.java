package com.kentux.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kentux.inventoryapp.data.DbBitmapUtility;
import com.kentux.inventoryapp.data.ProductContract;
import com.kentux.inventoryapp.data.ProductContract.ProductEntry;

import static android.R.attr.id;
import static android.R.attr.inset;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.kentux.inventoryapp.R.id.price;

/**
 * Created by Tiago Gomes on 17/07/2017.
 */

public class ProductCursorAdapter extends CursorAdapter {

    public static final String LOG_TAG = ProductCursorAdapter.class.getName();

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ImageView productImageView = (ImageView) view.findViewById(R.id.product_image_view);
        TextView nameTextView = (TextView) view.findViewById(R.id.product_name);
        TextView priceTextView = (TextView) view.findViewById(price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.in_stock);
        TextView salesTextView = (TextView) view.findViewById(R.id.sales);
        Button sellButton = (Button) view.findViewById(R.id.sell_button);

        int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);
        int salesColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SALES);

        int id = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));
        byte[] productImageRaw = cursor.getBlob(imageColumnIndex);
        final String productName = cursor.getString(nameColumnIndex);
        final double productPrice = cursor.getDouble(priceColumnIndex);
        String productPriceText = "Price: " + String.valueOf(productPrice) + "€";
        final int quantity = cursor.getInt(quantityColumnIndex);
        String inStock = "In stock: " + quantity;
        final double sales = cursor.getFloat(salesColumnIndex);
        String salesText = "Sales: " + sales + " €";

        final Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

        Bitmap productImage = DbBitmapUtility.getImage(productImageRaw);
        productImageView.setImageBitmap(productImage);
        nameTextView.setText(productName);
        priceTextView.setText(productPriceText);
        quantityTextView.setText(inStock);
        salesTextView.setText(salesText);

        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver resolver = v.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (quantity > 0) {
                    int stock = quantity;
                    double price = productPrice;
                    double salesTotal = sales + price;
                    values.put(ProductEntry.COLUMN_PRODUCT_STOCK, --stock);
                    values.put(ProductEntry.COLUMN_PRODUCT_SALES, salesTotal);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                    Log.v(LOG_TAG, "Quantity: " + quantity + ", Sales: " + sales);

                } else {
                    Toast.makeText(context, "You can't sale anymore because there's no stock available", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}

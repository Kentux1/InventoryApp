package com.kentux.inventoryapp;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kentux.inventoryapp.data.DbBitmapUtility;
import com.kentux.inventoryapp.data.ProductContract;
import com.kentux.inventoryapp.data.ProductContract.ProductEntry;

import java.io.File;
import java.sql.Blob;

import static android.R.attr.canRetrieveWindowContent;
import static android.R.attr.data;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.os.Build.VERSION_CODES.M;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mNameEditText;

    private EditText mSupplierEditText;

    private EditText mPriceEditText;

    private Button mDecreaseButton;

    private EditText mQuantityEditText;

    private Button mIncreaseButton;

    private String mProductName;

    private String mProductSupplier;

    private int mProductQuantity;

    private final static int SELECT_IMAGE = 100;

    private final static int REQUEST_READ_EXTERNAL_STORAGE = 200;

    private LinearLayout mSelectImageView;

    private ImageView mProductImageView;

    private TextView mAddPhotoTextView;

    private Bitmap mProductBitmap;

    private static final int EXISTING_PRODUCT_LOADER = 0;

    private Uri mCurrentProductUri;

    private byte[] mCurrentImageBytes;

    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle("Add new product");
            invalidateOptionsMenu();
        } else {
            setTitle("Edit product");
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        mNameEditText = (EditText) findViewById(R.id.name_edit_text);
        mSupplierEditText = (EditText) findViewById(R.id.supplier_edit_text);
        mPriceEditText = (EditText) findViewById(R.id.price_edit_text);

        mQuantityEditText = (EditText) findViewById(R.id.stock_edit_text);
        mQuantityEditText.setText("0");
        mDecreaseButton = (Button) findViewById(R.id.decrease_button);
        mDecreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProductQuantity = Integer.valueOf(mQuantityEditText.getText().toString().trim());
                if (mProductQuantity >0) {
                    mProductQuantity--;
                    mQuantityEditText.setText(String.valueOf(mProductQuantity));
                } else {
                    Toast.makeText(EditorActivity.this, "Quantity must be a positive number.", Toast.LENGTH_SHORT).show();
                }
            }
        });



        mIncreaseButton = (Button) findViewById(R.id.increase_button);
        mIncreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProductQuantity = Integer.valueOf(mQuantityEditText.getText().toString().trim());
                mProductQuantity++;
                mQuantityEditText.setText(String.valueOf(mProductQuantity));
            }
        });

        mProductImageView = (ImageView) findViewById(R.id.image_view);
        mAddPhotoTextView = (TextView) findViewById(R.id.add_a_photo);
        mSelectImageView = (LinearLayout) findViewById(R.id.select_image_view);
        mProductImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mProductHasChanged = true;
                mAddPhotoTextView.setVisibility(View.GONE);
                return false;
            }
        });
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSelectImageView.setOnTouchListener(mTouchListener);

        mProductImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED) {
                        selectProductImage();
                    } else {
                        String[] requestPermission = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(requestPermission, REQUEST_READ_EXTERNAL_STORAGE);
                    }
                } else {
                    selectProductImage();
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case EXISTING_PRODUCT_LOADER:
                return new CursorLoader(
                        this,
                        mCurrentProductUri,
                        null,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            mProductName = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
            mNameEditText.setText(mProductName);
            mProductSupplier = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER));
            mSupplierEditText.setText(mProductSupplier);
            mPriceEditText.setText(data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE)));
            mProductQuantity = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK));
            mQuantityEditText.setText(String.valueOf(mProductQuantity));
            mCurrentImageBytes = data.getBlob(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE));
            mProductBitmap = DbBitmapUtility.getImage(mCurrentImageBytes);
            if (data.getBlob(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE)) != null) {
                mProductImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.getText().clear();
        mQuantityEditText.setText("0");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            selectProductImage();
        } else {
            Toast.makeText(this, "You need permission to access device pictures", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectProductImage() {
        Intent imagePicker = new Intent(Intent.ACTION_PICK);
        imagePicker.setType("image/*");
        startActivityForResult(imagePicker, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            mProductBitmap = BitmapFactory.decodeFile(imagePath, options);
            mCurrentImageBytes = DbBitmapUtility.getBytes(mProductBitmap);
            mProductImageView.setImageBitmap(mProductBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (mProductHasChanged) {
                    saveProduct();
                    finish();
                    return true;
                } else {
                    Toast.makeText(this, "Insertion or update of product has failed.", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                } else {
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                                }
                            };
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveProduct() {
        String name = mNameEditText.getText().toString().trim();
        String price = mPriceEditText.getText().toString().trim();
        String supplier = mSupplierEditText.getText().toString().trim();
        String quantity = mQuantityEditText.getText().toString().trim();
        byte[] image = DbBitmapUtility.getBytes(mProductBitmap);

        if (name.isEmpty() || price.isEmpty() || supplier.isEmpty() || quantity.isEmpty()) {
            Toast.makeText(this, "There are missing fields to fill", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
        values.put(ProductEntry.COLUMN_PRODUCT_STOCK, quantity);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplier);
        values.put(ProductEntry.COLUMN_PRODUCT_SALES, 0.0);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, image);

        if (mCurrentProductUri == null) {
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, "Error while saving product", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, "Error while updating product", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, "Couldn't delete product.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Product deleted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete the product?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteProduct();
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to leave without saving changes?");
        builder.setPositiveButton("Yes", discardButtonClickListener);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}

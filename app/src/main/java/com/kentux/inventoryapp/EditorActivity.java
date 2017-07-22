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
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
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
import com.kentux.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.os.Build.VERSION_CODES.M;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mNameEditText;

    private EditText mSupplierEditText;

    private EditText mPriceEditText;

    private EditText mQuantityEditText;

    private int mProductQuantity;

    private final static int SELECT_IMAGE = 100;

    private final static int REQUEST_READ_EXTERNAL_STORAGE = 200;

    private ImageView mProductImageView;

    private TextView mAddPhotoTextView;

    private Bitmap mProductBitmap;

    private static final int EXISTING_PRODUCT_LOADER = 0;

    private Uri mCurrentProductUri;

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
        Button mOrderButton = (Button) findViewById(R.id.order_from_supplier);
        if (mCurrentProductUri == null) {
            setTitle("Add new product");
            invalidateOptionsMenu();
            mOrderButton.setVisibility(View.INVISIBLE);
        } else {
            setTitle("Edit product");
            mAddPhotoTextView = (TextView) findViewById(R.id.add_a_photo);
            mAddPhotoTextView.setVisibility(View.GONE);
            mOrderButton.setVisibility(View.VISIBLE);
            mOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(Uri.parse("mailto:"));
                    intent.setType("text/plain");
                    startActivity(Intent.createChooser(intent, "Send Mail..."));
                }
            });
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }


        mNameEditText = (EditText) findViewById(R.id.name_edit_text);
        mSupplierEditText = (EditText) findViewById(R.id.supplier_edit_text);
        mPriceEditText = (EditText) findViewById(R.id.price_edit_text);

        mQuantityEditText = (EditText) findViewById(R.id.stock_edit_text);
        mQuantityEditText.setText("0");
        Button mDecreaseButton = (Button) findViewById(R.id.decrease_button);
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



        Button mIncreaseButton = (Button) findViewById(R.id.increase_button);
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
        LinearLayout mSelectImageView = (LinearLayout) findViewById(R.id.select_image_view);
        mSelectImageView.setOnClickListener(new View.OnClickListener() {
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
                mAddPhotoTextView.setVisibility(View.INVISIBLE);
            }
        });
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSelectImageView.setOnTouchListener(mTouchListener);
        mIncreaseButton.setOnTouchListener(mTouchListener);
        mDecreaseButton.setOnTouchListener(mTouchListener);
        mOrderButton.setOnTouchListener(mTouchListener);

        mPriceEditText.setFilters(new InputFilter[] {
                new DecimalInputFilter(100,2)
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_STOCK,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_SALES,
                ProductEntry.COLUMN_PRODUCT_IMAGE
        };
        switch (id) {
            case EXISTING_PRODUCT_LOADER:
                return new CursorLoader(this,
                        mCurrentProductUri,
                        projection,
                        null,
                        null,
                        null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }
        if (data.moveToFirst()) {
            String mProductName = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
            mNameEditText.setText(mProductName);
            String mProductSupplier = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER));
            mSupplierEditText.setText(mProductSupplier);
            mPriceEditText.setText(data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE)));
            mProductQuantity = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK));
            mQuantityEditText.setText(String.valueOf(mProductQuantity));

            byte[] saveProductImage = data.getBlob(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_IMAGE));
            if (saveProductImage != null) {
                mProductBitmap = DbBitmapUtility.getImage(saveProductImage);
                mProductImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.getText().clear();
        mQuantityEditText.setText("0");
        mSupplierEditText.getText().clear();
        mPriceEditText.setText("");
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
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imagePath = cursor.getString(columnIndex);
                cursor.close();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                mProductBitmap = BitmapFactory.decodeFile(imagePath, options);
                mProductBitmap = getBitmapFromUri(selectedImage);
                mProductImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        // Get the dimensions of the View
        mProductImageView = (ImageView) findViewById(R.id.image_view);
        int targetW = mProductImageView.getWidth();
        int targetH = mProductImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            if (input != null) {
                input.close();
            }

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            if (input != null) {
                input.close();
            }

            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e("AddActivity", "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e("AddActivity", "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
                Log.e("AddActivity", "Failed to load image.", ioe);
            }
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
                } else {
                    finish();
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
        String priceEditText = mPriceEditText.getText().toString().trim();
        String supplier = mSupplierEditText.getText().toString().trim();
        String quantity = mQuantityEditText.getText().toString().trim();
        mProductQuantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        double mProductPrice = Double.parseDouble(mPriceEditText.getText().toString().trim());

        boolean isNameEmpty = checkFieldEmpty(name);
        boolean isPriceEmpty = checkFieldEmpty(priceEditText);
        boolean isSupplierEmpty = checkFieldEmpty(supplier);
        boolean isQuantityEmpty = checkFieldEmpty(quantity);

        if (isNameEmpty) {
            Toast.makeText(this, "Please input a valid product name", Toast.LENGTH_SHORT).show();
        } else if (isSupplierEmpty) {
            Toast.makeText(this, "Plese input a valid product supplier", Toast.LENGTH_SHORT).show();
        } else if (isPriceEmpty || mProductPrice <= 0) {
            Toast.makeText(this, "Please input a valid product price", Toast.LENGTH_SHORT).show();
        } else if (mProductQuantity < 0 || isQuantityEmpty) {
            Toast.makeText(this, "Please input a valid product quantity", Toast.LENGTH_SHORT).show();
        } else if (mProductBitmap == null) {
            Toast.makeText(this, "Please input a valid product image", Toast.LENGTH_SHORT).show();
        } else {
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
            values.put(ProductEntry.COLUMN_PRODUCT_STOCK, mProductQuantity);
            double price = Double.parseDouble(priceEditText);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
            values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplier);

            if (mProductBitmap != null) {
                byte[] saveProductImage = DbBitmapUtility.getBytes(mProductBitmap);
                values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, saveProductImage);
            }

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
            finish();
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

    private boolean checkFieldEmpty(String string) {
        return TextUtils.isEmpty(string) || string.equals(".");
    }
}

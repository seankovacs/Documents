package com.skdevsolutions.documents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Toast;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {
    public static final int REQUEST_TAKE_PHOTO = 1;
    public static final int REQUEST_CROP_PHOTO = Crop.REQUEST_CROP;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this, "" + i, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_camera) {
            launchCamera();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {

                    // Convert on disk picture to grayscale
                    convertPictureToGrayscale(mCurrentPhotoPath);

                    Uri source = Uri.fromFile(new File(mCurrentPhotoPath));
                    new Crop(source).output(source).asSquare().start(this);
                }
                break;
            case REQUEST_CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    // Image captured and saved to fileUri specified in the Intent
                    GridView gridview = (GridView) findViewById(R.id.gridview);
                    ((BaseAdapter)gridview.getAdapter()).notifyDataSetChanged();

                    // Prompt for email
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                    // set title
                    alertDialogBuilder.setTitle("Email Document?");

                    // set dialog message
                    alertDialogBuilder
                            .setMessage("Would you like to send this via email?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, close
                                    // current activity
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("application/image");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "Document");
                                    intent.putExtra(Intent.EXTRA_TEXT, "Document captured via Documents for Android.");
                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:"+mCurrentPhotoPath));
                                    startActivity(Intent.createChooser(intent, "Send Email"));
                                }
                            })
                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel();
                                }
                            });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                }
                break;
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public void launchCamera() {
        if(checkCameraHardware(MainActivity.this)) {
            // Kick off camera
            dispatchTakePictureIntent();
        }else {
            Toast.makeText(MainActivity.this, "This device doesn't have a camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error creating file.", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDir = new File(storageDir,"DocPics");
        appDir.mkdirs();

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                appDir      /* directory */
        );

        Log.d("Path",image.toString());
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void convertPictureToGrayscale(String path)
    {
        Bitmap original = BitmapFactory.decodeFile(path);
        Bitmap grayoriginal = toGrayscale(original);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            grayoriginal.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}

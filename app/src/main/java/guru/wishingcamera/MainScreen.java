package guru.wishingcamera;

import android.Manifest;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainScreen extends AppCompatActivity
        implements MessageTemplateDialog.MessageTemplateListener,
        CustomMessageDialog.CustomMessageDialogListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;


    String m_tempFilePath;
    Bitmap m_scaledCapturedBitmap;
    ImageView m_imageView;
    String m_wishingMessage = "";

    private String m_tempImageFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Camera button
        Button startCamera = (Button) findViewById(R.id.camera_action);
        startCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePictureIntent();
            }
        });

        Button message = (Button) findViewById(R.id.message);
        message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMessageDialog();
            }
        });

        Button savePhoto = (Button) findViewById(R.id.save);
        savePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveEditedPhoto();
            }
        });

        m_imageView = (ImageView) findViewById(R.id.image);
    }

    private void showMessageDialog() {
        DialogFragment dialog = new MessageTemplateDialog();
        dialog.show(getFragmentManager(), "MessageTemplateDialog");
    }

    //save the edited photo
    protected void saveEditedPhoto() {
        try {
            saveCapturedPhotos();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    //intent for the camera action
    protected void takePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //create the temp file
            File tempPhotoFile = null;
            try {
                tempPhotoFile = createTempFile();
            } catch (IOException e) {
                Log.e("SD Card", "Error in the SD Card");
            }
            if (tempPhotoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "guru.wishingcamera.fileprovider",
                        tempPhotoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                galleryAddPic();
            }

        }
    }

    //create a temp file to read into the app
    private File createTempFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        m_tempFilePath = image.getAbsolutePath();
        m_tempImageFileName = image.getName();
        Log.d("Create", "tempfile path:" + m_tempFilePath);
        Log.d("Create", "filename:" + m_tempImageFileName);
        return image;
    }

    //this is the return method after the camera action is completed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //get the captured temp file
            scaleDownCapturedImage();

            //set the image view with the edited file
            m_imageView.setImageBitmap(m_scaledCapturedBitmap);
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(m_tempFilePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    @Override
    public void onMessageTemplateClick(DialogInterface dialogFragment, String message, int which) {
        if (which == 3) {
            //draw a new dialog fragment with edit text and take a custom message
            DialogFragment dialog = new CustomMessageDialog();
            dialog.show(getFragmentManager(), "CustomMessageDialog");
        } else {
            m_wishingMessage = message;
            //draw the edited scaled image to image view
            Bitmap editedScaledBitmap = getEditedCapturedImage(m_scaledCapturedBitmap);
            updateImageView(editedScaledBitmap);
        }
    }


    @Override
    /*
    * send message when the custom message has been sent back to activity upon ok
     */
    public void onCustomMessageDialogOk(DialogInterface dialog, String message) {
        m_wishingMessage = message;
        //draw the edited scaled image to image view
        Bitmap editedScaledBitmap = getEditedCapturedImage(m_scaledCapturedBitmap);
        updateImageView(editedScaledBitmap);
    }

    protected Bitmap getEditedCapturedImage(Bitmap uneditedImage) {
        if (uneditedImage != null) {
            Bitmap mutableBitmap = uneditedImage.copy(Bitmap.Config.ARGB_8888, true);
            int imageHeight = mutableBitmap.getHeight();
            int fontSize = (imageHeight / 10);
            //create a canvas and edit the file
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(fontSize);
            canvas.drawText(m_wishingMessage, 20, (imageHeight - 20), paint);
            return mutableBitmap;
        }
        return null;
    }

    private void updateImageView(Bitmap image) {
        m_imageView.setImageBitmap(image);
        m_imageView.invalidate();
    }

    private void saveCapturedPhotos() throws IOException {
        boolean canWriteToSdCard = checkStoragePermission();
        Log.d("Permissino", Boolean.toString(canWriteToSdCard));
        if(canWriteToSdCard) {
            writePhoto();
        }
        else {
            //request permission to write to external disk
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void writePhoto() throws  IOException {
        Bitmap fullSizeCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath);
        Bitmap fullSizeEditedBitmap = getEditedCapturedImage(fullSizeCapturedBitmap);
        File imageFile = new File(m_tempFilePath);

        //Copy file to public folder
        if(isExternalStorageWritable()) {
            File albumDir = getAlbumStorageDir("WishPics");
            File publicImageFile = new File(albumDir, m_tempImageFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(publicImageFile);
            if (fileOutputStream != null) {
                fullSizeEditedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                Toast.makeText(getApplicationContext(), "File saved", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();

                //delete the temp file
                imageFile.delete();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "SD Card not loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkStoragePermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "Permission Granted");
                    saveEditedPhoto();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Permission", "Permission to write SD Card denied");
                    Toast.makeText(this, "App requires SD card permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imagesFolder = new File(file, albumName);
        if (!imagesFolder.mkdirs()) {
            Log.d("Create", imagesFolder + ":Directory not created");
        }
        return imagesFolder;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    //set the picture in the imageview
    private void scaleDownCapturedImage() {
        // Get the dimensions of the View
        int targetW = m_imageView.getWidth();
        int targetH = m_imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        m_scaledCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath, bmOptions);
        if (m_scaledCapturedBitmap == null) {
            BitmapDrawable bmDrawable = (BitmapDrawable) m_imageView.getDrawable();
            m_scaledCapturedBitmap = bmDrawable.getBitmap();
        }
    }

}

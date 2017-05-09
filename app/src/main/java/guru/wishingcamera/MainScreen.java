package guru.wishingcamera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainScreen extends AppCompatActivity
        implements MessageTemplateDialog.MessageTemplateListener,
        CustomMessageDialog.CustomMessageDialogListener,
        SeekBar.OnSeekBarChangeListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;


    String m_tempFilePath;
    Bitmap m_scaledCapturedBitmap;
    ImageView m_imageView;
    String m_wishingMessage = "";
    int m_fontSize = 20;

    private String m_tempImageFileName;
    private int m_scaleFactor;
    private File m_publicImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Camera button
        RelativeLayout startCamera = (RelativeLayout) findViewById(R.id.camera_action);
        startCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePictureIntent();
            }
        });

        RelativeLayout message = (RelativeLayout) findViewById(R.id.message);
        message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMessageDialog();
            }
        });

        RelativeLayout savePhoto = (RelativeLayout) findViewById(R.id.save);
        savePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveEditedPhoto();
            }
        });

        m_imageView = (ImageView) findViewById(R.id.image);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_photo_booth_launch);

        SeekBar slider = (SeekBar) findViewById(R.id.fontSizeSlider);

        slider.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SeekBar slider = (SeekBar) findViewById(R.id.fontSizeSlider);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int maxfontSize = Integer.parseInt(sharedPref.getString("max_font", "100"));
        slider.setMax(maxfontSize);
        Bitmap editedScaledBitmap = getEditedCapturedImage(m_scaledCapturedBitmap, 1);
        updateImageView(editedScaledBitmap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempFiles();
    }

    private void showMessageDialog() {
        AppCompatDialogFragment dialog = new MessageTemplateDialog();
        dialog.show(getSupportFragmentManager(), "MessageTemplateDialog");
    }

    //save the edited photo
    protected void saveEditedPhoto() {
        try {
            saveCapturedPhotos();
        } catch (IOException e) {
            Toast.makeText(this, "Cannot write to sd card", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Error in writing to sd card", Toast.LENGTH_SHORT).show();
            }
            if (tempPhotoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "guru.wishingcamera.fileprovider",
                        tempPhotoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
        Uri contentUri = Uri.fromFile(m_publicImageFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    @Override
    public void onMessageTemplateClick(DialogInterface dialogFragment, String message, int which) {
        if (which == 3) {
            //draw a new dialog fragment with edit text and take a custom message
            AppCompatDialogFragment dialog = new CustomMessageDialog();
            dialog.show(getSupportFragmentManager(), "CustomMessageDialog");
        } else {
            m_wishingMessage = message;
            //draw the edited scaled image to image view
            Bitmap editedScaledBitmap = getEditedCapturedImage(m_scaledCapturedBitmap, 1);
            //update the new image view
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
        updateTextInImage();
    }

    public void updateTextInImage() {
        Bitmap editedScaledBitmap = getEditedCapturedImage(m_scaledCapturedBitmap, 1);
        updateImageView(editedScaledBitmap);
    }

    protected Bitmap getEditedCapturedImage(Bitmap uneditedImage, int scale_factor) {
        if (uneditedImage != null) {
            Bitmap mutableBitmap = uneditedImage.copy(Bitmap.Config.ARGB_8888, true);
            int imageHeight = mutableBitmap.getHeight();
            int imageWidth = mutableBitmap.getWidth();
            int fontSize;
            //create a canvas and edit the file
            Canvas bcanvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);

            m_fontSize *= scale_factor;
            paint.setTextSize(m_fontSize);
            bcanvas.drawText(m_wishingMessage, 20, (imageHeight - m_fontSize / 2), paint);
            return mutableBitmap;
        }
        return null;
    }

    //Todo work for multiple row of strings
    /*
    protected String[] getStrings(int imageWidth) {
        //String[] messageParts = m_wishingMessage.split(" ");
        int stringSize = m_wishingMessage.length();
        int overflow = imageWidth / stringSize;
        String[] messageParts = new String[overflow + 1];
        int beginIndex = 0;
        int partSize = (stringSize / (overflow + 1));
        for(int i = 0 ; i < overflow; i++) {
            if(i == overflow - 1) {
                messageParts[i] = m_wishingMessage.substring(beginIndex, m_wishingMessage.length() - 1);
            }
            messageParts[i] = m_wishingMessage.substring(beginIndex, partSize + beginIndex);
            beginIndex = partSize + beginIndex + 1;
            Log.d("message", messageParts[i]);
        }

        return messageParts;
    }*/


    private void updateImageView(Bitmap image) {
        m_imageView.setImageBitmap(image);
        m_imageView.invalidate();
    }

    private void saveCapturedPhotos() throws IOException, NullPointerException {
        boolean canWriteToSdCard = checkStoragePermission();
        if(canWriteToSdCard) {
            //this should be done in a different thread
            writePhoto();
            //this should be done on onPostExecute in a different thread
            galleryAddPic();
        }
        else {
            //request permission to write to external disk
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void writePhoto() throws  IOException, NullPointerException {
        if(m_tempFilePath != null) {
            Bitmap fullSizeCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath);
            Bitmap fullSizeEditedBitmap = getEditedCapturedImage(fullSizeCapturedBitmap, m_scaleFactor);

            //Copy file to public folder
            if (isExternalStorageWritable()) {
                File albumDir = getAlbumStorageDir("WishPics");
                m_publicImageFile = new File(albumDir, m_tempImageFileName);
                FileOutputStream fileOutputStream = new FileOutputStream(m_publicImageFile);
                if (fileOutputStream != null) {
                    fullSizeEditedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                    Toast.makeText(getApplicationContext(), "File saved", Toast.LENGTH_SHORT).show();
                    fileOutputStream.close();

                }
            } else {
                Toast.makeText(getApplicationContext(), "SD Card not loaded", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTempFiles() {
        if(m_tempFilePath != null) {
            File imageFile = new File(m_tempFilePath);

            if (imageFile != null) {
                //delete the temp file
                imageFile.delete();
            }
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
                    saveEditedPhoto();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "App requires SD card permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imagesFolder = new File(file, albumName);
        imagesFolder.mkdirs();
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
        BitmapFactory.decodeFile(m_tempFilePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        m_scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = m_scaleFactor;
        m_scaledCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath, bmOptions);
        if (m_scaledCapturedBitmap == null) {
            BitmapDrawable bmDrawable = (BitmapDrawable) m_imageView.getDrawable();
            m_scaledCapturedBitmap = bmDrawable.getBitmap();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent preferenceIntent = new Intent(this, SettingsActivity.class);
                startActivity(preferenceIntent);
                return true;

            case R.id.clear_image:
                m_imageView.setImageDrawable(null);
                deleteTempFiles();
                return true;

            case R.id.help:
                sendMail();

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void sendMail() {
        Intent sendMailIntent = new Intent(Intent.ACTION_SEND);
        sendMailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"mbtaloc@gmail.com"});
        sendMailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issue: Family Booth");
        sendMailIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendMailIntent, "Sending Mail"));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        m_fontSize = progress;
        updateTextInImage();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}

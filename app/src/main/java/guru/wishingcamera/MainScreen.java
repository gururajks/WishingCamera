package guru.wishingcamera;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainScreen extends Activity
                        implements MessageTemplateDialog.MessageTemplateListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    String m_tempFilePath;
    Bitmap m_scaledCapturedBitmap;
    ImageView m_imageView;
    String m_wishingMessage="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Camera button
        Button startCamera = (Button) findViewById(R.id.camera_action);
        startCamera.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                takePictureIntent();
            }
        });

        Button message = (Button) findViewById(R.id.message);
        message.setOnClickListener( new View.OnClickListener() {
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
        dialog.show(getFragmentManager(), "NoticeDialogFragment");
    }

    //save the edited photo
    protected void saveEditedPhoto() {
        try {
            saveCapturedPhotos();
        }
        catch (IOException e)
        {
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
            }
            catch (IOException e)
            {
                System.out.println("Error in the SD Cards");
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
        image.deleteOnExit();
        m_tempFilePath = image.getAbsolutePath();
        System.out.println("tempfile path:" + m_tempFilePath);
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

    @Override
    public void onMessageTemplateClick(DialogInterface dialogFragment, String message, int which) {
        if(which == 3) {
            //draw a new dialog fragment with edit text and take a custom message
        }
        else {
            m_wishingMessage = message;
            //draw the edited scaled image to image view
            drawEditedCapturedImage();
        }
    }

    protected void drawEditedCapturedImage() {
        if(m_scaledCapturedBitmap != null) {
            Bitmap mutableBitmap = m_scaledCapturedBitmap.copy(Bitmap.Config.ARGB_8888, true);

            //create a canvas and edit the file
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(40);
            canvas.drawText(m_wishingMessage, 20, (mutableBitmap.getHeight() - 20), paint);
            m_imageView.setImageBitmap(mutableBitmap);
            m_imageView.invalidate();
        }
    }


    private void saveCapturedPhotos() throws IOException {
        //Bitmap fullSizeCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath);
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
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        m_scaledCapturedBitmap = BitmapFactory.decodeFile(m_tempFilePath, bmOptions);
        if(m_scaledCapturedBitmap == null) {
            BitmapDrawable bmDrawable = (BitmapDrawable) m_imageView.getDrawable();
            m_scaledCapturedBitmap = bmDrawable.getBitmap();
        }
    }

}

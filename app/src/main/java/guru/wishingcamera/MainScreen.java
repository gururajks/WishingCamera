package guru.wishingcamera;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

public class MainScreen extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    String tempFilePath;

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
    }

    private void showMessageDialog() {
        DialogFragment dialog = new MessageTemplateDialog();
        dialog.show(getFragmentManager(), "NoticeDialogFragment");
    }

    //save the edited photo
    protected void saveEditedPhoto() {
        try {
            File photoStorageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File tempImage = File.createTempFile("temp", ".jpg", photoStorageDirectory);
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
        tempFilePath = image.getAbsolutePath();
        System.out.println("tempfile path:" + tempFilePath);
        return image;
    }

    //this is the return method after the camera action is completed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //get the captured temp file
            Bitmap capturedBitmap = BitmapFactory.decodeFile(tempFilePath);
            //create a mutable image so that it can be edited
            Bitmap mutableBitmap = capturedBitmap.copy(Bitmap.Config.ARGB_8888, true);

            //create a canvas and edit the file
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            canvas.drawText("Congrats", 20, 20, paint);
            //set the image view with the edited file
            ImageView imageCaptured = (ImageView) findViewById(R.id.image);
            imageCaptured.setImageBitmap(mutableBitmap);

        }
    }

}

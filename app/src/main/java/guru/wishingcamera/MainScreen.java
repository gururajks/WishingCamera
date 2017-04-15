package guru.wishingcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class MainScreen extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

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

        Button savePhoto = (Button) findViewById(R.id.save);
        savePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveEditedPhoto();
            }
        });


    }

    //save the edited photo
    protected void saveEditedPhoto() {
        try {
            File photoStorageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File tempImage = File.createTempFile("temp", ".jpg", pho
                    toStorageDirectory);
            System.out.println(tempImage.getAbsolutePath());
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
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //this is the return method after the camera action is completed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView imageCaptured = (ImageView) findViewById(R.id.image);
            imageCaptured.setImageBitmap(imageBitmap);
        }
    }

}

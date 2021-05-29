package com.editor.videotrimmer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    Uri selectedUri;
    ImageView trimvideo;
    Button uploadVideoBtn;
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trimvideo = findViewById(R.id.trim);
        uploadVideoBtn = findViewById(R.id.uploadVideoBtn);
        videoView = findViewById(R.id.videoView);

        uploadVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openVideo();

            }
        });


        trimvideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedUri != null) {
                    Intent intent = new Intent(MainActivity.this, TrimActivity.class);
                    intent.putExtra("uri", selectedUri.toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please Upload a Video", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void openVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, 100);
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            selectedUri = data.getData();
            videoView.setVideoURI(selectedUri);
            videoView.start();
           /* Intent intent = new Intent(MainActivity.this, TrimActivity.class);
            intent.putExtra("uri", selectedUri.toString());
            startActivity(intent);*/
        }
    }

}
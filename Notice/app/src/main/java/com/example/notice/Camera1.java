package com.example.notice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class Camera1 extends Activity {

    ImageView imageView;
    TextView time_txt;
    TextView height_txt;

    Bitmap bm = null;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        setContentView(R.layout.activity_camera1);

        imageView = (ImageView)findViewById(R.id.image);
        time_txt = (TextView)findViewById(R.id.time_txt);
        height_txt = (TextView)findViewById(R.id.person_height_txt);

        Bundle bundle = intent.getExtras();

        String received = bundle.getString("time_height");

        if(received.equals("")){
            time_txt.setText("떨어진 시간 : ");
            height_txt.setText("키 : ");
        }

        else{

            String[] time_height = received.split(",");

            Log.d("cheolsoon",time_height[0]);
            Log.d("cheolsoon",time_height[1]);
            Log.d("cheolsoon",received);

            time_txt.setText("떨어진 시간 : " + time_height[0]);
            height_txt.setText("키 : " + time_height[1]);

        }

        byte[] image = bundle.getByteArray("image");

        if(image == null){
            bm = null;
        }

        else {
            bm = BitmapFactory.decodeByteArray(image, 0, image.length);
        }

        imageView.setImageBitmap(bm);

    }
}

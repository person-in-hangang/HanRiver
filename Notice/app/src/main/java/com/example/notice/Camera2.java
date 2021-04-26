package com.example.notice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

public class Camera2 extends Activity {

    ImageView tracking_img;
    TextView latitude_txt;
    TextView longitude_txt;
    Button map_btn;
    Double latitude;
    Double longitude;
    Bitmap bm = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        tracking_img = (ImageView)findViewById(R.id.tracking_img);
        latitude_txt = (TextView)findViewById(R.id.latitude_txt);
        longitude_txt = (TextView)findViewById(R.id.longitude_txt);
        map_btn = (Button)findViewById(R.id.map_btn);

        Intent intent = getIntent();

        Bundle bundle = intent.getExtras();

        byte image[] = bundle.getByteArray("image");

        if(image == null){
            bm = null;
        }

        else {
            bm = BitmapFactory.decodeByteArray(image, 0, image.length);
        }

        tracking_img.setImageBitmap(bm);

        latitude = bundle.getDouble("latitude");
        longitude = bundle.getDouble("longitude");

        latitude_txt.setText("위도 : " + latitude);
        longitude_txt.setText("경도 : " + longitude);

        map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(getApplicationContext(),MapsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude",latitude);
                bundle.putDouble("longitude",longitude);
                intent2.putExtras(bundle);
                startActivity(intent2);
            }
        });



    }
}

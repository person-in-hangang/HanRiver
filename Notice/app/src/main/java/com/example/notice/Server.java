package com.example.notice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Output;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        final TextView fallen_signal_txt = (TextView) findViewById(R.id.fallen_signal_txt);
        TextView height_txt = (TextView) findViewById(R.id.height_txt);
        Button location_btn = (Button)findViewById(R.id.location_btn);
        Button map_btn = (Button)findViewById(R.id.map_btn);

        final double[] longitude_value = new double[1];
        final double[] latitude_value = new double[1];

        Intent intent = getIntent();

        final LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        final LocationListener gpsLocationListner = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                String provider = location.getProvider();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                double altitude = location.getAltitude();

                longitude_value[0] = longitude;
                latitude_value[0] = latitude;

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s) {
            }
        };

        location_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Server.this, new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            0);
                }
                else{
                    Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    String provider = location.getProvider();
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    double altitude = location.getAltitude();

                    fallen_signal_txt.setText("위치정보 : " + provider + "\n" +  "위도 : " + latitude + "\n" + "경도 : " + longitude + "\n" + "고도 : " + altitude);

                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            1000,1,gpsLocationListner);
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,1,gpsLocationListner);
                }
            }
        });

        map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(getApplicationContext(),MapsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude",latitude_value[0]);
                Log.i("cheol",Double.toString(latitude_value[0]));
                bundle.putDouble("longitude",longitude_value[0]);
                Log.i("cheol",Double.toString(longitude_value[0]));
                intent2.putExtras(bundle);

                startActivity(intent2);
            }
        });

    }

}

package com.example.notice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    FragmentManager fm;
    FragmentTransaction tran;
    WeatherFragment weather_f;
    RealtimeFragment realtime_f;
    ReportFragment report_f;
    SettingFragment setting_f;

    FrameLayout frame_layout;
    Button realtimeB,weatherB,reportB,settingB;

    int count = 0;

    String msg="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("jinajina",msg);

        realtime_f = new RealtimeFragment();
        report_f = new ReportFragment();
        setting_f = new SettingFragment();
        setFrag(0);

        frame_layout=findViewById(R.id.fragment);

        realtimeB=findViewById(R.id.realtime);
        reportB=findViewById(R.id.report);
        settingB=findViewById(R.id.setting);

        realtimeB.setOnClickListener(this);
        reportB.setOnClickListener(this);
        settingB.setOnClickListener(this);

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){

            case R.id.realtime:
                setFrag(0);
                break;
            case R.id.report:
                setFrag(2);
                break;
            case R.id.setting:
                setFrag(3);
                break;
        }
    }

    public void setFrag(int n) {    //프레그먼트 교체 메소드
        fm = getFragmentManager();
        tran = fm.beginTransaction();
        switch (n) {
            case 0:
                tran.replace(R.id.fragment,realtime_f);
                tran.commit();
                break;
            case 2:
                tran.replace(R.id.fragment,report_f);
                tran.commit();
                break;
            case 3:
                tran.replace(R.id.fragment,setting_f);
                tran.commit();
                break;
        }
    }

    public void alarm() {

        //알림(Notification)을 관리하는 관리자 객체를 운영체제(Context)로부터 소환하기
        NotificationManager notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Notification 객체를 생성해주는 건축가객체 생성(AlertDialog 와 비슷)
        NotificationCompat.Builder builder= null;

        //Oreo 버전(API26 버전)이상에서는 알림시에 NotificationChannel 이라는 개념이 필수 구성요소가 됨.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            String channelID="channel_02"; //알림채널 식별자
            String channelName="MyChannel02"; //알림채널의 이름(별명)

            //알림채널 객체 만들기
            NotificationChannel channel= new NotificationChannel(channelID,channelName,NotificationManager.IMPORTANCE_HIGH);

            //알림매니저에게 채널 객체의 생성을 요청
            notificationManager.createNotificationChannel(channel);

            //알림건축가 객체 생성
            builder=new NotificationCompat.Builder(this, channelID);


        }else{
            //알림 건축가 객체 생성
            builder= new NotificationCompat.Builder(this, null);
        }

        //건축가에게 원하는 알림의 설정작업
        builder.setSmallIcon(R.drawable.ic_bell);

        //상태바를 드래그하여 아래로 내리면 보이는
        //알림창(확장 상태바)의 설정

        builder.setContentTitle("119 NOTICE");//알림창 제목
        if(count==0){
            builder.setContentText("투신 감지");//알림창 내용
            count++;
        }
        else{
            builder.setContentText("투신 예측");//알림창 내용
            count = 0;
        }
        //알림창의 큰 이미지
        Bitmap bm= BitmapFactory.decodeResource(getResources(),R.drawable.ic_bell);
        builder.setLargeIcon(bm);//매개변수가 Bitmap을 줘야한다.

//        알림창을 클릭시에 실행할 작업
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //지금 실행하는 것이 아니라 잠시 보류시키는 Intent 객체 필요
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        //알림창 클릭시에 자동으로 알림제거
        builder.setAutoCancel(true);

        //건축가에게 알림 객체 생성하도록
        Notification notification=builder.build();

        //알림매니저에게 알림(Notify) 요청
        notificationManager.notify(1, notification);

        //알림 요청시에 사용한 번호를 알림제거 할 수 있음.
        //notificationManager.cancel(1);

    }

}

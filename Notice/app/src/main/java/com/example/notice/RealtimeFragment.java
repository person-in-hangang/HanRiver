package com.example.notice;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class RealtimeFragment extends Fragment implements View.OnClickListener{
    ViewGroup rootView;
    LinearLayout camera1,camera2;
    ImageButton start, reset;

    Thread t1, t2, t3, t4;
    String time_height="";

    Double latitude = 0.0;
    Double longitude = 0.0;

    TextView text1, text2, text3;

    ImageView imageView;
    TextView time_txt;
    TextView height_txt;
    TextView information_txt;
    TextView gps_txt;

    ImageView tracking_img;
    TextView latitude_txt;
    TextView longitude_txt;
    ImageButton map_btn;

    byte[] bitmapdata = null;
    byte[] bitmapdata2 = null;

    private Handler mHandler;
    private Handler mHandler2;
    private Handler mHandler3;

    private ByteArrayOutputStream buf;
    private ByteArrayOutputStream size;
    private int port = 7002;
    Handler handler = new Handler();
    Handler handler2 = new Handler();
    Handler handler3 = new Handler();

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    // ImageView noImage;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        rootView = (ViewGroup) inflater.inflate(R.layout.frag_realtime, container, false);

        camera1 = rootView.findViewById(R.id.camera1);
        camera2 = rootView.findViewById(R.id.camera2);

        camera1.setOnClickListener(this);
        camera2.setOnClickListener(this);

        //start = rootView.findViewById(R.id.start_btn);
        reset = rootView.findViewById(R.id.reset_btn);

        imageView = rootView.findViewById(R.id.img1);
        time_txt = rootView.findViewById(R.id.TextTime);
        height_txt = rootView.findViewById(R.id.TextPerson);
        information_txt = rootView.findViewById(R.id.TextInformation);

        gps_txt = rootView.findViewById(R.id.GPS);

        text1 = rootView.findViewById(R.id.text1);
        text2 = rootView.findViewById(R.id.text2);
        text3 = rootView.findViewById(R.id.text3);

        //  noImage = rootView.findViewById(R.id.noimage);

        tracking_img = rootView.findViewById(R.id.img3);
        latitude_txt = rootView.findViewById(R.id.latitude_txt);
        longitude_txt = rootView.findViewById(R.id.longitude_txt);
        map_btn = rootView.findViewById(R.id.map_btn);
        Timer m_timer = new Timer();
        TimerTask m_taxk = new TimerTask() {
            @Override
            public void run() {
                connect2();
            }
        };

        connect();

        // connect2()를 1초뒤에 실행
        m_timer.schedule(m_taxk,1000);

        map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(),MapsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putDouble("latitude",latitude);
                bundle.putDouble("longitude",longitude);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                time_txt.setText("투신 시각 : ");
                height_txt.setText("투신자 예측 : ");

//                latitude_txt.setText("위도 : ");
//                longitude_txt.setText("경도 : ");

                gps_txt.setText("위도 : 경도: ");

                text1.setText("정보가 없습니다");
                text2.setText("정보가 없습니다");
                text3.setText("정보가 없습니다");

                latitude = 0.0;
                longitude = 0.0;
                time_height = "";
                //getResources().getIdentifier("yourpackagename:drawable/" + StringGenerated, null, null);

                imageView.setImageResource(R.drawable.ic_no_photo);
                tracking_img.setImageResource(R.drawable.ic_no_photo);

                Timer m_timer = new Timer();
                TimerTask m_taxk = new TimerTask() {
                    @Override
                    public void run() {
                        connect2();
                    }
                };

                connect();

                // connect2()를 1초뒤에 실행
                m_timer.schedule(m_taxk,1000);

            }
        });


        return rootView;
    }

    void connect(){
        mHandler = new Handler(Looper.getMainLooper());
        Log.w("connect","연결 하는중");
        // 받아오는거
        t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 서버 접속
                String newip = "210.102.181.248";
                try {
                    socket = new Socket(newip, port);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                    Log.d("jinajina","ddddddddd");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(true) {
                    try {
                        dos.writeUTF(":2:"+"@");
                        dos.flush();
                        break;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    // picture

//                    iv = (ImageView) findViewById(R.id.imageView1);
                    InputStream in = socket.getInputStream();
                    //이 버퍼는 되겠지
                    BufferedInputStream bis = new BufferedInputStream(in);
                    buf = new ByteArrayOutputStream();
                    Log.d("jinajina", "" + bis);
                    //파이썬을 꺼야 이게 시작됨 왜  ?
                    int result = bis.read();
                    while(result!=-1){
                        //하나씩 읽어서 bytearray 버퍼에 저장!!!!
                        buf.write((byte) result);
                        result = bis.read();
                        Log.d("jinajina","new555");
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                //파이썬을 끄면 이게 실행됨
                Log.d("jinajina", "new4455" + buf);
                bitmapdata = buf.toByteArray();//이게왜
                Log.d("jinajina", "new777" + bitmapdata);
                final Bitmap bm = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {  // 화면에 그ㄹ;ㄱ;

                        ((MainActivity)getActivity()).alarm();

                        imageView.setImageBitmap(bm);

                        Timer m_timer2 = new Timer();
                        TimerTask m_taxk2 = new TimerTask() {
                            @Override
                            public void run() {
                                connect3();
                            }
                        };

                        connect4();

                        m_timer2.schedule(m_taxk2,1000);

                        Log.d("jinajina", "new44");
                    }
                });

            }
        });
        t1.start();
        Log.d("cheolsoon","t1 start");
    }

    void connect2(){

        mHandler2 = new Handler(Looper.getMainLooper());
        Log.w("connect","연결 하는중");
        // 받아오는거
        t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 서버 접속
                String newip = "210.102.181.248";
                try {
                    socket = new Socket(newip, port);
                    Log.i("cheol","come");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                    Log.d("jinajina","ddddddddd");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(true) {
                    try {
                        dos.writeUTF(":3:"+"@");
                        dos.flush();
                        break;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    byte[] dataS = new byte[4];
                    dis.read(dataS,0,4);
                    ByteBuffer b = ByteBuffer.wrap(dataS);
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    int length = b.getInt();
                    dataS= new byte[length];
                    dis.read(dataS,0,length);

                    time_height = new String(dataS, "UTF-8");
                    Log.d("cheolsoon",time_height);

                    handler2.post(new Runnable() {
                        @Override
                        public void run() {  // 화면에 그ㄹ;ㄱ;
                            text2.setText("투신이 예측됩니다");
                            text1.setText("투신 감지됩니다");
                            String[] time_height2 = time_height.split(",");

                            // time_height2[0]:시간, time_height2[1]:키, time_height2[2]:특징1, time_height2[3]:특징2, time_height2[4]:특징3

                            String[] arr = time_height2[0].split("-");

                            String newww = arr[1]+"-"+arr[2];
                            Log.d("jin",newww);

                            String[] dfdf = newww.split(":");
                            Log.d("jin",dfdf[0]+"/..."+dfdf[1]);
                            time_txt.setText("투신 시각 : " + dfdf[0]+" : "+dfdf[1]);

                            if(Double.parseDouble(time_height2[1]) > 120)
                                height_txt.setText("투신자 예측 : 성인" );
                            else
                                height_txt.setText("투신자 예측 : 어린이");

                            information_txt.setText("투신자 특징 : " + time_height2[2] + ", " + time_height2[3] + ", " + time_height2[4]);


                            // time_txt.setText("떨어진 시간 : " + time_height2[0]);
                            // height_txt.setText("키 : " + time_height2[1]);
//                            gps_txt.setText("위도 : " + latitude + "경도 : " + longitude);
                            // start connect3 and connect4 method after set image and height

                            Log.d("jinajina", "new445");
                        }
                    });

                }
                catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        t2.start();
        Log.d("cheolsoon","t2 start");
    }

    void connect3(){
        mHandler3 = new Handler(Looper.getMainLooper());
        Log.w("connect","연결 하는중");
        // 받아오는거
        t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 서버 접속
                String newip = "210.102.181.248";
                try {
                    socket = new Socket(newip, port);
                    Log.i("cheol","come");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                    Log.d("jinajina","ddddddddd");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(true) {
                    try {
                        dos.writeUTF(":5:"+"@");
                        dos.flush();
                        Log.d("cheolsoon","5 start");
                        break;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    byte[] dataS = new byte[4];
                    dis.read(dataS,0,4);
                    ByteBuffer b = ByteBuffer.wrap(dataS);
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    int length = b.getInt();
                    dataS= new byte[length];
                    dis.read(dataS,0,length);

                    final String coor = new String(dataS, "UTF-8");
//                    Log.d("cheolsoon",coor);

                    Log.d("cheolsoon",coor);

                    String[] location = coor.split(",");
                    latitude = Double.valueOf(location[0]);
                    longitude = Double.valueOf(location[1]);
//                    Log.d("cheolsoon",""+latitude);
//                    Log.d("cheolsoon",""+longitude);

                    handler3.post(new Runnable() {
                        @Override
                        public void run() {
                            text3.setText("예상 낙하 위치");

                            gps_txt.setText("위도 : " + Math.round(latitude*1000)/1000.0 + "  경도 : " + Math.round(longitude*1000)/1000.0);

//                            Log.d("jinajina", "new445");
                        }
                    });

                }
                catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        t3.start();
        Log.d("cheolsoon","t3 start");
    }

    void connect4(){
        mHandler = new Handler(Looper.getMainLooper());
        Log.w("connect","연결 하는중");
        // 받아오는거
        t4 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 서버 접속
                String newip = "210.102.181.248";
                try {
                    socket = new Socket(newip, port);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                    Log.d("jinajina","ddddddddd");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(true) {
                    try {
                        dos.writeUTF(":7:"+"@");
                        dos.flush();
                        break;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    // picture

                    InputStream in = socket.getInputStream();
                    //이 버퍼는 되겠지
                    BufferedInputStream bis = new BufferedInputStream(in);
                    buf = new ByteArrayOutputStream();
                    Log.d("jinajina", "" + bis);
                    //파이썬을 꺼야 이게 시작됨 왜  ?
                    int result = bis.read();
                    while(result!=-1){
                        //하나씩 읽어서 bytearray 버퍼에 저장!!!!
                        buf.write((byte) result);
                        result = bis.read();
                        Log.d("jinajina","new555");
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                //파이썬을 끄면 이게 실행됨
                Log.d("jinajina", "new4455" + buf);
                bitmapdata2 = buf.toByteArray();//이게왜
                Log.d("jinajina", "new777" + bitmapdata2);
                final Bitmap bm2 = BitmapFactory.decodeByteArray(bitmapdata2, 0, bitmapdata2.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {  // 화면에 그ㄹ;ㄱ;

                        ((MainActivity)getActivity()).alarm();

                        tracking_img.setImageBitmap(bm2);
                        Log.d("jinajina", "new44");
                    }
                });

            }
        });
        t4.start();
        Log.d("cheolsoon","t4 start");
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){

            case R.id.camera1:
                setFrag(0);
                break;
//            case R.id.server:
//                setFrag(1);
//                break;
            case R.id.camera2:
                setFrag(2);
                break;
        }
    }

    public void setFrag(int n) {    //프레그먼트 교체 메소드
        Intent intent;
        switch (n) {
            case 0:
                Log.d("jina","1");
                intent = new Intent(getContext(),Camera1.class);
                Bundle bundle = new Bundle();
                bundle.putByteArray("image",bitmapdata);
                bundle.putString("time_height",time_height);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
//            case 1:
//                Log.d("jina","2");
//                intent = new Intent(getContext(),Server.class);
//                startActivity(intent);
//                break;
            case 2:
                Log.d("jina","3");
                intent = new Intent(getContext(),Camera2.class);
                Bundle bundle2 = new Bundle();
                // 트래킹 이미지 전달 추가
                bundle2.putDouble("latitude",latitude);
                bundle2.putDouble("longitude",longitude);
                bundle2.putByteArray("image",bitmapdata2);
                intent.putExtras(bundle2);
                startActivity(intent);
                break;
        }
    }
}
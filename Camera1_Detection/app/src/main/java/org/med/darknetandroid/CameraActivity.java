package org.med.darknetandroid;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {

    //루트 찾기
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mPersonRef, mFallRef, mPhotoRef,mTimeRef; //변수 4개 생성
    //DatabaseReference mInitTable, mInitMemRef, mInitTimeRef;
    long now ;
    Date date;
    SimpleDateFormat sdfNow;
    String formatDate;
    private int person = 0;
    private static final String TAG = "CameraActivity";
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static List<String> classNames;
    private static List<Scalar> colors = new ArrayList<>();
    private final Semaphore writeLock = new Semaphore(1);
    private int[] p1 = {277,0};
    private int[] p2 = {138,416};
    int class_id;
    int centerX;
    int centerY;
    int width;
    int height;
    int left;
    int top;
    int right;
    int bottom;
    boolean isConnected=true;

    private byte[] data;
    private Net net;
    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean permissionGranted = false;
    private Mat frame; //openCV에서 쓰는 data type, Matrix 구조체이다
    // Mat 타입의 2차원 영상 VS blob 타입의 다차원 영상
    // openCV 에서는 blob 은 Mat 타입의 4차원 행렬로 표현됨!
    List<String> outBlobNames;
    Bitmap bmp;
    //바운딩박스 왼쪽위, 오른쪽아래, 객체이름
    Point left_top;
    Point right_bottom;
    String label;
    private String html = "";
    private Handler mHandler;
    private Handler mHandler2;
    Handler handler = new Handler();
    private Socket socket;
    Socket socket2;
    String base64;

    private DataOutputStream dos;
    private DataInputStream dis;
    private DataOutputStream os;
    private DataInputStream is;
    private int port = 7002;


    boolean isfall = false;
    boolean recording = false;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        postFirebaseDatabase(true);


        //카메라 퍼미션임
        if (!permissionGranted) {
            checkPermissions();
        }
        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setMaxFrameSize(416,416);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        classNames = readLabels("labels.txt", this); //파일에서 분류할 클래스파일이름 가져옴
        for (int i = 0; i < classNames.size(); i++)
            colors.add(randomColor());
        Log.d("jina", classNames.toString()); //이거 그 물체이름 다 나옴
        //난간 (선) 그리기
        Log.d("ㅓjina", "ㅇㄹ"+p1[0]+p1[1]);


    }
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    //네트워크 로드
    @Override
    public void onCameraViewStarted(int width, int height) {

        String modelConfiguration = getAssetsFile("yolov3-tiny.cfg", this);
        String modelWeights = getAssetsFile("yolov3-tiny.weights", this); //가중치 파일.
        net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights); //레이블 이름(coco dataset)을 읽는다.
    }
    @Override
    public void onCameraViewStopped() {
    }
    //실시간 감지 & 프레임 흐름 생성 부분
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) { //미리보기 frame 으로 Mat 객체 반환

        //색변경 & 프레임 크기 정규화
        frame = inputFrame.rgba();
        Log.d("kfkf","df"+frame.height()+frame.width()); //1080, 1920
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // frame_size 는 32배를 더하거나 빼서 크기 변경가능, 크기를 줄이면 성능향상, 정확도는 떨어진다
        Size frame_size = new Size(256, 256);
        Scalar mean = new Scalar(127.5);
        //난간그리기
        Imgproc.line(frame, new Point(p1[0],p1[1]), new Point(p2[0],p2[1]), new Scalar(0,0,255), 7, 2);
        // frame 전처리  -> 행렬로 변환시킴
        // blob 은 Mat 타입의 4차원 행렬. 각 차원은 NCHW 정보를 포함한다.
        // N 은 영상갯수, C는 채널갯수, H,W는 각각 영상의 세로, 가로 크기 !
        Mat blob = Dnn.blobFromImage(frame, 1.0 / 255.0, frame_size, mean, true, false);
        //save_mat(blob); //영상데이터 저장 !!!
        net.setInput(blob); //학습된 모델에 얻은 영상데이터 넣기!

        List<Mat> result = new ArrayList<>(); // 미리보기 frame에서 모든 detect를 기록함!
        outBlobNames = net.getUnconnectedOutLayersNames(); //출력 layer 의 이름을 가져온다.

        //탐지하는 단계.
        //이름이 outBlobNames 인 layer의 출력을 계산하기 위해 net 의 forward 로 전달
        net.forward(result, outBlobNames); //이거 출력하면 욜로16, 욜로 23이 나옴 -> 뭔뜻? 23레이어를 출력하겠다..?
        float confThreshold = 0.5f;
        //경계상자, label 그리기 ~~
        for (int i = 0; i < result.size(); ++i) {
            // each row is a candidate detection, the 1st 4 numbers are
            // [center_x, center_y, width, height], followed by (N-4) class probabilities
            Mat level = result.get(i);
            for (int j = 0; j < level.rows(); ++j) {
                Mat row = level.row(j);
                Mat scores = row.colRange(5, level.cols());
                Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                float confidence = (float) mm.maxVal;
                Point classIdPoint = mm.maxLoc;
                if (confidence > confThreshold) {

                    centerX = (int) (row.get(0, 0)[0] * frame.cols());
                    centerY = (int) (row.get(0, 1)[0] * frame.rows());
                    width = (int) (row.get(0, 2)[0] * frame.cols());
                    height = (int) (row.get(0, 3)[0] * frame.rows());
                    Log.d("point", "point:" + centerX + centerY);
                    //x범위가 2220 y 범위가 1080임
//mLocked.displayWidth 1440, mLocked.displayHeight 2960

                    //바운딩박스 4개 좌표
                    left = (int) (centerX - width * 0.5);
                    top = (int) (centerY - height * 0.5);
                    right = (int) (centerX + width * 0.5);
                    bottom = (int) (centerY + height * 0.5);
//left, top, width, height

                    left_top = new Point(left, top);
                    right_bottom = new Point(right, bottom);
                    Point label_left_top = new Point(left, top - 5);
                    DecimalFormat df = new DecimalFormat("#.##");
                    class_id = (int) classIdPoint.x;
                    label = classNames.get(class_id) + ": " + df.format(confidence);
                    if (classNames.get(class_id).equals("person")) {
                        if (detect(centerX, centerY)) {
                            if(!isfall) {//지금 안떨어진상태라면

                                Log.d("point", classNames.get(class_id) + " detect");
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        final Toast toast = Toast.makeText(getApplicationContext(), classNames.get(class_id) + " is fall", Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                });
                                save_mat(frame); //저장하고
                                isfall = true; //떨어진상태로 바꿈.

                            }
                        }
                        else
                            isfall = false; //선의 왼쪽이면 안떨어진 걸로바꿈
                    }
                    //디스플레이 화면 width 는 1080, height는 2220
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // final Toast toast = Toast.makeText(getApplicationContext(), label + "\n" + left_top.toString() + "\n" + right_bottom.toString(), Toast.LENGTH_SHORT);
                            // toast.show();
                        }
                    });
                    //랜덤으로 객체마다 색을 다르게함미다
                    Scalar color = colors.get(class_id);
                    //화면에 네모 그리기 !
                    Imgproc.rectangle(frame, left_top, right_bottom, color, 3, 2);
                    Imgproc.putText(frame, label, label_left_top, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 4);
                    Imgproc.putText(frame, label, label_left_top, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 2);
                }
            }
        }
        return frame;
    }


    private boolean checkPermissions() {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
            return false;
        } else {
            return true;
        }

    }


    private static String getAssetsFile(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }


    //labels.txt파일에서 분류이름 가져옴
    private List<String> readLabels(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        List<String> labelsArray = new ArrayList<>();
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            Scanner fileScanner = new Scanner(new File(outFile.getAbsolutePath())).useDelimiter("\n");
            String label;
            while (fileScanner.hasNext()) {
                label = fileScanner.next();
                labelsArray.add(label);
            }
            fileScanner.close();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to read labels!");
        }
        return labelsArray;
    }

    private Scalar randomColor() {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        return new Scalar(r, g, b);
    }
    private void save_mat(Mat mat) {
        //해상도 줄이기
        Log.d("thisthis","falfkjalksjdf");
        int num =2;

        bmp = Bitmap.createBitmap((mat.width()), mat.height(), Bitmap.Config.ARGB_8888);
        Mat tmp = new Mat(mat.width(), mat.height(), CvType.CV_8UC1, new Scalar(4));
        //Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGB2GRAY);//흑백으로 바꿔보기
        Utils.matToBitmap(tmp, bmp);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] bImage = baos.toByteArray();
        base64 = Base64.encodeToString(bImage,0);
        data = getImageByte(bmp);
        Log.d("bbbb", "this" + data);

     //   connect2();
        connect();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }
    public void releaseWriteLock() {
        writeLock.release();
    }
    //비트맵의 byte배열을 얻는다
    public byte[] getImageByte(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        return out.toByteArray();
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {     // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth)
        {
            int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //숫자를 byte형태로 바꾼다
    private byte[] getByte(int num) {
        byte[] buf = new byte[4];
        buf[0] = (byte) ((num >>> 24) & 0xFF);
        buf[1] = (byte) ((num >>> 16) & 0xFF);
        buf[2] = (byte) ((num >>> 8) & 0xFF);
        buf[3] = (byte) ((num >>> 0) & 0xFF);

        return buf;
    }

    boolean detect(float x,float y)
    {
        // y= a*x +b
        float a=(p1[1]-p2[1])/(p1[0]-p2[0]);
        float b=p2[1]-(a)*p2[0];
        if((a*x+b)<y)
            return true;
        return false;
    }

    //서버한테 보내기
    void connect(){
        mHandler = new Handler(Looper.getMainLooper());
        Log.w("connect","연결 하는중");
        // 받아오는거
        Thread checkUpdate = new Thread() {
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String token ="@";
                byte[] size = getByte(data.length);
                byte[] total = new byte[size.length +token.getBytes(Charset.defaultCharset()).length+ data.length];

                System.arraycopy(size, 0,total,0,size.length);
                System.arraycopy(token.getBytes(),0,total,size.length,token.getBytes().length);
                System.arraycopy(data,0,total,(size.length+token.getBytes().length),data.length);
                while(true) {
                    try {
                        dos.writeUTF(":1:"+"@" +left +"@"+width+"@"+top+"@"+height+"@");
                        dos.flush();
                        dos.write(total, 0, total.length);
                        dos.flush();
                        //dos.writeUTF(base64);
                        //dos.flush();
                        socket.close();
                        break;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        };
        checkUpdate.start();
    }

    //이건
    /*
    void connect2(){
        person++;
        postFirebaseDatabase(true);
    }

     */





//
//    public void postFirebaseDatabase(boolean add){
//        //  Toast toast = Toast.makeText(getApplicationContext(), "firebaes", Toast.LENGTH_SHORT);
//        // toast.show();
//        now = System.currentTimeMillis();
//        date = new Date(now);
//        // 시간을 나타냇 포맷을 정한다
//        sdfNow = new SimpleDateFormat("MM/dd HH:mm");
//        // nowDate 변수에 값을 저장한다.
//        formatDate = sdfNow.format(date);
//        //시간을 디비에 넣는다
//
//        Table dataList = new Table(formatDate);
//        /*
//        mRootRef.child("person_list").push().setValue("1@"+formatDate);
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//
//
//
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//
//
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//
//
//*/
//        /*
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"06/21 14");
//
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"07/21 14");
//
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"08/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"09/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"10/21 14");
//
//
//
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//        mRootRef.child("person_list").push().setValue("1@"+"11/21 14");
//
//
//
//
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//        mRootRef.child("person_list").push().setValue("2@1@성수대교");
//
//
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//        mRootRef.child("person_list").push().setValue("2@2@마포대교");
//
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//        mRootRef.child("person_list").push().setValue("2@3@천호대교");
//
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//        mRootRef.child("person_list").push().setValue("2@4@청담대교");
//
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//        mRootRef.child("person_list").push().setValue("2@5@잠수교");
//
//
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//        mRootRef.child("person_list").push().setValue("2@6@영동대교");
//
//         */
//
//
//        //mRootRef.child("person_list").child("time").updateChildren(dataList.toMap());
//        //mRootRef.child("person_list").child("time").setValue(dataList);
//
//
//        //mRootRef.child("person_list").child("person"+person).child("number").setValue(person);
//        //mRootRef.child("person_list").child("person"+person).child("fall").setValue(true);
//
//        // mRootRef.child("person_list").child("person"+person).child("time").setValue(formatDate);
//
//
//        // }
//
//    }
//






/*
    void connect2(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    String ip = "192.168.35.23";//IP 주소가 작성되어 있는 EditText에서 서버 IP 얻어오기
                    String port = "5001";
                    if(ip.isEmpty() || port.isEmpty()){


                    }else {
                        //서버와 연결하는 소켓 생성..
                        socket2 = new Socket(InetAddress.getByName(ip), Integer.parseInt(port));
                        //여기까지 왔다는 것을 예외가 발생하지 않았다는 것이므로 소켓 연결 성공..

                        //서버와 메세지를 주고받을 통로 구축
                        //   is = new DataInputStream(socket.getInputStream());
                        os = new DataOutputStream(socket2.getOutputStream());

                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                SendMessage();

            }//run method...
        }).start();//Thread 실행..
    }




    public void SendMessage() {
        if(os==null) return;   //서버와 연결되어 있지 않다면 전송불가..

        //네트워크 작업이므로 Thread 생성
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        //os.writeUTF(msg);
                        //os.flush();
                        byte[] sisi = getByte(data.length);
                        os.write(sisi,0,sisi.length);
                        os.flush();
                        os.write(data,0,data.length);
                        os.flush();


                        socket2.close();
                        break;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }//run method..

        }).start(); //Thread 실행..
    }

*/

}
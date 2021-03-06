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

    //?????? ??????
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mPersonRef, mFallRef, mPhotoRef,mTimeRef; //?????? 4??? ??????
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
    private Mat frame; //openCV?????? ?????? data type, Matrix ???????????????
    // Mat ????????? 2?????? ?????? VS blob ????????? ????????? ??????
    // openCV ????????? blob ??? Mat ????????? 4?????? ????????? ?????????!
    List<String> outBlobNames;
    Bitmap bmp;
    //??????????????? ?????????, ???????????????, ????????????
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


        //????????? ????????????
        if (!permissionGranted) {
            checkPermissions();
        }
        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setMaxFrameSize(416,416);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        classNames = readLabels("labels.txt", this); //???????????? ????????? ????????????????????? ?????????
        for (int i = 0; i < classNames.size(); i++)
            colors.add(randomColor());
        Log.d("jina", classNames.toString()); //?????? ??? ???????????? ??? ??????
        //?????? (???) ?????????
        Log.d("???jina", "??????"+p1[0]+p1[1]);


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
    //???????????? ??????
    @Override
    public void onCameraViewStarted(int width, int height) {

        String modelConfiguration = getAssetsFile("yolov3-tiny.cfg", this);
        String modelWeights = getAssetsFile("yolov3-tiny.weights", this); //????????? ??????.
        net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights); //????????? ??????(coco dataset)??? ?????????.
    }
    @Override
    public void onCameraViewStopped() {
    }
    //????????? ?????? & ????????? ?????? ?????? ??????
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) { //???????????? frame ?????? Mat ?????? ??????

        //????????? & ????????? ?????? ?????????
        frame = inputFrame.rgba();
        Log.d("kfkf","df"+frame.height()+frame.width()); //1080, 1920
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // frame_size ??? 32?????? ???????????? ?????? ?????? ????????????, ????????? ????????? ????????????, ???????????? ????????????
        Size frame_size = new Size(256, 256);
        Scalar mean = new Scalar(127.5);
        //???????????????
        Imgproc.line(frame, new Point(p1[0],p1[1]), new Point(p2[0],p2[1]), new Scalar(0,0,255), 7, 2);
        // frame ?????????  -> ????????? ????????????
        // blob ??? Mat ????????? 4?????? ??????. ??? ????????? NCHW ????????? ????????????.
        // N ??? ????????????, C??? ????????????, H,W??? ?????? ????????? ??????, ?????? ?????? !
        Mat blob = Dnn.blobFromImage(frame, 1.0 / 255.0, frame_size, mean, true, false);
        //save_mat(blob); //??????????????? ?????? !!!
        net.setInput(blob); //????????? ????????? ?????? ??????????????? ??????!

        List<Mat> result = new ArrayList<>(); // ???????????? frame?????? ?????? detect??? ?????????!
        outBlobNames = net.getUnconnectedOutLayersNames(); //?????? layer ??? ????????? ????????????.

        //???????????? ??????.
        //????????? outBlobNames ??? layer??? ????????? ???????????? ?????? net ??? forward ??? ??????
        net.forward(result, outBlobNames); //?????? ???????????? ??????16, ?????? 23??? ?????? -> ??????? 23???????????? ???????????????..?
        float confThreshold = 0.5f;
        //????????????, label ????????? ~~
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
                    //x????????? 2220 y ????????? 1080???
//mLocked.displayWidth 1440, mLocked.displayHeight 2960

                    //??????????????? 4??? ??????
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
                            if(!isfall) {//?????? ????????????????????????

                                Log.d("point", classNames.get(class_id) + " detect");
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        final Toast toast = Toast.makeText(getApplicationContext(), classNames.get(class_id) + " is fall", Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                });
                                save_mat(frame); //????????????
                                isfall = true; //?????????????????? ??????.

                            }
                        }
                        else
                            isfall = false; //?????? ???????????? ???????????? ????????????
                    }
                    //??????????????? ?????? width ??? 1080, height??? 2220
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // final Toast toast = Toast.makeText(getApplicationContext(), label + "\n" + left_top.toString() + "\n" + right_bottom.toString(), Toast.LENGTH_SHORT);
                            // toast.show();
                        }
                    });
                    //???????????? ???????????? ?????? ??????????????????
                    Scalar color = colors.get(class_id);
                    //????????? ?????? ????????? !
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


    //labels.txt???????????? ???????????? ?????????
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
        //????????? ?????????
        Log.d("thisthis","falfkjalksjdf");
        int num =2;

        bmp = Bitmap.createBitmap((mat.width()), mat.height(), Bitmap.Config.ARGB_8888);
        Mat tmp = new Mat(mat.width(), mat.height(), CvType.CV_8UC1, new Scalar(4));
        //Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGB2GRAY);//???????????? ????????????
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
    //???????????? byte????????? ?????????
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

    //????????? byte????????? ?????????
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

    //???????????? ?????????
    void connect(){
        mHandler = new Handler(Looper.getMainLooper());
        Log.w("connect","?????? ?????????");
        // ???????????????
        Thread checkUpdate = new Thread() {
            public void run() {
                // ?????? ??????
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

    //??????
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
//        // ????????? ????????? ????????? ?????????
//        sdfNow = new SimpleDateFormat("MM/dd HH:mm");
//        // nowDate ????????? ?????? ????????????.
//        formatDate = sdfNow.format(date);
//        //????????? ????????? ?????????
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
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//
//
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
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
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//        mRootRef.child("person_list").push().setValue("2@1@????????????");
//
//
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//        mRootRef.child("person_list").push().setValue("2@2@????????????");
//
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//        mRootRef.child("person_list").push().setValue("2@3@????????????");
//
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//        mRootRef.child("person_list").push().setValue("2@4@????????????");
//
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//        mRootRef.child("person_list").push().setValue("2@5@?????????");
//
//
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
//        mRootRef.child("person_list").push().setValue("2@6@????????????");
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
                    String ip = "192.168.35.23";//IP ????????? ???????????? ?????? EditText?????? ?????? IP ????????????
                    String port = "5001";
                    if(ip.isEmpty() || port.isEmpty()){


                    }else {
                        //????????? ???????????? ?????? ??????..
                        socket2 = new Socket(InetAddress.getByName(ip), Integer.parseInt(port));
                        //???????????? ????????? ?????? ????????? ???????????? ???????????? ???????????? ?????? ?????? ??????..

                        //????????? ???????????? ???????????? ?????? ??????
                        //   is = new DataInputStream(socket.getInputStream());
                        os = new DataOutputStream(socket2.getOutputStream());

                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                SendMessage();

            }//run method...
        }).start();//Thread ??????..
    }




    public void SendMessage() {
        if(os==null) return;   //????????? ???????????? ?????? ????????? ????????????..

        //???????????? ??????????????? Thread ??????
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

        }).start(); //Thread ??????..
    }

*/

}
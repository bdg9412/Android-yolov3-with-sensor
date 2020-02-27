package com.example.myapplication;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.*;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;


import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import android.os.*;

import android.os.Environment;
import android.os.Bundle;
import android.view.View;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;






public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean startYolo = false;
    boolean firstTimeYolo = false;
    Net tinyYolo;

    //안드로이드 센서
    //https://m.blog.naver.com/PostList.nhn?blogId=roboholic84 참고
    //센서관리하는 모듈, 각 센서를 선언하거나 센서값을 내보낸다.
    private SensorManager sensorManager;
    private Sensor linearAccelerSensor;
    private Sensor GyroscopeSensor;

    //SensorEventListener를 선언
    //listener는 센서 개수와 상관없이 한번만 등록하면 된다.
    private SensorEventListener mAccLis;

    //센서에 사용할 변수 선언
    private float lAccX,lAccY,lAccz;
    private float RotX,RotY;
    private float alpha = 0.8f;
    //X축과 Y축 변화 확인을 위한 Textview 설정
    TextView tvlXaxis, tvlYaxis;
    //X축과 Y축의 속도 변화 확인을 위한 Textview 설정
    TextView acclXaxis, acclYaxis;
    //X축과 Y축의 각도 변화 확인을 위한 Textview 설정
    TextView RotXaxis;
    //Z축 이동 변화 확인을 위한 Textview 설정
    TextView Move;

    //시간
    private static final float NS2S = 1.0f/1000000000.0f;
    private double timestamp;
    private double dt;
    // for radian -> dgree, 180/pi
    private double RAD2DGR = 180 / Math.PI;

    //Roll and Pitch
    private double pitch;
    private double roll;
    private double yaw;

    //box.tl().x값 저장 //box.br().x값 저장
    private double x;
    private double x_br;

    //비교값
    private double tempx;
    private double default_x=0;

    //box.tl().y값 저장, box.br().y값 저장
    private double y;
    private double y_br;

    //비교값
    private double tempy;
    private double default_y=0;

    //box크기 구하기
    private double sqrb;
    private double default_sqr=0;

    //Z축 operation
    private double op;

    //Z축 비교값
    private double sqrb_temp=0;

    //참고: https://ghj1001020.tistory.com/307
    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TestLog";
    final static String filename = "YHK-handnavi-dongekun.txt";

    //txt로 저장할 변수
    String tvlXaxis_txt;
    String tvlYaxis_txt;
    String Move_txt;
    double roll_txt;

    String bowl;
    String null_sample="null";
    //참고 https://devfarming.tistory.com/3
    //참고 https://itmining.tistory.com/16
    //MainThread가 아닌 다른 Thread에서 UI를 변경하려 하면 오류가 발생
    //그럼으로 Handler를 이용하여 두개의 서로 다른 쓰레드를 연결할 수 있도록 함
    //안드로이드에선 메인스레드와 서브스레드 간에 Handler를 통해 메시지를 전달하여 메시지 큐에 저장하는 방식의 통신을 사용
    //Handler는 해당 Handler를 호출한 스레드의 MessageQueue와 Looper에 자동 연결된다.
    //임시로 MainThread를 사용할 수 있도록 해준다.
    final Handler handler = new Handler(){
        public void handleMessage(Message msg){
            //이곳에 변경할 UI행동을 넣어줌으로써 동작을 한다.
            //x축
            if(tempx>0) {
                tvlXaxis.setText("X axis: " + "Left");
                tvlXaxis_txt="Left";
                System.out.println("Left");
            }
            else{
                tvlXaxis.setText("X axis: " + "Right");
                tvlXaxis_txt="Right";
            }
            //y축
            if(tempy>0) {
                tvlYaxis.setText("Y axis: " + "Up");
                tvlYaxis_txt="Up";
            }
            else{
                tvlYaxis.setText("Y axis: " + "Down");
                tvlYaxis_txt="Down";
            }
            //z축
            if(op==1){
                Move.setText("Move: " + "Go");
                Move_txt="Go";
            }
            else if (op==2){
                Move.setText("Move:" + "Back");
                Move_txt="Back";
            }

            RotXaxis.setText("X axis Rot: " + String.format("%.2f",  roll*RAD2DGR));
            roll_txt=roll*RAD2DGR;

        }
    };

    public void YOLO(View Button){

        //YOLO 버튼을 누르면 startYolo가 true가 되어 detection 진행
        //버튼을 누르면 sensorManager등록, 이때 Listener로 this가 아닌 선언한 변수 mAccLis를 넣어줘야 에러 발생 없다.
        //Yolov3와 무관하게 실험값을 확인하기 위해 다른 버튼으로 작동하도록 만들기

        sensorManager.registerListener(mAccLis,linearAccelerSensor,SensorManager.SENSOR_DELAY_UI);
        //참고 https://mailmail.tistory.com/10
        //자이로 센서 등록
        sensorManager.registerListener(mAccLis,GyroscopeSensor,SensorManager.SENSOR_DELAY_UI);
        if (startYolo == false){

            startYolo = true;

            if (firstTimeYolo == false){

                firstTimeYolo = true;
                //opencv DNN.readNetFromDarknet을 사용하기 위해 string 인자를 두개(cfg,weight) 넘겨줘야 합니다.
                //getpath라는 임의의 함수를 이용하여 filepath를 string으로 저장합니다.
                String tinyYoloCfg = getPath("yolov3.cfg",this) ;
                String tinyYoloWeights = getPath("yolov3.weights",this);

                //opencv에서 제공하는 Dnn모델(Deep Neural Network)을 이용
                tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);

            }

        }

        else{

            startYolo = false;
            sensorManager.unregisterListener(mAccLis);
        }

    }

    //어플리케이션 부팅 후 적용되는 부분
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //x축, y축 xml접근
        tvlXaxis=(TextView)findViewById(R.id.tvlXaixs);
        tvlYaxis=(TextView)findViewById(R.id.tvlYaixs);
        //x축, y축 가속도 xml접근
        acclXaxis=(TextView)findViewById(R.id.acclXaixs);
        acclYaxis=(TextView)findViewById(R.id.acclYaixs);
        //x축, y축 각도 xml접근
        RotXaxis=(TextView)findViewById(R.id.RotXaixs);
        //z축 xml접근
        Move=(TextView)findViewById(R.id.Move);
        //디바이스에서 사용 가능한 센서 정보를 확인한다.
        //getDefaultSensor를 통해 사용할 센서를 선택할 수 있다.
        //SensorManager를 통한 가속도 센서 사용
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //linearAccelerSensor=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        linearAccelerSensor=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        GyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //SensorListener로 선언한 변수에 리스너 클래스를 할당
        mAccLis=new AccelometerListener();

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);


        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };


    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //inputframe중 rgba format을 프레임변수에 할당
        Mat frame = inputFrame.rgba();
        //Mat to Bitmap으로 변환
        Mat frame2 = frame;
        Bitmap bmp = null;
        try {
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(frame2.cols(), frame2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame2, bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}

        //YOLO 버튼 클릭으로 startYolo가 true로 바뀌었을 때
        if (startYolo == true) {
            //Imgproc을 이용해 이미지 프로세싱을 한다. rgba를 rgb로 컬러체계변환
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

            //출처 https://stackoverflow.com/questions/39411160/how-to-save-opencv-resultant-images-in-android-automatically
            //bmp파일을 이미지 폴더에 저장, 저장경로(내장메모리/Pictures)
            //MediaStore.Images.Media.insertImage(getContentResolver(),bmp,"t","hello");

            //blob이란 input image가  mean subtraction, normalizing, and channel swapping을 거치고 난 후를 말합니다.
            //Dnn.blobFromImage를 이용하여 이미지 픽셀의 평균값을 계산하여 제외하고 스케일링을 하고 또 채널 스왑(RED와 BLUE)을 진행합니다.
            //현재는 128 x 128로 스케일링하고 채널 스왑은 하지 않습니다. 생성된 4-dimensional blob 값을 imageBlob에 할당합니다.
            //www.pyimagesearch.com/2017/11/06/deep-learning-opencvs-blobfromimage-works 참고하였습니다.
            Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(128,128),new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);


            tinyYolo.setInput(imageBlob);

            //cfg파일에서 yolo layer number를 확인하여 이를 순전파에 넣어줍니다.
            //yolov3의 경우 yolo layer가 3개임으로 initialCapacity를 3으로 줍니다.
            java.util.List<Mat> result = new java.util.ArrayList<Mat>(3);

            List<String> outBlobNames = new java.util.ArrayList<>();
            outBlobNames.add(0, "yolo_82");
            outBlobNames.add(1, "yolo_94");
            outBlobNames.add(2, "yolo_106");

            //순전파를 진행합니다.
            tinyYolo.forward(result,outBlobNames);

            //30%이상의 확률만 출력해주겠다.
            float confThreshold = 0.3f;



            List<Integer> clsIds = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();




            for (int i = 0; i < result.size(); ++i)
            {

                Mat level = result.get(i);

                for (int j = 0; j < level.rows(); ++j)
                {
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());

                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);




                    float confidence = (float)mm.maxVal;


                    Point classIdPoint = mm.maxLoc;



                    if (confidence > confThreshold)
                    {
                        int centerX = (int)(row.get(0,0)[0] * frame.cols());
                        int centerY = (int)(row.get(0,1)[0] * frame.rows());
                        int width   = (int)(row.get(0,2)[0] * frame.cols());
                        int height  = (int)(row.get(0,3)[0] * frame.rows());


                        int left    = centerX - width  / 2;
                        int top     = centerY - height / 2;

                        clsIds.add((int)classIdPoint.x);
                        confs.add((float)confidence);




                        rects.add(new Rect(left, top, width, height));
                    }
                }
            }
            int ArrayLength = confs.size();

            if (ArrayLength>=1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;




                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));


                Rect[] boxesArray = rects.toArray(new Rect[0]);

                MatOfRect boxes = new MatOfRect(boxesArray);

                MatOfInt indices = new MatOfInt();



                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);


                // Draw result boxes:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {

                    int idx = ind[i];
                    Rect box = boxesArray[idx];

                    int idGuy = clsIds.get(idx);

                    float conf = confs.get(idx);


                    List<String> cocoNames = Arrays.asList("a person", "a bicycle", "a motorbike", "an airplane", "a bus", "a train", "a truck", "a boat", "a traffic light", "a fire hydrant", "a stop sign", "a parking meter", "a car", "a bench", "a bird", "a cat", "a dog", "a horse", "a sheep", "a cow", "an elephant", "a bear", "a zebra", "a giraffe", "a backpack", "an umbrella", "a handbag", "a tie", "a suitcase", "a frisbee", "skis", "a snowboard", "a sports ball", "a kite", "a baseball bat", "a baseball glove", "a skateboard", "a surfboard", "a tennis racket", "a bottle", "a wine glass", "a cup", "a fork", "a knife", "a spoon", "a bowl", "a banana", "an apple", "a sandwich", "an orange", "broccoli", "a carrot", "a hot dog", "a pizza", "a doughnut", "a cake", "a chair", "a sofa", "a potted plant", "a bed", "a dining table", "a toilet", "a TV monitor", "a laptop", "a computer mouse", "a remote control", "a keyboard", "a cell phone", "a microwave", "an oven", "a toaster", "a sink", "a refrigerator", "a book", "a clock", "a vase", "a pair of scissors", "a teddy bear", "a hair drier", "a toothbrush");



                    int intConf = (int) (conf * 100);


                    //opencv의 이미지 프로세싱을 진행합니다.
                    //putText를 이용하여 label의 이름을 입력하여 줍니다.
                    Imgproc.putText(frame,cocoNames.get(idGuy) + " " + intConf + "%",box.tl(),Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,255,0),2);

                    //opencv의 이미지 프로세싱을 진행합니다.
                    //rectangle을 이용하여 사각형을 그려줍니다.
                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);


                    //참고 https://devfarming.tistory.com/3
                    if(cocoNames.get(idGuy)=="a laptop"){

                        x = box.tl().x;
                        tempx=x-default_x;
                        System.out.println("x좌표"+x);
                        y = box.tl().y;
                        tempy=y-default_y;
                        System.out.println("y좌표"+y);
                        //br() 포인터의 두 값 할당
                        x_br = box.br().x;
                        y_br = box.br().y;
                        //box 절대값 크기 구하기
                        sqrb=Math.abs((x-x_br)*(y-y_br));

                        System.out.println(sqrb);
                        if(sqrb_temp!=0 && default_sqr<sqrb){
                            op=1;
                            System.out.println("GO");
                        }
                        else if(sqrb_temp!=0 && default_sqr>sqrb){
                            op=2;
                            System.out.println("back");
                        }
                        sqrb_temp=sqrb-default_sqr;
                         //Handler를 이용하여 변화를 줄 때 불러오는 부분
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                        String now = new SimpleDateFormat("yyyy.MMddHHmmss").format(new Date());

                        //전체 명령어를 띄워주는 부분
                        String contents = "Log 생성 : " + tvlXaxis_txt + " " + now + " " + tvlYaxis_txt + " " + Move_txt + " " + roll_txt;

                        default_x=x;
                        default_y=y;
                        default_sqr=sqrb;

                        //이 부분을 통해서 frame별 사진 저장
                        MediaStore.Images.Media.insertImage(getContentResolver(),bmp,"IMAGE","IMAGE");

                    }

                }
            }


        }



        return frame;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {


        if (startYolo == true){

            String tinyYoloCfg = getPath("yolov3.cfg",this) ;
            String tinyYoloWeights = getPath("yolov3.weights",this);

            tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);


        }



    }

    // Upload file to storage and return a path.
    // YOLO의 cfg와 weight를 불러오기 위한 코드입니다.
    // asset 폴더를 읽어오는 과정에서 string이 아닌 inputstream으로 받아오기에 이를 다시 string으로 변환해줍니다.
    // https://recipes4dev.tistory.com/125을 참고하여 asset폴더를 생성하고 yolo 모델 파일을 저장하였습니다.
    // https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html에서 getpath함수를 참고하였습니다.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
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
            ex.printStackTrace();
        }
        return "";
    }


    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
        sensorManager.unregisterListener(mAccLis);


    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
        sensorManager.unregisterListener(mAccLis);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    //텍스트내용을 경로의 텍스트 파일에 쓰기
    public void WriteTextFile(String foldername, String filename, String contents) {
        try {
            File dir = new File(foldername);
            //디렉토리 폴더가 없으면 생성함
            if (!dir.exists()) {
                dir.mkdir();
            }
            //파일 output stream 생성
            FileOutputStream fos = new FileOutputStream(foldername + "/" + filename, true);
            //파일쓰기
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(contents);
            writer.flush();

            writer.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //sensor가 변경되었을 때를 관리하는 부분
    private  class AccelometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {

            // dt : 센서가 현재 상태를 감지하는 시간 간격
            //NS2S : nano second -> second */
            dt = (event.timestamp - timestamp) * NS2S;
            timestamp = event.timestamp;

            if (event.sensor == linearAccelerSensor) {
                lAccX = event.values[0];
                lAccY = event.values[1];
                lAccz = event.values[2];

                acclXaxis.setText("X axis Acc: " + String.format("%.2f", lAccX*10));
                acclYaxis.setText("Y axis Acc: " + String.format("%.2f", lAccY*10));
            }
            if (event.sensor == GyroscopeSensor) {

                if (dt - timestamp*NS2S != 0) {
                    RotX = event.values[2];
                    //System.out.println("x" + RotX);
                    RotY = event.values[1];
                    //System.out.println("y" + RotY);
                    pitch = pitch + RotY * dt;
                    //System.out.println("pitch" + pitch*RAD2DGR);
                    roll = roll + RotX * dt;
                    //System.out.println("roll" + roll*RAD2DGR);
                    //yaw = yaw + gyroZ*dt;

                    //RotXaxis.setText("X axis Rot: " + String.format("%.2f",  roll*RAD2DGR));
                }
            }

        }
        //이 부분 작성하지 않을 시
        //Class must either be declared abstract or implement abstract method error 발생
        //apphappy.tistory.com/40 참고
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
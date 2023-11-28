/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import static android.speech.tts.TextToSpeech.ERROR;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.altbeacon.beacon.Beacon;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.navi.Bluetooth;
import org.tensorflow.lite.examples.detection.navi.user_orientation;
import org.tensorflow.lite.examples.detection.navi.wifi;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import android.os.VibrationEffect;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener{


    //Navi Parameters
    private SensorManager sm;
    private WifiManager wfm;
    private Sensor gyro;
    private Sensor mag;
    private Sensor grv;
    private user_orientation user_ori;
    private Drawable drawable;
    private Resources res;

    private int test = 0;
    Bluetooth ble;

    int[] nop_per_section = new int [] {5, 3, 4};
    int [] init_nop = new int[]{0,5,8};

    private boolean explaining = false;
    //navigation

    private static final Logger LOGGER = new Logger();

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    protected boolean isDetectPerson = false;
    protected long PersonDetectTime = 0;
    protected final long PERSON_DETECT_INTERVAL_TIME = 3000;
    protected long[] pattern = {100,1000,1000,1000};
    protected int[] amplitudes={0,100,0,200};
    protected String title_Start_String = "";
    protected String title_End_String = "";

    protected String detect_person_String = "";
    protected TextToSpeech tts;
    protected Vibrator vibrator;
    protected ArrayList<ArrayList> QRData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        TextView tv = findViewById(R.id.tv_sector);
        TextView tv_section = findViewById(R.id.tv_section);
        Button start_s1 = findViewById(R.id.bt_start_s1);
        Button stop_s1 = findViewById(R.id.bt_stop_s1);

        Button start_s2 = findViewById(R.id.bt_start_s2);
        Button stop_s2 = findViewById(R.id.bt_stop_s2);

        Button start_s3 = findViewById(R.id.bt_start_s3);
        Button stop_s3 = findViewById(R.id.bt_stop_s3);

        Button bt_test = findViewById(R.id.bt_test);



        ImageView[] maps_sector1 = new ImageView[48];
        maps_sector1[0] = findViewById(R.id.s_1);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setLanguage(Locale.KOREAN);
            }
        });
        tts.setPitch(1.0f);


        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //grv = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        res = getResources();
        wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi WiFi = new wifi(wfm, this, tv_section);
        WiFi.start();


        user_ori = new user_orientation(sm, gyro, mag, tts);


        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        ble = new Bluetooth(adapter, tv, maps_sector1, user_ori, tts);


        start_s1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak("시스템을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                //user_ori.set_correction(90);
                ble.start( "0");
                //ble.start(sector2,"2",p_loc_2,corner_2);
                //ble.start(sector3,"3",p_loc_3,corner_3);
            }
        });

        stop_s1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.stop();
            }
        });

        start_s2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.start("1");
            }
        });

        stop_s2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.stop();
            }
        });

        start_s3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak("시스템을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                ble.start("2");
            }
        });

        stop_s3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble.stop();
            }
        });

        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test++;
            }
        });
    }

    public void setExplaining(boolean tf){
        explaining = tf;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void updateActiveModel() {
        // Get UI information before delegating to background
        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        //String threads = threadsTextView.getText().toString().trim();
        final int numThreads = 1; //Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;


            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
                detector.useCPU();
            } else if (device.equals("GPU")) {
                detector.useGpu();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    public void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setPitch(1.0f); //음성톤 기본 설정
                    tts.setSpeechRate(1f); //읽는 속도 0.5배 빠르게
                }
            }
        });

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        QRData = getQRData();
    }

    @Override
    protected void onBeaconDetected(Beacon beacon) {
        // 비콘 정보를 받아 처리하는 로직을 여기에 구현

        double distance = calculateDistance(beacon.getTxPower(), beacon.getRssi());

        Toast.makeText(
                        this,
                        beacon.getId1() + ":" + Double.toString(distance),
                        Toast.LENGTH_SHORT)
                .show();
    }

    private String getJsonString()
    {
        String json = "";

        try {
            InputStream is = getAssets().open("EnvInfo.json");
            int fileSize = is.available();

            byte[] buffer = new byte[fileSize];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        return json;
    }

    private String[] jsonParsing()
    {
        int all = 0;
        for(int i = 0; i < nop_per_section.length; i++)
            all+= nop_per_section[i];
        String[] URLList = new String[all];
        for(int i = 0; i < all; i++)
            URLList[i] = "";

        String getJsonStr = getJsonString();
        try{
            JSONObject jsonObject = new JSONObject(getJsonStr);


            int idNum = 0;
            JSONArray urlList = jsonObject.getJSONArray("QRCode");

            for(int i=0; i<urlList.length(); i++)
            {
                JSONObject urlObject = urlList.getJSONObject(i);
                idNum = Integer.valueOf(urlObject.getString("id"));

                if(idNum > 0 && idNum <= 20){
                    URLList[i] = urlObject.getString("Url");

                }
                else{
                    //LOGGER.e("Json ID Error " + idNum);
                }
            }
            JSONArray VoiceStr = jsonObject.getJSONArray("Voice");
            JSONObject voiceObject = VoiceStr.getJSONObject(0);
            title_Start_String =voiceObject.getString("titleS");
            title_End_String = voiceObject.getString("titleE");

            JSONArray detectStr = jsonObject.getJSONArray("PersonDetect");
            JSONObject personObject = detectStr.getJSONObject(0);
            detect_person_String =personObject.getString("detect");

        }catch (JSONException e) {
            e.printStackTrace();
        }
        return URLList;
    }

    /*
     * QR Code Image에 대한 String을 미리 읽어오는 부분
     * */
    protected ArrayList<ArrayList> getQRData(){
        String[] urlList = jsonParsing();

        ArrayList<ArrayList> res = new ArrayList<ArrayList>();

        Document doc = null;

        try{
            for(String url:urlList) {
                res.add(new GetImageInfo().execute(url).get());
            }
        }catch(ExecutionException | InterruptedException e){
            e.printStackTrace();
        }
        return res;
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        long tempTime = System.currentTimeMillis();
        trackingOverlay.postInvalidate();


        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        int[] maxSize = {0, 0};
                        int size, r = 0;
                        for (Classifier.Recognition result : results) {
                            RectF location = result.getLocation();
                            size = (int)(location.height() * location.width());
                            if (size > maxSize[1]) {
                                maxSize[0] = r;
                                maxSize[1] = size;
                            }
                            r++;
                        }
                        if (results.size() > 0) {
                            Classifier.Recognition result = results.get(maxSize[0]);
                            int classIndex = result.getDetectedClass();
                            Log.e("CHECK", "getConfidence: "  +Float.toString(result.getConfidence()));
                            if (classIndex <= 1 && result.getConfidence() > 0.6) {  //신뢰도 0.6 이상만.
                                RectF location = result.getLocation();
                                if (location != null && result.getConfidence() >= minimumConfidence) {
                                    canvas.drawRect(location, paint);
                                    findInfo = location.toString();
                                    cropToFrameTransform.mapRect(location);
                                    result.setLocation(location);
                                    mappedRecognitions.add(result);
                                    Log.e("CHECK", "isDetectPerson: "  +Long.toString(tempTime - PersonDetectTime));
                                    Log.e("CHECK", "result confidence: " + result.getConfidence().toString());
                                    if ((tempTime - PersonDetectTime) >= PERSON_DETECT_INTERVAL_TIME) {
                                        PersonDetectTime = tempTime;
                                        tts.speak(detect_person_String, TextToSpeech.QUEUE_FLUSH, null, null);
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, -1);
                                            vibrator.vibrate(vibrationEffect);
                                        }

                                    }
                                }
                           }
                            else if(!ble.getchangable() && explaining) {
                                new Thread(() -> {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                //tts.speak(String.valueOf(ble.getsection()) + "번 전시관입니다.", TextToSpeech.QUEUE_FLUSH, null);
                                                //tts.speak(String.valueOf(ble.num_pic()),TextToSpeech.QUEUE_FLUSH,null);
                                                //title 알아오기
                                                String title = (String) QRData.get(init_nop[ble.getsection()] + ble.num_pic()).get(0);
                                                //내용 알아오기
                                                String desc = (String) QRData.get(init_nop[ble.getsection()]+ ble.num_pic()).get(1);

                                                Log.e("section_check", String.valueOf(init_nop[ble.getsection()] + ble.num_pic()));
                                                Log.e("section_check", title);
                                                Log.e("section_check_pic", desc);
                                                tts.speak(title, TextToSpeech.QUEUE_FLUSH, null, null);
                                                Thread.sleep(2000);
                                                //tts.speak(desc, TextToSpeech.QUEUE_FLUSH, null, null);
                                                explaining = false;
                                                while(true){
                                                    if(ble.getchangable()) {
                                                        tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                                                        break;
                                                    }
                                                }
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }).start();

                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        //showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showFindInfo(findInfo);
                                    }
                                });
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }
}

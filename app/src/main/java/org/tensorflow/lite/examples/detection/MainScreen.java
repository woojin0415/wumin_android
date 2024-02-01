package org.tensorflow.lite.examples.detection;

import static android.speech.tts.TextToSpeech.ERROR;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.ImageReader;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.Beacon;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.layout.squarebutton;
import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.navi.Bluetooth;
import org.tensorflow.lite.examples.detection.navi.user_orientation;
import org.tensorflow.lite.examples.detection.navi.wifi;
import org.tensorflow.lite.examples.detection.storage.BLEStorage;
import org.tensorflow.lite.examples.detection.storage.DetectorStorage;
import org.tensorflow.lite.examples.detection.storage.OrientationStorage;
import org.tensorflow.lite.examples.detection.storage.StoreManagement;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainScreen extends CameraActivity implements ImageReader.OnImageAvailableListener {

    //Navi Parameters
    private SensorManager sm;
    private WifiManager wfm;
    private Sensor gyro;
    private Sensor mag;
    private Sensor rotation;
    private user_orientation user_ori;
    private boolean start = false;
    Bluetooth ble;
    private TextToSpeech tts;


    private boolean click;
    private boolean detect_start = false;



    private static final Logger LOGGER = new Logger();

    //private static final DetectorActivity.DetectorMode MODE = DetectorActivity.DetectorMode.TF_OD_API;
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
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
    protected final long PERSON_DETECT_HEAD_SIZE_2M = 0;

    protected String detect_person_String = "사람이 전방에 있습니다.";
    protected Vibrator vibrator;
    protected ArrayList<ArrayList> QRData;

    //Storage
    private DetectorStorage[] detect_storage;
    private int detector_storage_count;
    private StoreManagement store_m;


    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout b_layout = findViewById(R.id.button_layout);
        squarebutton bt_main = findViewById(R.id.bt_main);
        ViewGroup.LayoutParams params = b_layout.getLayoutParams();
        int width = params.width;
        params.height = width;
        Log.e("Layout", String.valueOf(width) + "/" + String.valueOf(params.height));
        b_layout.setLayoutParams(params);


        //try {
        //    store_m = new StoreManagement();
        //} catch (IOException e) {
        //    throw new RuntimeException(e);
        //}

        TextView tv = findViewById(R.id.tv_position);


        click = false;

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mag = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi WiFi = new wifi(wfm, this);

        user_ori = new user_orientation(sm, gyro, mag, rotation, tts, store_m);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        ble = new Bluetooth(adapter, user_ori, tts, store_m, tv);
        user_ori.set_ble(ble);



        //detect_storage = new DetectorStorage[10000];



//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    int i = 0;
//                    while(true) {
//                        if(click) {
//                            b_layout.setRotation(i);
//                            bt_main.setRotation(-i);
//                            Thread.sleep(10);
//                            i++;
//                        }
//
//                    }
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }).start();


        bt_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                click = !click;
                if(!start){

                    ble.start();

                    tts.speak("방향 설정을 시작합니다.", TextToSpeech.QUEUE_FLUSH, null);
                    start = true;
                    detect_start = true;


                    user_ori.start();
                    //WiFi.start();

                    //store_m.reset_detector(detect_storage);
                    //detector_storage_count = 0;

                }
                else {
                    start = false;
                    detect_start = false;
                    try {
                        ble.stop();
                        user_ori.stop();
                        //store_m.detector_store(detect_storage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = 0; //modelView.getCheckedItemPosition();
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
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
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
        final int modelIndex = 0; //modelView.getCheckedItemPosition();
        //final int deviceIndex = deviceView.getCheckedItemPosition();
        //String threads = threadsTextView.getText().toString().trim();
        final int numThreads = 1; //Integer.parseInt(threads);

        handler.post(() -> {
           // if (modelIndex == currentModel/* && deviceIndex == currentDevice*/) {
           //     return;
           // }
            //currentModel = modelIndex;
            //currentDevice = deviceIndex;


            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            //String device = deviceStrings.get(deviceIndex);

            //LOGGER.i("Changing model to " + modelString + " device " + device);

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
//                LOGGER.e(e, "Exception in updateActiveModel()");
//                Toast toast =
//                        Toast.makeText(
//                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
//                toast.show();
                finish();
            }


            //if (device.equals("CPU")) {
                detector.useCPU();
            //} else if (device.equals("GPU")) {
            //    detector.useGpu();
           // } else if (device.equals("NNAPI")) {
            //    detector.useNNAPI();
           // }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

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
    }

    @Override
    protected void onBeaconDetected(Beacon beacon) {
        // 비콘 정보를 받아 처리하는 로직을 여기에 구현

        double distance = calculateDistance(beacon.getTxPower(), beacon.getRssi());
        /* 아직은 필요가 없음..
        Toast.makeText(
                        this,
                        beacon.getId1() + ":" + Double.toString(distance),
                        Toast.LENGTH_SHORT)
                .show();

         */
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
                        if (true) {
                            LOGGER.i("Running detection on image " + currTimestamp);
                            final long startTime = SystemClock.uptimeMillis();
                            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                            Log.e("CHECK", "run: " + results.size());

                            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                            final Canvas canvas = new Canvas(cropCopyBitmap);
                            final Paint paint = new Paint();
                            paint.setColor(Color.RED);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(2.0f);

                            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                            //switch (MODE) {
                            //    case TF_OD_API:
                            minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                            //         break;
                            // }

                            final List<Classifier.Recognition> mappedRecognitions =
                                    new LinkedList<Classifier.Recognition>();

                            int[] maxSize = {0, 0};
                            int size, r = 0;
                            for (Classifier.Recognition result : results) {
                                RectF location = result.getLocation();
                                size = (int) (location.height() * location.width());
                                if (size > maxSize[1]) {
                                    maxSize[0] = r;
                                    maxSize[1] = size;
                                }
                                r++;
                            }
                            if (results.size() > 0) {
                                Classifier.Recognition result = results.get(maxSize[0]);
                                int classIndex = result.getDetectedClass();
                                //Log.e("CHECK", "getConfidence: " + Float.toString(result.getConfidence()));
                                if (classIndex <= 1 && result.getConfidence() >= 0.6) {  //신뢰도 0.6 이상만.
                                    RectF location = result.getLocation();

                                    //float[] location_data = new float[]{location.right, location.left, location.top, location.bottom};
                                    //long detect_time = System.currentTimeMillis();
                                    //String detect = "";

                                    if (location != null) {
                                    //    detect = "T";
                                        if (((location.right - location.left) * (location.bottom - location.top)) >= PERSON_DETECT_HEAD_SIZE_2M) {
                                            //canvas.drawRect(location, paint);
                                            findInfo = location.toString() + " : " + Float.toString(((location.right - location.left)
                                                    * (location.bottom - location.top)));
                                            cropToFrameTransform.mapRect(location);
                                            result.setLocation(location);
                                            mappedRecognitions.add(result);
                                            //Log.e("CHECK", "isDetectPerson: " + Long.toString(tempTime - PersonDetectTime));
                                            //Log.e("CHECK", "result confidence: " + result.getConfidence().toString());
                                            if ((tempTime - PersonDetectTime) >= PERSON_DETECT_INTERVAL_TIME) {
                                                PersonDetectTime = tempTime;
                                                //tts.speak(detect_person_String, TextToSpeech.QUEUE_ADD, null, null);
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, -1);
                                                    vibrator.vibrate(vibrationEffect);
                                                }

                                            }
                                        } else {
                                        //    detect = "F";
                                            findInfo = "No " + location.toString() + " : " + Float.toString(((location.right - location.left)
                                                    * (location.bottom - location.top)));
                                        }

                                        //DetectorStorage detector_dataset = new DetectorStorage();
                                        //detector_dataset.set_values(detect_time, detect, location_data);
                                        //detect_storage[detector_storage_count++] = detector_dataset;

                                        //if (detector_storage_count == detect_storage.length) {
                                        //    try {
                                        //        store_m.detector_store(detect_storage);
                                        //        store_m.reset_detector(detect_storage);
                                        //        detector_storage_count = 0;
                                        //    } catch (IOException e) {
                                        //        throw new RuntimeException(e);
                                        //    }
                                        //}


                                    }
                                    //findinfo 저장
                                }
                            }

                            tracker.trackResults(mappedRecognitions, currTimestamp);
                            trackingOverlay.postInvalidate();

                            computingDetection = false;

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            //showFrameInfo(previewWidth + "x" + previewHeight);
                                            //showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                            showFindInfo(findInfo);
                                        }
                                    });
                        }
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

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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.tensorflow.lite.examples.detection.beacon.BeaconAdvertisingHandler;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        BeaconConsumer,
//        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String ASSET_PATH = "";
  protected int previewWidth = 0;
  protected int previewHeight = 0;

  protected String findInfo = "";
  private boolean debug = false;
  protected Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  protected int defaultModelIndex = 0;


  protected int defaultDeviceIndex = 0;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  protected ArrayList<String> modelStrings = new ArrayList<String>();

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  protected TextView findAreaTextView;
  protected ImageView bottomSheetArrowImageView;
  //protected ListView deviceView;
  protected Button setCapture;
  //protected ListView modelView;
  /** Current indices of device and model. */
  //int currentDevice = -1;
  //int currentModel = -1;

  //ArrayList<String> deviceStrings = new ArrayList<String>();


  protected final long FINISH_INTERVAL_TIME = 2000;
  protected long backPressedTime = 0;

  private static final int LOCATION_PERMISSION_CODE = 100;
  private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 200;

  private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 300;
  private static final int BLUETOOTH_CONNECT_PERMISSION_CODE = 400;
  private static final int BLUETOOTH_ADVERTISE_PERMISSION_CODE = 500;

  private BeaconAdvertisingHandler advertisingHandler;
  private BeaconManager beaconManager;
  private Beacon lastDetectedBeacon; // 추가: 최근에 감지된 비콘을 저장할 변수

  @Override
  public void onBackPressed() {
    long tempTime = System.currentTimeMillis();
    long intervalTime = tempTime - backPressedTime;

    if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime)
    {
      finish();
    }
    else
    {
      backPressedTime = tempTime;
    }
  }


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.menu_screen);

    //Toolbar toolbar = findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);
    //getSupportActionBar().setDisplayShowTitleEnabled(false);


    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    setCapture = findViewById(R.id.capture);
    //deviceView = findViewById(R.id.device_list);
    //deviceStrings.add("CPU");
    //deviceStrings.add("GPU");
    //deviceStrings.add("NNAPI");
    //deviceView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    //ArrayAdapter<String> deviceAdapter =
    //        new ArrayAdapter<>(
    //                CameraActivity.this , R.layout.deviceview_row, R.id.deviceview_row_text, deviceStrings);
    //deviceView.setAdapter(deviceAdapter);
    //deviceView.setItemChecked(defaultDeviceIndex, true);
    //currentDevice = defaultDeviceIndex;
    /*
    deviceView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
              @Override
              public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateActiveModel();
              }
            });
    */
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
    //modelView = findViewById((R.id.model_list));

    modelStrings = getModelStrings(getAssets(), ASSET_PATH);
    //modelView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    //ArrayAdapter<String> modelAdapter =
    //        new ArrayAdapter<>(
    //                CameraActivity.this , R.layout.listview_row, R.id.listview_row_text, modelStrings);
    //modelView.setAdapter(modelAdapter);
    //modelView.setItemChecked(defaultModelIndex, true);
    //currentModel = defaultModelIndex;
    //modelView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id)->updateActiveModel());

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //                int width = bottomSheetLayout.getMeasuredWidth();
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);

    sheetBehavior.addBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
              case BottomSheetBehavior.STATE_HIDDEN:
                break;
              case BottomSheetBehavior.STATE_EXPANDED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                }
                break;
              case BottomSheetBehavior.STATE_COLLAPSED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                }
                break;
              case BottomSheetBehavior.STATE_DRAGGING:
                break;
              case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                break;
            }
          }

          @Override
          public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

    findAreaTextView = findViewById(R.id.detected_info);

    setCapture.setOnClickListener(this);

    initializeTTS();

  }

  @Override
  public void onBeaconServiceConnect() {
    beaconManager.addMonitorNotifier(new MonitorNotifier() {
      @Override
      public void didEnterRegion(Region region) {
        Log.d("Monitoring Beacon","비콘이 존재함......하나....");
      }

      @Override
      public void didExitRegion(Region region) {
        Log.d("Monitoring Beacon","비콘이 없음.........");
      }

      @Override
      public void didDetermineStateForRegion(int state, Region region) {
        if(state == 0)
          Log.d("Monitoring Beacon","비콘이 보임...."+state);
        else
          Log.d("Monitoring Beacon","비콘이 안보임...."+state);
      }
    });

    // 비콘을 감지하면 호출되는 콜백 처리
    beaconManager.addRangeNotifier(new RangeNotifier() {
      @Override
      public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        List<Beacon> list = (List<Beacon>)collection;
        if (list.size() > 0) {
          Beacon firstBeacon = list.iterator().next();
          lastDetectedBeacon = firstBeacon; // 최근에 감지된 비콘 저장
          if (lastDetectedBeacon != null) {
            onBeaconDetected(lastDetectedBeacon);
          }
        }
      }
    });

    beaconManager.startMonitoring(new Region("test", null, null, null));
    beaconManager.startRangingBeacons(new Region("test", null, null, null));
  }

  public void startBeaconService(){
    // 비콘 스캔을 위한 설정
    beaconManager = BeaconManager.getInstanceForApplication(this);
    beaconManager.bind(this);
    BeaconParser parser = new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);
    beaconManager.getBeaconParsers().add(parser);

    // HandlerThread 초기화 및 시작
    HandlerThread handlerThread = new HandlerThread("AdvertisingThread");
    handlerThread.start();

    // AdvertisingHandler 초기화
    advertisingHandler = new BeaconAdvertisingHandler(handlerThread.getLooper(), this); // 수정

    // 비콘 advertising 시작 메시지 전송
    advertisingHandler.sendEmptyMessage(BeaconAdvertisingHandler.MSG_START_ADVERTISING);

    if(!isBluetoothEnabled())
      Toast.makeText(
                      CameraActivity.this,
                      "Not isBluetoothEnabled",
                      Toast.LENGTH_LONG)
              .show();
  }

  private boolean isBluetoothEnabled() {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
  }

  protected void stopBeaconService() {
    // 액티비티가 종료되면서  종료
    beaconManager.unbind(this);
    // 액티비티 종료 시 AdvertisingHandler를 이용해 비콘 advertising 중지
    advertisingHandler.sendEmptyMessage(BeaconAdvertisingHandler.MSG_STOP_ADVERTISING);
  }
  protected ArrayList<String> getModelStrings(AssetManager mgr, String path){
    ArrayList<String> res = new ArrayList<String>();
    try {
      String[] files = mgr.list(path);
      for (String file : files) {
        String[] splits = file.split("\\.");
        if (splits[splits.length - 1].equals("tflite")) {
          res.add(file);
        }
      }
    }
    catch (IOException e){
      System.err.println("getModelStrings: " + e.getMessage());
    }
    return res;
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }


  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };
      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    //initializeTTS();
    checkPermission();
    startBeaconService();
  }

  private void checkPermission() {
    if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      // Fine Location permission is granted
      // Check if current android version >= 11, if >= 11 check for Background Location permission
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          // Ask for Background Location Permission
          askPermissionForBackgroundUsage();
        }
      }
    } else {
      // Fine Location Permission is not granted so ask for permission
      askForLocationPermission();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Permission Need!");
        builder.setMessage("Bluetooth scan Permission Need!");
        builder.setPositiveButton(android.R.string.ok, null);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

          @Override
          public void onDismiss(DialogInterface dialog) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, BLUETOOTH_SCAN_PERMISSION_CODE);
          }
        });
        builder.show();
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Permission Need!");
        builder.setMessage("Bluetooth connection Permission Need!");
        builder.setPositiveButton(android.R.string.ok, null);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

          @Override
          public void onDismiss(DialogInterface dialog) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_CONNECT_PERMISSION_CODE);
          }
        });
        builder.show();
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Permission Need!");
        builder.setMessage("Bluetooth advertise Permission Need!");
        builder.setPositiveButton(android.R.string.ok, null);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

          @Override
          public void onDismiss(DialogInterface dialog) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, BLUETOOTH_ADVERTISE_PERMISSION_CODE);
          }
        });
        builder.show();
      }
    }
  }

  private void askForLocationPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(CameraActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
      new AlertDialog.Builder(this)
              .setTitle("Permission Need!")
              .setMessage("Location Permission Need!")
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  ActivityCompat.requestPermissions(CameraActivity.this,
                          new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                }
              })
              .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  // Permission is denied by the user
                }
              })
              .create().show();
    } else {
      ActivityCompat.requestPermissions(CameraActivity.this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }
  }

  private void askPermissionForBackgroundUsage() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(CameraActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
      new AlertDialog.Builder(this)
              .setTitle("Permission Needed!")
              .setMessage("Background Location Permission Needed!")
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  ActivityCompat.requestPermissions(CameraActivity.this,
                          new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                }
              })
              .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  // User declined for Background Location Permission.
                }
              })
              .create().show();
    } else {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
    }
  }


  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }
    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
    stopBeaconService();
  }




  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch(requestCode){
      case PERMISSIONS_REQUEST:
        if (allPermissionsGranted(grantResults)) {
          setFragment();
        } else {
          requestPermission();
        }
        break;
      case LOCATION_PERMISSION_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // User granted location permission
          // Now check if android version >= 11, if >= 11 check for Background Location Permission
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              askPermissionForBackgroundUsage();
            }
          }
        }
        break;
      case BACKGROUND_LOCATION_PERMISSION_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // User granted for Background Location Permission.
        } else {
          // User declined for Background Location Permission.
        }
        break;
      case BLUETOOTH_SCAN_PERMISSION_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d("디버깅", "coarse location permission granted");
        } else {
          final AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("권한 제한");
          builder.setMessage("블루투스 스캔권한이 허용되지 않았습니다.");
          builder.setPositiveButton(android.R.string.ok, null);
          builder.setOnDismissListener(dialogInterface ->{});
          builder.show();
        }
        break;
      case BLUETOOTH_CONNECT_PERMISSION_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d("디버깅", "coarse location permission granted");
        } else {
          final AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("권한 제한");
          builder.setMessage("블루투스 연결 권한이 허용되지 않았습니다.");
          builder.setPositiveButton(android.R.string.ok, null);
          builder.setOnDismissListener(dialogInterface ->{});
          builder.show();
        }
        break;
      case BLUETOOTH_ADVERTISE_PERMISSION_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d("디버깅", "beacon advertise permission granted");
        }
        else {
          final AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("권한 제한");
          builder.setMessage("블루투스 advertise 권한이 허용되지 않았습니다.");
          builder.setPositiveButton(android.R.string.ok, null);
          builder.setOnDismissListener(dialogInterface ->{});
          builder.show();
        }
        break;
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermission() {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        //우리는 사용할꺼임.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        //LENS_FACING_BACK으로 설정 시 앞면 카메라
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_FRONT)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  public  boolean isStoragePermissionGranted() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED) {
        Log.v("Storage","Permission is granted");
        return true;
      } else {

        Log.v("Storage","Permission is revoked");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        return false;
      }
    }
    else { //permission is automatically granted on sdk<23 upon installation
      Log.v("Storage","Permission is granted");
      return true;
    }
  }

  @Override
  public void onClick(View v) {

    //View rootView = getWindow().getDecorView().getRootView();
    View rootView = v.getRootView();
    rootView.setDrawingCacheEnabled(true);

    if (v.getId() == R.id.capture) {
     // new Thread(() -> {
       // handler.post(new Runnable() {
         // @Override
         // public void run() {
              String folderName = "DCIM";
              try{
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                Date currentTime = new Date();
                String dateString = formatter.format(currentTime);

                File sdCardPath = getExternalFilesDir(null);
                File dirs = new File(sdCardPath ,folderName);
                if(isStoragePermissionGranted()) {
                  if(sdCardPath.exists()) {
                    if (!dirs.exists()) {
                      if (!dirs.mkdirs()) {
                        Log.e("Capture Save File Error", "Directory Not Created");
                      } else {
                        Log.e("Capture Save File", "Directory Create");
                      }
                    }
                  }
                }

                Bitmap captureView = rootView.getDrawingCache();
                FileOutputStream fos;
                String saveFileName;
                try{
                  saveFileName = dateString + ".jpg";
                  File file = new File(dirs, saveFileName);
                  fos = new FileOutputStream(file);
                  captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                  if(file !=null) {
                    //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    Toast.makeText(
                                    CameraActivity.this,
                                    saveFileName + " created",
                                    Toast.LENGTH_SHORT)
                            .show();
                  }
                  fos.close();
                }catch(FileNotFoundException e){
                  e.printStackTrace();
                }
              }catch (Exception e){
                e.printStackTrace();
              }
        //  }
        //});
    //  }).start();
    }
    v.setDrawingCacheEnabled(false);
  }

  //Beacon 거리 재기
  protected static double calculateDistance(int measurePower, double rssi){
    if(rssi == 0)
      return -1.0;
    double ratio = rssi * 1.0 / measurePower;
    if(ratio < 1.0)
      return Math.pow(ratio, 10);
    else{
      return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
    }
  }

  //private long QRReadTime = 0;
  //private final long QR_READ_INTERVAL_TIME = 60000; //1분 이내 QR Code 다시 못 읽게 하기 위해.


  protected void showFindInfo(String findInfo) {
    findAreaTextView.setText(findInfo);
  }


  protected abstract void updateActiveModel();
  protected abstract void processImage();


  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();


  protected abstract void initializeTTS();
  protected abstract void onBeaconDetected(Beacon beacon);


}

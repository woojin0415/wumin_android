package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.examples.detection.layout.squarebutton;
import org.tensorflow.lite.examples.detection.navi.Bluetooth;
import org.tensorflow.lite.examples.detection.navi.user_orientation;
import org.tensorflow.lite.examples.detection.navi.wifi;

import java.util.List;
import java.util.Locale;

public class MainScreen extends AppCompatActivity {

    //Navi Parameters
    private SensorManager sm;
    private WifiManager wfm;
    private Sensor gyro;
    private Sensor mag;
    private Sensor grv;
    private user_orientation user_ori;
    private boolean start = false;
    Bluetooth ble;
    private TextToSpeech tts;


    private boolean click;

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_screen);
        

        LinearLayout b_layout = findViewById(R.id.button_layout);
        Button bt_test = findViewById(R.id.bt_test);
        squarebutton bt_main = findViewById(R.id.bt_main);
        ViewGroup.LayoutParams params = b_layout.getLayoutParams();
        int width = params.width;
        params.height = width;
        Log.e("Layout", String.valueOf(width) + "/" + String.valueOf(params.height));
        b_layout.setLayoutParams(params);
        click = false;


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
        wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi WiFi = new wifi(wfm, this);


        user_ori = new user_orientation(sm, gyro, mag, tts);


        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        ble = new Bluetooth(adapter, user_ori, tts);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    while(true) {
                        if(click) {
                            b_layout.setRotation(i);
                            bt_main.setRotation(-i);
                            Thread.sleep(10);
                            i++;

                        }

                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        bt_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                click = !click;
                if(!start){
                    tts.speak("시스템을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                    start = true;
                    ble.start("0");
                    WiFi.start();
                }
                else {
                    start = false;
                    ble.stop();
                }
            }
        });

        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DetectorActivity.class);
                startActivity(intent);
            }
        });



    }
}

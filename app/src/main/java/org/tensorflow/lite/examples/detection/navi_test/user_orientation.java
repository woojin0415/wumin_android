package org.tensorflow.lite.examples.detection.navi_test;


import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;

public class user_orientation implements SensorEventListener{
    private ImageView tv;
    private SensorManager sm;
    private Sensor gyro;
    private Sensor mag;
    private Sensor grv;
    private float[] acc_values = new float[3];
    private float[] mag_values = new float[3];

    private int filter_length = 10;
    private float [] filter = new float[filter_length];
    private int count;
    private boolean start = false;
    private Bluetooth ble;
    private boolean time_interval;
    private boolean explaining;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain;



    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor gyro, Sensor mag, TextToSpeech tts){
        this.sm = sm;
        this.gyro = gyro;
        this.mag = mag;
        this.tts =tts;
        this.explain = false;
        this.time_interval = true;
        this.count = 0;


        sm.registerListener((SensorEventListener) this, gyro, SensorManager.SENSOR_DELAY_UI);
        //sm.registerListener((SensorEventListener) this, mag, SensorManager.SENSOR_DELAY_UI);
        for(int i = 0; i < filter_length; i++)
            filter[i] = 0;



    }
    public void set_ble(Bluetooth ble){
        this.ble = ble;
    }

    //그림 설명 중인지 아닌지 세팅
    public void set_explain(boolean value, String p_name){
        this.explain = value;
        Log.e("ori", String.valueOf(explain));
        Log.e("ori", p_name);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == gyro) {
            System.arraycopy(event.values, 0, acc_values, 0, event.values.length);
            filter[count%filter_length] = acc_values[1];
            count++;
            if (check_filter(filter) && time_interval){
                explain = false;
                if(ble.set_changable() == 1){
                    tv.setBackgroundColor(Color.parseColor("#646464"));
                }
                if(ble.set_changable() == 2){
                    tv.setBackgroundColor(Color.parseColor("#ffffff"));
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            time_interval = false;
                            Thread.sleep(4000);
                            time_interval = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();

            }
            //Log.e("ori", String.valueOf(acc_values[0]) + " / " + String.valueOf(acc_values[1]) + " / " + String.valueOf(acc_values[2]));
        }


    }

    public void sector_change(ImageView new_tv){
        if(tv == null) {
            start = true;
            tv = new_tv;
            //tv.setImageResource(R.drawable.resize_compass);
            //tv.setBackgroundColor(Color.parseColor("#646464"));
        }
        else {
            tv.setRotation(0);
            //tv.setImageResource(0);
            //tv.setBackgroundColor(Color.parseColor("#646464"));
            tv = new_tv;
            //tv.setImageResource(R.drawable.resize_compass);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

   boolean check_filter(float []arr){
        float sum = 0;
        for(int i =0; i<arr.length; i++){
            sum += Math.abs(arr[i]);
        }
        if(sum / arr.length >= 1)
            return true;
        else
            return false;
   }
}

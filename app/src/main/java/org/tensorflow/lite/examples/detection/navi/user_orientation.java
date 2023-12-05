package org.tensorflow.lite.examples.detection.navi;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class user_orientation implements SensorEventListener{
    private SensorManager sm;
    private Sensor gyro;
    private Sensor acc;
    private Sensor grv;
    private float[] gyro_value = new float[3];
    private float[] acc_values = new float[3];

    private int filter_length = 10;
    private float [] filter = new float[filter_length];
    private float [] moving_check_filter = new float[filter_length];
    private int count;
    private int mcf_count;
    private boolean start = false;
    private Bluetooth ble;
    private boolean time_interval;
    private boolean mcf_time_interval;
    private boolean explaining;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain;






    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor gyro, Sensor acc, TextToSpeech tts){
        this.sm = sm;
        this.gyro = gyro;
        this.acc = acc;
        this.tts =tts;
        this.explain = false;
        this.time_interval = true;
        this.mcf_time_interval = true;
        this.count = 0;
        this.mcf_count = 0;


        sm.registerListener((SensorEventListener) this, gyro, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_UI);
        for(int i = 0; i < filter_length; i++) {
            filter[i] = 0;
            moving_check_filter[i] = 0;
        }



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
            System.arraycopy(event.values, 0, gyro_value, 0, event.values.length);
            //Log.e("gyro",String.valueOf(gyro_value[0]) + " / " + String.valueOf(gyro_value[1]) + " / " + String.valueOf(gyro_value[2]) + " / " );
            filter[count%filter_length] = gyro_value[1];
            count++;
            if (check_filter(filter, -1) && time_interval && !explain && ble.p_location()){
                explain = true;
                ble.set_explaining(explain);
                tts.speak("작품 상세 설명을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                ble.speak_wi();
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
            else if (check_filter(filter, 1)&& time_interval && explain && ble.p_location()){
                explain = false;
                tts.speak("앞 쪽으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                ble.set_explaining(false);
                ble.set_changable(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            time_interval = false;
                            Thread.sleep(1000);
                            time_interval = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }



    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

   boolean check_filter(float []arr, int direc){
        //direc == -1 이면 왼쪽
        //direc == 1 이면 오른 쪽
        float sum = 0;
        for(int i =0; i<arr.length; i++){
            sum += arr[i];
        }
        if(direc == -1) {
            if (sum / arr.length < -1 && sum < 0)
                return true;
            else
                return false;
        }
        else if(direc == 1){
            if (sum / arr.length > 1 && sum > 0)
                return true;
            else
                return false;
        }
        return false;
   }

    boolean mcf_check_filter(float []arr){
        int num_of_moving_data = 0;
        for(int i =0; i<arr.length; i++){
            if (Math.abs(arr[i]) < 0.4)
                num_of_moving_data +=1;
        }
        if(num_of_moving_data >= arr.length-3)
            return true;
        else
            return false;
    }
}

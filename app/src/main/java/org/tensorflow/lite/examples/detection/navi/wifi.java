package org.tensorflow.lite.examples.detection.navi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.List;


public class wifi{
    private WifiManager wfm;
    private Context context;
    private TextView tv;
    public wifi(WifiManager wfm, Context context, TextView tv) {
        this.wfm = wfm;
        this.context = context;
        this.tv = tv;
        Log.e("wifi", "init");
    }

    public void start(){
        wfm.startScan();
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    }
    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        //scan 시작시 시작되는 함수
        @Override
        public void onReceive(Context context, Intent intent) {
            wfm.startScan();
            String action = intent.getAction();
            //Log.e("wifi", "receive check");
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                //Log.e("wifi", "can receive");
                wifiscan();
            }
        }
    };

    private void wifiscan() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("wifi", "permission error");
            return;
        }
        int s1_rssi = -100;
        int s2_rssi = -100;
        int s3_rssi = -100;

        List<ScanResult> scanresults = wfm.getScanResults();
        for(int i =0; i< scanresults.size(); i++){
            ScanResult result = scanresults.get(i);
            String ssid = result.SSID;
            int rssi = result.level;
            if(ssid.equals("wumin_mesh_s1"))
                s1_rssi = rssi;
            else if (ssid.equals("wumin_mesh_s2"))
                s2_rssi = rssi;
            else if (ssid.equals("wumin_mesh_s3"))
                s3_rssi = rssi;
            //Log.e("wifiscan", ssid + ": " + String.valueOf(rssi));
        }
        tv.setText(String.valueOf(s1_rssi) + " / " + String.valueOf(s2_rssi) + " / " + String.valueOf(s3_rssi));
    }
}

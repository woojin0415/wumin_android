package org.tensorflow.lite.examples.detection.storage;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class BLEStorage {

    private double[] RSSI = new double[6];
    private long time;
    private String sector;
    BLEStorage() {}
    public double []get_RSSI() {
        return RSSI;
    }
    public long get_Time(){
        return time;
    }
    public String get_Sector() { return sector;};

    public void setValue(long now, double []RSSI, String sector) {
        this.RSSI = RSSI;
        this.sector = sector;
        this.time = now;
    }
}

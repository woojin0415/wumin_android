package org.tensorflow.lite.examples.detection.storage;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class OrientationStorage {
    private long time;
    private String sector;
    private double azimuth;

    OrientationStorage() {}
    public long get_time(){
        return this.time;
    }
    public String get_sector(){
        return this.sector;
    }
    public double get_azimuth(){
        return this.azimuth;
    }
    public void setValue(long time, double azimuth, String sector){
        this.time = time;
        this.azimuth = azimuth;
        this.sector = sector;
    }
}

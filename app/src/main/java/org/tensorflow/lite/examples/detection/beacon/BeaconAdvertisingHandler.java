package org.tensorflow.lite.examples.detection.beacon;
// AdvertisingHandler.java
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Random;
import java.util.UUID;
import java.util.Arrays;

public class BeaconAdvertisingHandler extends Handler {
    private BeaconTransmitter beaconTransmitter;
    private Context context; // 추가
    public static final int MSG_START_ADVERTISING = 1;
    public static final int MSG_STOP_ADVERTISING = 2;

    public BeaconAdvertisingHandler(Looper looper, Context context) {
        super(looper);
        this.context = context; // 추가
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_ADVERTISING:
                startBeaconAdvertising();
                break;
            case MSG_STOP_ADVERTISING:
                stopBeaconAdvertising();
                break;
        }
    }

    private String generateDynamicUUID() {
        // 동적으로 UUID를 생성하는 코드 추가
        // 예시로 랜덤 UUID를 생성하도록 하겠습니다.
        return UUID.randomUUID().toString();
    }

    private int generateDynamicMajor() {
        // 동적으로 major를 생성하는 코드 추가
        // 예시로 랜덤 major를 생성하도록 하겠습니다.
        return new Random().nextInt(65536);
    }

    private int generateDynamicMinor() {
        // 동적으로 minor를 생성하는 코드 추가
        // 예시로 랜덤 minor를 생성하도록 하겠습니다.
        return new Random().nextInt(65536);
    }

    private void startBeaconAdvertising() {
        try {
            // BeaconParser를 설정하여 사용할 비콘 형식을 지정
            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);

            // BeaconTransmitter 초기화
            beaconTransmitter = new BeaconTransmitter(context, beaconParser);

            // 동적으로 UUID, major, minor 설정
            String dynamicUUID = generateDynamicUUID();
            int dynamicMajor = generateDynamicMajor();
            int dynamicMinor = generateDynamicMinor();
            Log.e("dynamicUUID", "dynamicUUID : "  +dynamicUUID);
            Log.e("dynamicUUID", "dynamicUUID : "  +dynamicUUID);
            Log.e("dynamicUUID", "dynamicUUID : "  +dynamicUUID);

            // 비콘 데이터 설정
            Beacon beacon = new Beacon.Builder()
                    .setId1(dynamicUUID)
                    .setId2(String.valueOf(dynamicMajor))
                    .setId3(String.valueOf(dynamicMinor))
                    .setManufacturer(0x0119) //회사 코드
                    .setTxPower(-21) //전송 파워
                    .setDataFields(Arrays.asList(new Long[] {0l}))
                    .build();


            // 비콘 advertising 시작
            beaconTransmitter.startAdvertising(beacon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private void stopBeaconAdvertising() {
        // 비콘 advertising 중지
        if (beaconTransmitter != null) {
            beaconTransmitter.stopAdvertising();
        }
    }
}


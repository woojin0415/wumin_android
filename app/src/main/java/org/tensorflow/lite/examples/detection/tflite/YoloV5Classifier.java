/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.detection.tflite;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.examples.detection.MainScreen;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.env.Utils;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

public class YoloV5Classifier implements Classifier {
    public static YoloV5Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final boolean isQuantized,
            final int inputSize)
            throws IOException {
        final YoloV5Classifier d = new YoloV5Classifier();

        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.labels.add(line);
        }
        br.close();

        try {
            Interpreter.Options options = (new Interpreter.Options());
            options.setNumThreads(NUM_THREADS);
            if (isNNAPI) {
                d.nnapiDelegate = null;
                // Initialize interpreter with NNAPI delegate for Android Pie or above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    d.nnapiDelegate = new NnApiDelegate();
                    options.addDelegate(d.nnapiDelegate);
                    options.setNumThreads(NUM_THREADS);
                    options.setUseNNAPI(true);
                }
            }
            if (isGPU) {
                GpuDelegate.Options gpu_options = new GpuDelegate.Options();
                gpu_options.setPrecisionLossAllowed(true); // It seems that the default is true
                gpu_options.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED);
                d.gpuDelegate = new GpuDelegate(gpu_options);
                options.addDelegate(d.gpuDelegate);
            }
            d.tfliteModel = Utils.loadModelFile(assetManager, modelFilename);
            d.tfLite = new Interpreter(d.tfliteModel, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.INPUT_SIZE = inputSize;
        d.imgData = ByteBuffer.allocateDirect(1 * d.INPUT_SIZE * d.INPUT_SIZE * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.INPUT_SIZE * d.INPUT_SIZE];

        d.output_box = (int) ((Math.pow((inputSize / 32), 2) + Math.pow((inputSize / 16), 2) + Math.pow((inputSize / 8), 2)) * 3);
        if (d.isModelQuantized){
            Tensor inpten = d.tfLite.getInputTensor(0);
            d.inp_scale = inpten.quantizationParams().getScale();
            d.inp_zero_point = inpten.quantizationParams().getZeroPoint();
            Tensor oupten = d.tfLite.getOutputTensor(0);
            d.oup_scale = oupten.quantizationParams().getScale();
            d.oup_zero_point = oupten.quantizationParams().getZeroPoint();
        }

        int[] shape = d.tfLite.getOutputTensor(0).shape();
        int numClass = shape[shape.length - 1] - 5;
        d.numClass = numClass;
        d.outData = ByteBuffer.allocateDirect(d.output_box * (numClass + 5) * numBytesPerChannel);
        d.outData.order(ByteOrder.nativeOrder());
        return d;
    }

    public int getInputSize() {
        return INPUT_SIZE;
    }
    @Override
    public void enableStatLogging(final boolean logStats) {
    }

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {
        tfLite.close();
        tfLite = null;
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnapiDelegate != null) {
            nnapiDelegate.close();
            nnapiDelegate = null;
        }
        tfliteModel = null;
    }

    public void setNumThreads(int num_threads) {
        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
    }

    private void recreateInterpreter() {
        if (tfLite != null) {
            tfLite.close();
            tfLite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    public void useGpu() {
        if (gpuDelegate == null) {
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            recreateInterpreter();
        }
    }

    public void useCPU() {
        recreateInterpreter();
    }

    public void useNNAPI() {
        nnapiDelegate = new NnApiDelegate();
        tfliteOptions.addDelegate(nnapiDelegate);
        recreateInterpreter();
    }

    @Override
    public float getObjThresh() {
        return MainScreen.MINIMUM_CONFIDENCE_TF_OD_API;
    }

    private static final Logger LOGGER = new Logger();

    // Float model
    private final float IMAGE_MEAN = 0;

    private final float IMAGE_STD = 255.0f;

    //config yolo
    private int INPUT_SIZE = -1;

    private  int output_box;

    private static final float[] XYSCALE = new float[]{1.2f, 1.1f, 1.05f};

    private static final int NUM_BOXES_PER_BLOCK = 3;

    // Number of threads in the java app
    private static final int NUM_THREADS = 1;
    private static boolean isNNAPI = false;
    private static boolean isGPU = false;

    private boolean isModelQuantized;

    /** holds a gpu delegate */
    GpuDelegate gpuDelegate = null;
    /** holds an nnapi delegate */
    NnApiDelegate nnapiDelegate = null;

    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    // Config values.

    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;

    private ByteBuffer imgData;
    private ByteBuffer outData;

    private Interpreter tfLite;
    private float inp_scale;
    private int inp_zero_point;
    private float oup_scale;
    private int oup_zero_point;
    private int numClass;
    private YoloV5Classifier() {
    }

    //non maximum suppression
    protected ArrayList<Recognition> nms(ArrayList<Recognition> list) {
        ArrayList<Recognition> nmsList = new ArrayList<Recognition>();

        for (int k = 0; k < labels.size(); k++) {
            //1.find max confidence per class
            PriorityQueue<Recognition> pq =
                    new PriorityQueue<Recognition>(
                            50,
                            new Comparator<Recognition>() {
                                @Override
                                public int compare(final Recognition lhs, final Recognition rhs) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                                }
                            });

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getDetectedClass() == k) {
                    pq.add(list.get(i));
                }
            }

            //2.do non maximum suppression
            while (pq.size() > 0) {
                //insert detection with max confidence
                Recognition[] a = new Recognition[pq.size()];
                Recognition[] detections = pq.toArray(a);
                Recognition max = detections[0];
                nmsList.add(max);
                pq.clear();

                for (int j = 1; j < detections.length; j++) {
                    Recognition detection = detections[j];
                    RectF b = detection.getLocation();
                    if (box_iou(max.getLocation(), b) < mNmsThresh) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsList;
    }

    protected float mNmsThresh = 0.6f;

    protected float box_iou(RectF a, RectF b) {
        return box_intersection(a, b) / box_union(a, b);
    }

    protected float box_intersection(RectF a, RectF b) {
        float w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left);
        float h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top);
        if (w < 0 || h < 0) return 0;
        float area = w * h;
        return area;
    }

    protected float box_union(RectF a, RectF b) {
        float i = box_intersection(a, b);
        float u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
        return u;
    }

    protected float overlap(float x1, float w1, float x2, float w2) {
        float l1 = x1 - w1 / 2;
        float l2 = x2 - w2 / 2;
        float left = l1 > l2 ? l1 : l2;
        float r1 = x1 + w1 / 2;
        float r2 = x2 + w2 / 2;
        float right = r1 < r2 ? r1 : r2;
        return right - left;
    }

    protected static final int BATCH_SIZE = 1;
    protected static final int PIXEL_SIZE = 3;

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    protected ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
//        byteBuffer.order(ByteOrder.nativeOrder());
//        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;

        imgData.rewind();
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) ((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) (((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        return imgData;
    }

    public ArrayList<Recognition> recognizeImage(Bitmap bitmap) {
        ByteBuffer byteBuffer_ = convertBitmapToByteBuffer(bitmap);

        Map<Integer, Object> outputMap = new HashMap<>();
        HashMap<String,String> nameMap = new HashMap<String,String>(){{
            /*
            put("0", "사람이 근처에 있어요");
            put("1", "사람이 근처에 있어요");
            put("2", "첫번째 작품");
            put("3", "두번째 작품");
            put("4", "세번째 작품");
            put("5", "네번째 작품");
            put("6", "다섯번째 작품");
            put("7", "여섯번째 작품");
            put("8", "일곱번째 작품");
            put("9", "여덟번째 작품");
            put("10", "아홉번째 작품");
            put("11", "열번째 작품");
            */
            put("person", "사람이 근처에 있어요");
            put("head", "사람이 근처에 있어요");
            put("qr1", "첫번째 작품");
            put("qr2", "두번째 작품");
            put("qr3", "세번째 작품");
            put("qr4", "네번째 작품");
            put("qr5", "다섯번째 작품");
            put("qr6", "여섯번째 작품");
            put("qr7", "일곱번째 작품");
            put("qr8", "여덟번째 작품");
            put("qr9", "아홉번째 작품");
            put("qr10", "열번째 작품");

            /*
            put("0", "사람이 근처에 있어요");
            put("1", "자전거");
            put("2", "자동차");
            put("3", "오토바이");
            put("4", "비행기");
            put("5", "버스");
            put("6", "기차");
            put("7", "트럭");
            put("8", "보트");
            put("9", "신호등");
            put("10", "소화전");
            put("11", "정지 신호");
            put("12", "주차료 징수 기");
            put("13", "벤치");
            put("14", "새");
            put("15", "고양이");
            put("16", "개");
            put("17", "말");
            put("18", "양");
            put("19", "암소");
            put("20", "코끼리");
            put("21", "곰");
            put("22", "얼룩말");
            put("23", "기린");
            put("24", "배낭");
            put("25", "우산");
            put("26", "핸드백");
            put("27", "넥타이");
            put("28", "여행 가방");
            put("29", "프리스비");
            put("30", "스키");
            put("31", "스노보드");
            put("32", "스포츠 공");
            put("33", "연");
            put("34", "야구 방망이");
            put("35", "야구 글러브");
            put("36", "스케이트보드");
            put("37", "서핑보드");
            put("38", "테니스 라켓");
            put("39", "병");
            put("40", "와인 잔");
            put("41", "컵");
            put("42", "포크");
            put("43", "칼");
            put("44", "숟가락");
            put("45", "그릇");
            put("46", "바나나");
            put("47", "사과");
            put("48", "샌드위치");
            put("49", "오렌지");
            put("50", "브로콜리");
            put("51", "당근");
            put("52", "핫도그");
            put("53", "피자");
            put("54", "도넛");
            put("55", "케이크");
            put("56", "의자");
            put("57", "소파");
            put("58", "화분");
            put("59", "침대");
            put("60", "식탁");
            put("61", "화장실");
            put("62", "티비모니터");
            put("63", "랩탑");
            put("64", "생쥐");
            put("65", "원격");
            put("66", "건반");
            put("67", "휴대전화");
            put("68", "마이크로파");
            put("69", "오븐");
            put("70", "토스터");
            put("71", "싱크대");
            put("72", "냉장고");
            put("73", "책");
            put("74", "시계");
            put("75", "꽃병");
            put("76", "가위");
            put("77", "테디 베어");
            put("78", "헤어드라이어");
            put("79", "칫솔");
            */
        }};

        outData.rewind();
        outputMap.put(0, outData);
        Log.d("YoloV5Classifier", "mObjThresh: " + getObjThresh());

        Object[] inputArray = {imgData};
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        ByteBuffer byteBuffer = (ByteBuffer) outputMap.get(0);
        byteBuffer.rewind();

        ArrayList<Recognition> detections = new ArrayList<Recognition>();

        float[][][] out = new float[1][output_box][numClass + 5];
        Log.d("YoloV5Classifier", "out[0] detect start");
        for (int i = 0; i < output_box; ++i) {
            for (int j = 0; j < numClass + 5; ++j) {
                if (isModelQuantized){
                    out[0][i][j] = oup_scale * (((int) byteBuffer.get() & 0xFF) - oup_zero_point);
                }
                else {
                    out[0][i][j] = byteBuffer.getFloat();
                }
            }
            // Denormalize xywh
            for (int j = 0; j < 4; ++j) {
                out[0][i][j] *= getInputSize();
            }
        }

        // 각 bounding box에 대해 가장 확률이 높은 Class 예측
        for (int i = 0; i < output_box; ++i){
            final int offset = 0;
            final float confidence = out[0][i][4];
            int detectedClass = -1;
            float maxClass = 0;

            final float[] classes = new float[labels.size()];
            for (int c = 0; c < labels.size(); ++c) {
                classes[c] = out[0][i][5 + c];  // classes: 각 class의 확률 계산
            }

            for (int c = 0; c < labels.size(); ++c) {
                if (classes[c] > maxClass) {
                    detectedClass = c;
                    maxClass = classes[c];
                }   // 가장 큰 확률의 class로 선정
            }

            final float confidenceInClass = maxClass * confidence;
            if (confidenceInClass > getObjThresh()) {
                final float xPos = out[0][i][0];
                final float yPos = out[0][i][1];

                final float w = out[0][i][2];
                final float h = out[0][i][3];
                Log.d("YoloV5Classifier",
                        Float.toString(xPos) + ',' + yPos + ',' + w + ',' + h);

                final RectF rect =
                        new RectF(
                                Math.max(0, xPos - w / 2),
                                Math.max(0, yPos - h / 2),
                                Math.min(bitmap.getWidth() - 1, xPos + w / 2),
                                Math.min(bitmap.getHeight() - 1, yPos + h / 2));
                detections.add(new Recognition("" + offset, nameMap.get(labels.get(detectedClass)),
                        confidenceInClass, rect, detectedClass));
            }
        }

        Log.d("YoloV5Classifier", "detect end");
        final ArrayList<Recognition> recognitions = nms(detections);
        return recognitions;
    }

    public boolean checkInvalidateBox(float x, float y, float width, float height, float oriW, float oriH, int intputSize) {
        // (1) (x, y, w, h) --> (xmin, ymin, xmax, ymax)
        float halfHeight = height / 2.0f;
        float halfWidth = width / 2.0f;

        float[] pred_coor = new float[]{x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight};

        // (2) (xmin, ymin, xmax, ymax) -> (xmin_org, ymin_org, xmax_org, ymax_org)
        float resize_ratioW = 1.0f * intputSize / oriW;
        float resize_ratioH = 1.0f * intputSize / oriH;

        float resize_ratio = resize_ratioW > resize_ratioH ? resize_ratioH : resize_ratioW; //min

        float dw = (intputSize - resize_ratio * oriW) / 2;
        float dh = (intputSize - resize_ratio * oriH) / 2;

        pred_coor[0] = 1.0f * (pred_coor[0] - dw) / resize_ratio;
        pred_coor[2] = 1.0f * (pred_coor[2] - dw) / resize_ratio;

        pred_coor[1] = 1.0f * (pred_coor[1] - dh) / resize_ratio;
        pred_coor[3] = 1.0f * (pred_coor[3] - dh) / resize_ratio;

        // (3) clip some boxes those are out of range
        pred_coor[0] = pred_coor[0] > 0 ? pred_coor[0] : 0;
        pred_coor[1] = pred_coor[1] > 0 ? pred_coor[1] : 0;

        pred_coor[2] = pred_coor[2] < (oriW - 1) ? pred_coor[2] : (oriW - 1);
        pred_coor[3] = pred_coor[3] < (oriH - 1) ? pred_coor[3] : (oriH - 1);

        if ((pred_coor[0] > pred_coor[2]) || (pred_coor[1] > pred_coor[3])) {
            pred_coor[0] = 0;
            pred_coor[1] = 0;
            pred_coor[2] = 0;
            pred_coor[3] = 0;
        }

        // (4) discard some invalid boxes
        float temp1 = pred_coor[2] - pred_coor[0];
        float temp2 = pred_coor[3] - pred_coor[1];
        float temp = temp1 * temp2;
        if (temp < 0) {
            Log.e("checkInvalidateBox", "temp < 0");
            return false;
        }
        if (Math.sqrt(temp) > Float.MAX_VALUE) {
            Log.e("checkInvalidateBox", "temp max");
            return false;
        }

        return true;
    }
}

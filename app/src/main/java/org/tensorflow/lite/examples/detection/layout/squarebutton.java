package org.tensorflow.lite.examples.detection.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class squarebutton extends Button {

    public squarebutton(Context context) {
        super(context);
    }

    public squarebutton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public squarebutton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        width = Math.min(width, height);
        height = width;

        setMeasuredDimension(width, height);
    }
}
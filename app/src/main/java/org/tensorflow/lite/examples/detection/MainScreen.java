package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.layout.squarebutton;

public class MainScreen extends AppCompatActivity {

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

package io.filepicker.manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


@SuppressWarnings("FieldCanBeLocal")
public class SplashScreenActivity extends Activity {

    private static final int SPLASH_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreenActivity.this, Manager.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}

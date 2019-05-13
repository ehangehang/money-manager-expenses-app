package com.example.diglet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainConfig mainConfig = (MainConfig) getApplication();
                if (mainConfig.getCurrentUser() == null){
                    Intent intent = new Intent(MainActivity.this,UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                }else {
                    Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                }MainActivity.this.finish();
            }
        },1000);
    }
}

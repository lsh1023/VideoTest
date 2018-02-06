package com.test.videotest;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity  {

    private Button mBtnLocal;
    private Button mBtnEncode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnLocal = (Button) findViewById(R.id.btn_local);
        mBtnEncode = (Button) findViewById(R.id.btn_encode);
        mBtnEncode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EncodeActivity.class));
            }
        });

        mBtnLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DecoderActivity.class));
            }
        });


    }


}

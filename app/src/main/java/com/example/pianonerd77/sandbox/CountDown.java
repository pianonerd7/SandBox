package com.example.pianonerd77.sandbox;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class CountDown extends ActionBarActivity {


    private void createTimer(){
        final TextView timer = (TextView)findViewById(R.id.time);
        new CountDownTimer((int)(SelectionPage.hideDuration*60*1000), 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText(millisUntilFinished / 1000+"");
            }

            public void onFinish() {

                timer.setText("Ready or not, here it comes!");

                Intent intent = new Intent
                        ("com.example.pianonerd77.sandbox.YTMap");
                startActivity(intent);
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count_down);
        createTimer();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_count_down, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

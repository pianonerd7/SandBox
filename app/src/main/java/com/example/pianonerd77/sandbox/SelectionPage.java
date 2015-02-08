package com.example.pianonerd77.sandbox;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class SelectionPage extends ActionBarActivity {

    private static Button button_click;
    public static double hideDuration;
    public static double gameDuration;

    public void onClickButtonListener() {

        button_click = (Button) findViewById(R.id.nextButton);
        button_click.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(checkCondition()) {
                            Intent intent = new Intent
                                    ("com.example.pianonerd77.sandbox.CountDown");
                            startActivity(intent);
                        }
                    }
                }
        );
    }

    private boolean checkCondition() {

        String hideDurationStr = ((EditText)findViewById(R.id.duration_hide)).getText().toString();
        String durationGameStr = ((EditText)findViewById(R.id.durationGame)).getText().toString();
        /*
        System.out.println("hello");
        System.out.println(hideDurationStr);
        System.out.println(durationGameStr);
        Log.e("Msg", "hello");
        Log.e("Msg", hideDurationStr);
        Log.e("Msg", durationGameStr);*/

        if(hideDurationStr.length()==0 || durationGameStr.length() ==0){
            ((TextView)findViewById(R.id.hideError)).setText("Please enter reasonable inputs");
            return false;
        }

        hideDuration = Double.parseDouble(hideDurationStr);
        gameDuration = Double.parseDouble(durationGameStr);

        if (hideDuration < 0.1 || gameDuration < 0.1) {
            ((TextView)findViewById(R.id.hideError)).setText("Please enter reasonable inputs");
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_page);
        onClickButtonListener();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_selection_page, menu);
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

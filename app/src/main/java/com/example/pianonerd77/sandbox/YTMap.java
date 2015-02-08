package com.example.pianonerd77.sandbox;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class YTMap extends ActionBarActivity {

    static final LatLng pos = new LatLng(40, -79);

    private static GoogleMap googleMap;
    private static Marker[] markers = new Marker[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ytmap);

        try {

            if (googleMap == null) {

                googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            }

            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            googleMap.setMyLocationEnabled(true);
            googleMap.setIndoorEnabled(true);
            googleMap.setBuildingsEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);

            Marker marker = googleMap.addMarker(new MarkerOptions().position(pos).title("hello"));
        }

        catch(Exception e) {
            e.printStackTrace();
        }
        createTimer();
    }

    private void createTimer(){
        final TextView timer = (TextView)findViewById(R.id.time);
        new CountDownTimer((int)(SelectionPage.gameDuration*60*1000), 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText("Seconds remaining: " + millisUntilFinished / 1000+"");
            }

            public void onFinish() {

                //timer.setText("Ready or not, here it comes!");

                Intent intent = new Intent
                        ("com.example.pianonerd77.sandbox.GameEnded");
                startActivity(intent);
            }
        }.start();
    }

    public static void updateMarkers (int i, LatLng latLng) {
        if (markers[i] !=null)
            markers[i].remove();
        markers[i] = updateMarker(latLng);
    }
    private static Marker updateMarker(LatLng latLng) {
        return googleMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.common_full_open_on_phone))
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .anchor(0f, 1f));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ytmap, menu);
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

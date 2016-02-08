package org.hypeastrum.arduino.due.slitscan;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import java.util.Random;


public class SlitScanMainActivity extends ActionBarActivity {

    private ArduinoDue arduinoDue;
    private ImageProcessor imageProcessor;
    private ImageRenderer renderer;

    private SurfaceView surface;
    private TextView statusView;

    private Handler handler;
    private StatusUpdater statusUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surface = (SurfaceView) findViewById(R.id.surface);
        statusView = (TextView) findViewById(R.id.statusText);

        renderer = new ImageRenderer(surface.getHolder());
        handler = new Handler(getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();

        statusUpdater = new StatusUpdater();
        handler.postDelayed(statusUpdater, 50);
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusUpdater = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        initArduino();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopArduino();
    }

    private void initArduino() {
        arduinoDue = ArduinoDue.fromIntent(this, getIntent());
        if (arduinoDue != null) {
            arduinoDue.open();
            imageProcessor = new ImageProcessor(arduinoDue, renderer);
        }
    }

    private void stopArduino() {
        if (arduinoDue != null) {
            arduinoDue.close();
            imageProcessor.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class StatusUpdater implements Runnable {

        @Override
        public void run() {
            if (statusUpdater != null) {
                final StatusCenter statusCenter = SlitScanApplication.instance().getStatusCenter();
                final StringBuilder stringBuilder = new StringBuilder();
                for (Object key : statusCenter.getStatusKeys()) {
                    stringBuilder.append(key);
                    stringBuilder.append(": ");
                    stringBuilder.append(statusCenter.getStatus(key));
                    stringBuilder.append('\n');
                }
                for (Object key : statusCenter.getCounterKeys()) {
                    stringBuilder.append(key);
                    stringBuilder.append(": ");
                    stringBuilder.append(statusCenter.getCounter(key));
                    stringBuilder.append('\n');
                }

                statusView.setText(stringBuilder);
                handler.postDelayed(statusUpdater, 50);
            }
        }
    }
}

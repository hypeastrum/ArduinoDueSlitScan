package org.hypeastrum.arduino.due.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.MainThread;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class MainActivity extends ActionBarActivity {

    private UsbManager usbManager;
    private UsbAccessory usbAccessory;
    private FileInputStream accIn;
    private FileOutputStream accOut;
    private ParcelFileDescriptor accFileDescriptor;
    private AtomicLong counter;
    private AtomicLong sum;

    private TextView counterView;
    private TextView sumView;
    private TextView avgView;
    private Handler handler;
    private RefreshTask refreshTask;
    private SurfaceView surfaceView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        counter = new AtomicLong();
        counterView = (TextView) findViewById(R.id.counter);

        sum = new AtomicLong();
        sumView = (TextView) findViewById(R.id.sum);

        avgView = (TextView) findViewById(R.id.avg);
        surfaceView = (SurfaceView) findViewById(R.id.surface);

        refreshTask = new RefreshTask();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        registerDetachListener();

        bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onStart() {
        super.onStart();
        counter.set(0);
        sum.set(0);
        openUsbAccessory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeUsbAccessory();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(usbBroadcastReceiver);
        super.onDestroy();
    }

    private void registerDetachListener() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(usbBroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver usbBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)){

                Toast.makeText(MainActivity.this, "onReceive: ACTION_USB_ACCESSORY_DETACHED", Toast.LENGTH_LONG).show();

                UsbAccessory intentUsbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                if(intentUsbAccessory != null && intentUsbAccessory.equals(usbAccessory)){
                    closeUsbAccessory();
                }
            }
        }
    };

    private void openUsbAccessory() {
        usbAccessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        if (usbAccessory != null) {
            accFileDescriptor = usbManager.openAccessory(usbAccessory);
            if (accFileDescriptor != null) {
                final FileDescriptor fd = accFileDescriptor.getFileDescriptor();
                accIn = new FileInputStream(fd);
                accOut = new FileOutputStream(fd);

                final Thread thread = new Thread(new AccessoryCommunicator());
                thread.start();
            }
        }
    }

    private void closeUsbAccessory() {
        if (accFileDescriptor != null) {
            try {
                accFileDescriptor.close();
            } catch (IOException e) {
                Log.e(getLocalClassName(), e.getLocalizedMessage(), e);
            } finally {
                accFileDescriptor = null;
                accIn = null;
                accOut = null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private class AccessoryCommunicator implements Runnable {

        public static final int BUF_SIZE = 16384;

        @Override
        public void run() {
            final byte[] buffer = new byte[BUF_SIZE];
            final int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
            int pixel = 0;
            while (accIn != null) {
                try {
                    final int read = accIn.read(buffer);
                    counter.addAndGet(read);
                    for (int i = 0; i < read; i++) {
                        final int unsigned = (buffer[i] & 0xFF);
                        pixels[pixel++] = 0xFF000000 | unsigned | (unsigned << 8) | (unsigned << 16);
                        if (pixel >= bitmap.getWidth() * bitmap.getHeight()) {
                            pixel = 0;
                        }
                        sum.addAndGet(unsigned);
                    }
                    handler.post(refreshTask);
                    bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    render();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
                    break;
                }
            }
        }
    }

    private void render() {
        if (bitmap != null) {
            final SurfaceHolder holder = surfaceView.getHolder();
            final Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                canvas.drawBitmap(bitmap, 0, 0, null);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }


    private class RefreshTask implements Runnable {
        @Override
        public void run() {
            final long count = counter.get();
            final long sum = MainActivity.this.sum.get();

            counterView.setText(String.valueOf(count));
            sumView.setText(String.valueOf(sum));
            avgView.setText(String.format("%.3f", sum / (double)count));
        }
    }
}

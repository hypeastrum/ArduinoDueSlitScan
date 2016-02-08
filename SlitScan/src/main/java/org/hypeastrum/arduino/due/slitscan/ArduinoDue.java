package org.hypeastrum.arduino.due.slitscan;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ArduinoDue {

    public static final int BUFFER_SIZE = 16384;
    private final UsbAccessory usbAccessory;
    private final UsbManager usbManager;
    private final Context context;

    private ParcelFileDescriptor fileDescriptor;
    private InputStream in;
    private OutputStream out;
    private BroadcastReceiver detachListener;

    public ArduinoDue(Context context, UsbAccessory usbAccessory) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbAccessory == null) {
            throw new IllegalArgumentException("USB Accessory missed");
        }
        this.usbAccessory = usbAccessory;

    }

    public void open() {
        registerDetachListener();

        fileDescriptor = usbManager.openAccessory(usbAccessory);
        if (fileDescriptor != null) {
            final FileDescriptor fd = fileDescriptor.getFileDescriptor();

            SlitScanApplication.instance().getStatusCenter().setStatus("File Descriptor", fd.toString());
            SlitScanApplication.instance().getStatusCenter().setStatus("File Descriptor can detect errors", String.valueOf(fileDescriptor.canDetectErrors()));
            SlitScanApplication.instance().getStatusCenter().setStatus("Devices", usbManager.getDeviceList().toString());
            SlitScanApplication.instance().getStatusCenter().setStatus("Accessories", Arrays.asList(usbManager.getAccessoryList()).toString());

            in = new FileInputStream(fd); // new BufferedInputStream(new FileInputStream(fd), BUFFER_SIZE);
            out = new FileOutputStream(fd); //new BufferedOutputStream(new FileOutputStream(fd), BUFFER_SIZE);
            SlitScanApplication.instance().getStatusCenter().setStatus("Arduino", "attached");
        }
    }

    private void registerDetachListener() {
        detachListener = new DetachListener();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        context.registerReceiver(detachListener, intentFilter);
    }

    public void close() {
        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
            } finally {
                fileDescriptor = null;
                in = null;
                out = null;
                context.unregisterReceiver(detachListener);
            }
        }
        SlitScanApplication.instance().getStatusCenter().setStatus("Arduino", "detached");
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    public static ArduinoDue fromIntent(Context context, Intent intent) {
        final UsbAccessory usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        return usbAccessory != null ? new ArduinoDue(context, usbAccessory) : null;
    }

    private class DetachListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)){

                Toast.makeText(context, "onReceive: ACTION_USB_ACCESSORY_DETACHED", Toast.LENGTH_SHORT).show();

                UsbAccessory intentUsbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                if(intentUsbAccessory != null && intentUsbAccessory.equals(usbAccessory)){
                    close();
                }
            }
        }
    };
}

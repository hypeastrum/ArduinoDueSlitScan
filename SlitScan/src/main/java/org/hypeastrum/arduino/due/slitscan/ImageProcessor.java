package org.hypeastrum.arduino.due.slitscan;

import android.util.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ImageProcessor {

    public static final int BUFFER_PIXEL_COUNT = 5300;
    private final Thread thread;
    private final ImageRenderer renderer;
    private final InputStream in;
    private final OutputStream out;

    public ImageProcessor(ArduinoDue arduino, ImageRenderer renderer) {
        this.in = arduino.getIn();
        this.out = arduino.getOut();
        this.renderer = renderer;
        this.thread = new Thread(new DataReader(), "ImageDataProcessor Thread");
        this.thread.start();
    }

    public void close() {
        thread.interrupt();
    }

    private class DataReader implements Runnable {

        private final byte[] bytes = new byte[BUFFER_PIXEL_COUNT];
        private final int[] pixels = new int[BUFFER_PIXEL_COUNT];

        @Override
        public void run() {
            SlitScanApplication.instance().getStatusCenter().setStatus("Data reader thread", "started");

            final String helloString = "Hello!";
            try {
                final byte[] sendBytes = helloString.getBytes();
                out.write(sendBytes);
                SlitScanApplication.instance().getStatusCenter().setStatus("Arduino hello message", "sent");

                final byte[] rcvBytes = new byte[sendBytes.length];
                final int read = in.read(rcvBytes);
                SlitScanApplication.instance().getStatusCenter().setStatus("Arduino hello message", String.format("received msg (%d bytes): %s", read, new String(rcvBytes)));
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
                SlitScanApplication.instance().getStatusCenter().setStatus("Arduino hello message", "failed");
            }

            int pos = 0;
            int read;

            SlitScanApplication.instance().getStatusCenter().setStatus("Data reader thread", "reading");
            while (!Thread.interrupted()) {
                try {
                    SlitScanApplication.instance().getStatusCenter().addCounter("Bytes received from Arduino", 0);

                    read = in.read(bytes, pos, bytes.length - pos);
                    SlitScanApplication.instance().getStatusCenter().incCounter("Reads from Arduino");
                    SlitScanApplication.instance().getStatusCenter().addCounter("Bytes received from Arduino", read);

                    pos += read;
                    if (pos >= bytes.length) {
                        renderer.postRow(bytesToPixels());
                        pos = 0;
                    }
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
                    break;
                }
            }
            SlitScanApplication.instance().getStatusCenter().setStatus("Data reader thread", "finished");
        }

        private int[] bytesToPixels() {
            for (int i = 0; i < BUFFER_PIXEL_COUNT; i++) {
                pixels[i] = bytes[i];
            }
            return pixels;
        }

    }
}

package org.hypeastrum.arduino.due.slitscan;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;

public class ImageRenderer {

    private final SurfaceHolder surfaceHolder;

    private int height;
    private int width;

    private Bitmap bitmap;
    private Paint paint;
    private int currentRowIndex;

    private boolean isActive;
    private final Object monitor = new Object();

    public ImageRenderer(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        surfaceHolder.addCallback(new SurfaceHolderCallback());
    }

    private void init(int width, int height) {
        synchronized (monitor) {
            this.width = width;
            this.height = height;
            bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
        }
    }

    public void postRow(int[] rowPixels) {
        if (bitmap != null) {
            synchronized (monitor) {
                final int rowWidth = Math.min(rowPixels.length, width);
                bitmap.setPixels(rowPixels, 0, rowWidth, 0, currentRowIndex, rowWidth, 1);
                currentRowIndex++;
                if (currentRowIndex >= height) {
                    currentRowIndex = 0;
                }
            }
            render();
        }
        SlitScanApplication.instance().getStatusCenter().incCounter("Lines received");
    }

    public void render() {
        final Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            try {
                canvas.drawBitmap(bitmap, 0, 0, null);
                canvas.drawLine(0, currentRowIndex, width, currentRowIndex, paint);
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
            SlitScanApplication.instance().getStatusCenter().incCounter("Frames rendered");
        }
    }

    private boolean isActive() {
        synchronized (monitor) {
            return isActive;
        }
    }

    private class Renderer implements Runnable {

        @Override
        public void run() {
            while (isActive()) {
                render();
                try {
                    Thread.sleep(0, 700);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class SurfaceHolderCallback implements SurfaceHolder.Callback {
        private Thread thread;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            synchronized (monitor) {
//                isActive = true;
//            }
//            thread = new Thread(new Renderer());
//            thread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            init(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
//            synchronized (monitor) {
//                isActive = false;
//            }
//            try {
//                if (thread.isAlive()) {
//                    thread.join();
//                }
//                thread = null;
//            } catch (InterruptedException e) {
//                // если не получилось, то будем пытаться еще и еще
//            }
        }
    }
}

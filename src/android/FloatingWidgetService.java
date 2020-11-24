package com.emersonar.plugin.widgetfloat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class FloatingWidgetService extends Service {

    private WindowManager mWindowManager;
    private View mOverlayView;
    int mWidth;
    private static final int MAX_CLICK_DURATION = 200;
    private long startClickTime;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

       // setTheme(getApplication().getResources().getIdentifier("AppTheme", "style", getPackageName()));

        mOverlayView = LayoutInflater.from(this).inflate(
                getApplication().getResources().getIdentifier("overlay_layout", "layout", getPackageName())
                , null);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mOverlayView, params);

        Display display = mWindowManager.getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);

        final RelativeLayout layout = (RelativeLayout) mOverlayView.findViewById(getApplication().getResources().getIdentifier("relativeLayoutParent","id",getPackageName()));
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = layout.getMeasuredWidth();

                //To get the accurate middle of the screen we subtract the width of the floating widget.
                mWidth = size.x - width;

            }
        });

      //  CircleImageView imageBtn = mOverlayView.findViewById(getApplication()
      //          .getResources().getIdentifier("logoFocus","id",getPackageName()));

        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        int middle = mWidth / 2;
                        float nearestXWall = params.x >= middle ? mWidth : 0;

                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;

                        if (clickDuration < MAX_CLICK_DURATION) {
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            startActivity(launchIntent);
                        }

                        params.x = (int) nearestXWall;
                        mWindowManager.updateViewLayout(mOverlayView, params);

                        return false;

                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mOverlayView, params);
                        return true;
                }
                return false;
            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOverlayView != null)
            mWindowManager.removeView(mOverlayView);
    }
}

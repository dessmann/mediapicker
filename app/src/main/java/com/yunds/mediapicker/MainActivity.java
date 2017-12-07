package com.yunds.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dsm.platform.listener.OnPermissionResult;
import com.dsm.platform.util.PermisstionUtil;
import com.dsm.platform.util.ToastUtil;
import com.mediapicker.PicturePicker;
import com.mediapicker.RxBus2;

import rx.functions.Action1;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private PicturePicker picturePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.testBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxBus2.subscribe(MainActivity.this, PicturePicker.BitmapLoad.class, new Action1<PicturePicker.BitmapLoad>() {
                    @Override
                    public void call(PicturePicker.BitmapLoad bitmapLoad) {
                        Log.i(TAG, "bitmapLoad=" + bitmapLoad);
                        ((ImageView)findViewById(R.id.testImg)).setImageBitmap(bitmapLoad.bitmap);
                    }
                }, PicturePicker.class.getSimpleName());
                picturePicker = new PicturePicker(MainActivity.this, MainActivity.this);
                PermisstionUtil.requestCamaraPermission(MainActivity.this, new OnPermissionResult() {
                    @Override
                    public void granted(int i) {
                        PermisstionUtil.requestStoragePermisstion(MainActivity.this, new OnPermissionResult() {
                            @Override
                            public void granted(int i) {
                                picturePicker.loadPopup();
                            }

                            @Override
                            public void denied(int i) {
                                ToastUtil.showToast("没有存储卡读写权限");
                            }
                        });
                    }

                    @Override
                    public void denied(int i) {
                        ToastUtil.showToast("没有拍照权限");
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            picturePicker.OnActivityResult(requestCode, resultCode, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermisstionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

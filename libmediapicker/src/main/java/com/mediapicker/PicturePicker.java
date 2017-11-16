package com.mediapicker;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

@SuppressWarnings("ALL")
public class PicturePicker {
    public static final String TAG = PicturePicker.class.getSimpleName(); // 暴露出去

    // 拍照
    private static final int REQUEST_CODE_CAMERA = 0x101;
    // 相册
    private static final int REQUEST_CODE_PHOTO = 0x102;
    // 裁剪
    private static final int REQUEST_CODE_CROP = 0x103;

    private static PopupWindow pop;
    private LinearLayout llPopup;

    private Activity mActivity;

    private boolean flag = true;

    private ViewGroup parentView;

    private final Context context;
    private int layoutResId = -1;
    private String text1;
    private String text2;
    private String text3;
    private boolean hideSecondButton;
    private OnViewItemClickListener onViewItemClickListener;

    public interface OnViewItemClickListener {
        void onLineOneClickListener();
        void onLineTwoClickListener();
        void onLineTreeClickListener();
    }

    public PicturePicker(Context context, Activity activity) {
        this.context = context;
        mActivity = activity;
        initPicture(context, activity);
    }
    private PicturePicker(Context context, Activity activity, int layoutResId, String text1, String text2, String text3, boolean hideSecondButton, OnViewItemClickListener onViewItemClickListener) {
        this.context = context;
        mActivity = activity;
        this.layoutResId = layoutResId;
        this.text1 = text1;
        this.text2 = text2;
        this.text3 = text3;
        this.hideSecondButton = hideSecondButton;
        this.onViewItemClickListener = onViewItemClickListener;
        initPicture(context, activity);
    }

    private PicturePicker(Context context, Fragment fragment) {
        this.context = context;
        Fragment mFragment = fragment;
        initPicture(context, fragment);
    }

    public interface OnPopDismiss {
        void dismiss(Boolean flag);
    }

    public static final String path = Environment.getExternalStorageDirectory()+ "/cache/";
    private static final String cameraPath = path + "camera.png";
    private static String filename = System.currentTimeMillis() + ".png";

    private void initPicture(final Context context, final Activity activity) {
        pop = new PopupWindow(context);

        parentView = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        View view;
        if (layoutResId != -1) {
            view = LayoutInflater.from(context).inflate(layoutResId, parentView, false);
            if (hideSecondButton) {
                view.findViewById(R.id.secondLayout).setVisibility(View.GONE);
            }
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_popupwindows, parentView, false);
        }
        llPopup = (LinearLayout) view.findViewById(R.id.ll_popup);

        pop.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        pop.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        pop.setBackgroundDrawable(new BitmapDrawable());
        pop.setFocusable(true);
        pop.setOutsideTouchable(true);
        pop.setContentView(view);

        RelativeLayout parent = (RelativeLayout) view.findViewById(R.id.parent);
        Button bt1 = (Button) view.findViewById(R.id.item_popupwindows_camera);

        Button bt2 = (Button) view.findViewById(R.id.item_popupwindows_Photo);
        if (bt2 != null) {
            if (!TextUtils.isEmpty(text2)) {
                bt2.setText(text2);
            }
            bt2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (onViewItemClickListener != null) {
                        dismiss();
                        onViewItemClickListener.onLineTwoClickListener();
                        return;
                    }
                    //打开相册选择照片
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    //intent.setType("image/*");//相片类型
                    intent.setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "image/*");//相片类型
                    activity.startActivityForResult(intent, REQUEST_CODE_PHOTO);
                    dismiss();
                    flag = false;
                }
            });
        }

        Button bt3 = (Button) view.findViewById(R.id.item_popupwindows_cancel);
        if (!TextUtils.isEmpty(text1)) {
            bt1.setText(text1);
        }

        if (!TextUtils.isEmpty(text3)) {
            bt3.setText(text3);
        }

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        bt1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onViewItemClickListener != null) {
                    dismiss();
                    onViewItemClickListener.onLineOneClickListener();
                    return;
                }
                File file = new File(path);
                if(!file.exists()){
                    file.mkdir();
                }

                File imageFile = new File(cameraPath);
                Uri imageUri;
                if (isSdk24()) {
                    imageUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);
                } else {
                    imageUri = Uri.fromFile(imageFile);
                }

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);

                flag = false;
                dismiss();
            }
        });

        bt3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onViewItemClickListener != null) {
                    dismiss();
                    onViewItemClickListener.onLineTreeClickListener();
                    return;
                }
                dismiss();
                flag = true;
            }
        });
    }

    private void initPicture(final Context context, final Fragment activity) {
        pop = new PopupWindow(context);

        View view = LayoutInflater.from(context).inflate(R.layout.item_popupwindows, null);
        llPopup = (LinearLayout) view.findViewById(R.id.ll_popup);

        pop.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        pop.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        pop.setBackgroundDrawable(new BitmapDrawable());
        pop.setFocusable(true);
        pop.setOutsideTouchable(true);
        pop.setContentView(view);

        RelativeLayout parent = (RelativeLayout) view.findViewById(R.id.parent);
        Button bt1 = (Button) view.findViewById(R.id.item_popupwindows_camera);
        Button bt2 = (Button) view.findViewById(R.id.item_popupwindows_Photo);
        Button bt3 = (Button) view.findViewById(R.id.item_popupwindows_cancel);

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        bt1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                File file = new File(path);
                if(!file.exists()){
                    file.mkdir();
                }

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(cameraPath)));
                activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);

                dismiss();
            }
        });

        bt2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //打开相册选择照片
                Intent intent = new Intent(Intent.ACTION_PICK);
                //intent.setType("image/*");//相片类型
                intent.setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "image/*");//相片类型
                activity.startActivityForResult(intent, REQUEST_CODE_PHOTO);
                dismiss();
            }
        });

        bt3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void loadPopup(Context context, View parentView, int gravity, final OnPopDismiss onPopDismiss){
        flag = true;
        llPopup.startAnimation(getInAnimation());
        pop.showAtLocation(parentView, gravity, 0, 0);
        openBackground((Activity)context);
        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (onPopDismiss != null) {
                    onPopDismiss.dismiss(flag);
                }
            }
        });
    }

    public void loadPopup(Context context, View parentView, final OnPopDismiss onPopDismiss){
        loadPopup(context, parentView, Gravity.BOTTOM, onPopDismiss);
    }

    public void loadPopup(){
        loadPopup(context, parentView, Gravity.BOTTOM, null);
    }

    public class BitmapLoad {
        public final Bitmap bitmap;
        public final Uri uri;

        BitmapLoad(Bitmap bitmap, Uri uri) {
            this.bitmap = bitmap;
            this.uri = uri;
        }
    }

    public void OnActivityResult(int requestCode, int resultCode, Intent data) throws Exception {
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA) {
                RxBus2.post("取消拍照", TAG);
            } else if (requestCode == REQUEST_CODE_PHOTO) {
                RxBus2.post("取消照片选择", TAG);
            } else if (requestCode == REQUEST_CODE_CROP) {
                RxBus2.post("取消裁剪", TAG);
            }
            return;
        }

        if (requestCode == REQUEST_CODE_CAMERA) {
            Uri uri = Uri.fromFile(new File(cameraPath));
            if (isSdk24()) {
                uri = getImageContentUri(uri);
            }
            startPhotoZoom(mActivity, uri);
        } else if (requestCode == REQUEST_CODE_PHOTO) {
            startPhotoZoom(mActivity, data.getData());
        } else if(requestCode == REQUEST_CODE_CROP){
            /*Bitmap bitmap = null;
            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    bitmap = extras.getParcelable("data");
                } else {
                    bitmap = BitmapFactory.decodeFile(path + filename);
                }
            } else {
                bitmap = BitmapFactory.decodeFile(path + filename);
            }
            BitmapUtil.save(bitmap, path + filename);*/
            //已改为不回传数据
            Bitmap bitmap = BitmapFactory.decodeFile(path + filename);
            new File(cameraPath).delete();
            RxBus2.post(new BitmapLoad(bitmap, Uri.fromFile(new File(path + filename))), TAG);
        } else {
            RxBus2.post("未知请求", TAG);
        }
    }


    /**
     * 裁剪图片方法实现
     */
    private static void startPhotoZoom(Activity activity, Uri uri) {
        int outputX = 350;
        int outputY = 350;

        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }

        filename = System.currentTimeMillis()+".png";
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", outputX);
        intent.putExtra("aspectY", outputY);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(path + filename)));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // 去黑边
        // intent.putExtra("scale", true);
        // intent.putExtra("scaleUpIfNeeded", true);
        // 关闭人脸识别
        // intent.putExtra("noFaceDetection", true);
        // intent.putExtra("outputFormat",
        // Bitmap.CompressFormat.PNG.toString());
        activity.startActivityForResult(intent, REQUEST_CODE_CROP);
    }

    private void dismiss() {
        Animation outAnim = getOutAnimation();
        outAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // 避免出现 Attempting to destroy the window while drawing!
                Observable.timer(10, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            pop.dismiss();
                            closeBackground(mActivity);
                        }
                    });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        llPopup.startAnimation(outAnim);
    }

    private Animation getInAnimation() {
        int res = getAnimationFromResource(Gravity.BOTTOM, true);
        return AnimationUtils.loadAnimation(mActivity, res);
    }

    private Animation getOutAnimation() {
        int res = getAnimationFromResource(Gravity.BOTTOM, false);
        return AnimationUtils.loadAnimation(mActivity, res);
    }

    private static int getAnimationFromResource(int gravity,  boolean isInAnimation) {
        switch (gravity) {
            case Gravity.BOTTOM:
                return isInAnimation ? R.anim.slide_in_bottom : R.anim.slide_out_bottom;
            case Gravity.CENTER:
                break;
        }
        return -1;
    }

    private static void openBackground(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = 0.6f;
        activity.getWindow().setAttributes(lp);
    }

    private static void closeBackground(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = 1f;
        activity.getWindow().setAttributes(lp);
    }

    /**
     * 安卓7.0裁剪根据文件路径获取uri
     */
    private Uri getImageContentUri(Uri uri) {
        File imageFile = new File(uri.getPath());
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            new String[]{MediaStore.Images.Media._ID},
            MediaStore.Images.Media.DATA + "=? ",
            new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            cursor.close();
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getApplicationContext().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
    /**
     * 删除缓存
     *
     * @return
     */
//    private static Boolean clearCache() {
//        return FileUtil.deleteFile(path);
//    }
    private static boolean isSdk24() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
}

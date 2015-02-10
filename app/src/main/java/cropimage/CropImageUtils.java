package cropimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 馋猫 on 2015/2/10.
 */
public class CropImageUtils {
    public static final int IMAGE_CODE = 0;
    public static final int REQUEST_CropPictureActivity = 2;
    public static Uri imageUriFromCamera;
    public static Uri cropImageUri;
    /**
     * 存储位置 默认Environment.getExternalStorageDirectory().getPath()
     */
    public static final String root_path = Environment.getExternalStorageDirectory().getPath();
    /**
     * 存储目录  默认/image/
     */
    public static String base_path = "/image/";
    /**
     * 文件名称 root_path + base_path + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg"
     */
    public static String fileName = root_path + base_path + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";

    public static String getBase_path() {
        return base_path;
    }

    public static void setBase_path(String base_path) {
        CropImageUtils.base_path = base_path;
    }

    public static void takeFromCamera(Activity activity, int REQUEST_IMAGE_CODE, String photo_path) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            //// 设置照相输出图片的路径
            File dir = new File(root_path+base_path);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(photo_path);
            CropImageUtils.imageUriFromCamera = CropImageUtils.createImagePathUri(activity);
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            activity.startActivityForResult(intent, REQUEST_IMAGE_CODE);
        } else {
            Log.e("馋猫", "找不到存储设备，请检查权限和是否有外部存储");
        }
    }

    public static void takeFromGallery(Activity activity, int REQUEST_IMAGE_CODE) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        activity.startActivityForResult(intent, REQUEST_IMAGE_CODE);
    }

    public static void cropImage(Activity activity, String path, int CROP_IMAGE) {
        CropImageUtils.cropImageUri = CropImageUtils.createImagePathUri(activity);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(Uri.fromFile(new File(path)), "image/*");
        intent.putExtra("crop", "true");

        ////////////////////////////////////////////////////////////////
        // 1.宽高和比例都不设置时,裁剪框可以自行调整(比例和大小都可以随意调整)
        ////////////////////////////////////////////////////////////////
        // 2.只设置裁剪框宽高比(aspect)后,裁剪框比例固定不可调整,只能调整大小
        ////////////////////////////////////////////////////////////////
        // 3.裁剪后生成图片宽高(output)的设置和裁剪框无关,只决定最终生成图片大小
        ////////////////////////////////////////////////////////////////
        // 4.裁剪框宽高比例(aspect)可以和裁剪后生成图片比例(output)不同,此时,
        //	会以裁剪框的宽为准,按照裁剪宽高比例生成一个图片,该图和框选部分可能不同,
        //  不同的情况可能是截取框选的一部分,也可能超出框选部分,向下延伸补足
        ////////////////////////////////////////////////////////////////

        // aspectX aspectY 是裁剪框宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪后生成图片的宽高
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);

        // return-data为true时,会直接返回bitmap数据,但是大图裁剪时会出现问题,推荐下面为false时的方式
        // return-data为false时,不会返回bitmap,但需要指定一个MediaStore.EXTRA_OUTPUT保存图片uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, CropImageUtils.cropImageUri);
        intent.putExtra("return-data", false);
        activity.startActivityForResult(intent, CROP_IMAGE);
    }

    /**
     * 创建一条图片地址uri,用于保存拍照后的照片
     *
     * @param context 依赖
     * @return 图片的uri   剪切后返回图片的Uri
     */
    private static Uri createImagePathUri(Context context) {
        Uri imageFilePath;
        String status = Environment.getExternalStorageState();
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        long time = System.currentTimeMillis();
        String imageName = timeFormatter.format(new Date(time));
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (status.equals(Environment.MEDIA_MOUNTED)) {// 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
            imageFilePath = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            imageFilePath = context.getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);
        }
        Log.d("馋猫", "生成的照片输出路径：" + imageFilePath.toString());
        return imageFilePath;
    }

    public static String  getPhotoHaveData(Intent data, Activity activity) {
        try {
            String path = null;
            if (data.getData() != null) {
                Uri originalUri = data.getData(); // 获得图片的uri
                path = getPath(activity, originalUri);
            } else {
                Uri uri_DCIM = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Cursor cr = activity.getContentResolver().query(uri_DCIM, new String[]{MediaStore.Images.Media.DATA}, null, null,
                        MediaStore.Images.Media.DATE_ADDED + " desc");
                if (cr.moveToNext()) {
                    // 大图
                    path = cr.getString(cr.getColumnIndex(MediaStore.Images.Media.DATA));
                }
                cr.close();
            }
            /***
             * 这里加这样一个判断主要是为了第三方的软件选择，比如：使用第三方的文件管理器的话， 你选择的文件就不一定是图片了，
             * 这样的话，我们判断文件的后缀名 如果是图片格式的话，那么才可以
             */
            if (path != null
                    && (path.toLowerCase().endsWith("jpg") || path.toLowerCase().endsWith("jpeg") || path.toLowerCase().endsWith("png") || path
                    .toLowerCase().endsWith("bmp"))) {
                //CropImageUtils.cropImage(activity, path, CropImageUtils.REQUEST_CropPictureActivity);
                return path;
            } else {
                Log.e("馋猫", "图片路径不存在==" + path);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void cropPicture(Intent data, Activity activity) {
        String path = null;
        if (data != null) {
            //如果有data 根据data获取到图片路径
            path = CropImageUtils.getPhotoHaveData(data, activity);
        } else {
            //没有deta的时候 直接调用存储的图片路径
            if (!CropImageUtils.fileName.equals("") && new File(CropImageUtils.fileName).exists()) {
                path = CropImageUtils.fileName;
            } else {
                Log.e("馋猫", "图片路径不存在==" + CropImageUtils.fileName);
            }
        }
        if (path != null) {
            CropImageUtils.cropImage(activity, path, CropImageUtils.REQUEST_CropPictureActivity);
        }
    }
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // MediaProvider
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}

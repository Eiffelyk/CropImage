package cropimage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.eiffelyk.www.cropimage.R;

public class MainActivity extends Activity {
    private ImageView iv;
    private CircleImageView circleImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.iv);
        circleImageView = (CircleImageView) findViewById(R.id.fragment_my_image_user);
        findViewById(R.id.btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });
    }


    public void showImagePickDialog() {
        String title = "获取图片方式";
        String[] choices = new String[]{"拍照", "从手机中选择"};
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(choices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                CropImageUtils.takeFromCamera(MainActivity.this, CropImageUtils.IMAGE_CODE, CropImageUtils.fileName);
                                break;
                            case 1:
                                CropImageUtils.takeFromGallery(MainActivity.this, CropImageUtils.IMAGE_CODE);
                                break;
                        }
                    }
                })
                .setNegativeButton("返回", null)
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            // 拍照和本地选择获取图片
            case CropImageUtils.IMAGE_CODE:
                CropImageUtils.cropPicture(data, MainActivity.this);
                break;
            // 裁剪图片后结果
            case CropImageUtils.REQUEST_CropPictureActivity:
                if (CropImageUtils.cropImageUri != null) {
                    // 可以直接显示图片,或者进行其他处理(如压缩等)
                    iv.setImageURI(CropImageUtils.cropImageUri);
                    circleImageView.setImageURI(CropImageUtils.cropImageUri);//圆形图片显示
                }
                break;
            default:
                break;
        }
    }




}

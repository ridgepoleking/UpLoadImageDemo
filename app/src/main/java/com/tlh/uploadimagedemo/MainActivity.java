package com.tlh.uploadimagedemo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnResultCallbackListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button mButtonCamera;
    private Button mButtonPhoto;
    private ImageView head;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonCamera = findViewById(R.id.btn_camera);
        mButtonPhoto = findViewById(R.id.btn_photo);
        head=findViewById(R.id.head);
        requestPermission();

        initListener();

    }

    private void initListener() {

        //打开相机
        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PictureSelector.create(MainActivity.this)
                        .openGallery(PictureMimeType.ofImage())
                        .loadImageEngine(GlideEngine.createGlideEngine()) // 请参考Demo GlideEngine.java
                        .forResult(PictureConfig.CHOOSE_REQUEST);

            }
        });

        //打开相册
        mButtonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PictureSelector.create(MainActivity.this)
                        .openGallery(PictureMimeType.ofImage())
                        // 图片加载引擎 需要 implements ImageEngine接口
                        .loadImageEngine(GlideEngine.createGlideEngine())
                        //单选or多选 PictureConfig.SINGLE PictureConfig.MULTIPLE
                        .selectionMode(PictureConfig.SINGLE)
                        //开启分页模式，默认开启另提供两个参数；pageSize每页总数；isFilterInvalidFile是否过滤损坏图片
                        .isPageStrategy(true, 20, true)
                        //PictureConfig.SINGLE模式下是否直接返回
                        .isSingleDirectReturn(true)
                        //查询指定大小内的图片、视频、音频大小，单位M
                        .queryMaxFileSize(-1)
                        //只查询指定后缀的资源
                        .querySpecifiedFormatSuffix("")
                        //Android Q版本下是否需要拷贝文件至应用沙盒内
                        .isAndroidQTransform(true)
                        //是否开启裁剪
                        .enableCrop(true)
                        //裁剪比例
                        .withAspectRatio(10, 10)
                        //是否压缩
                        .compress(true)
                        // 小于多少kb的图片不压缩
                        .minimumCompressSize(100)
                        // 裁剪输出质量 默认100
                        .cutOutQuality(100)
                        //图片压缩后输出质量
                        .compressQuality(80)
                        //结果回调分两种方式onActivityResult()和OnResultCallbackListener方式
                        .forResult(new OnResultCallbackListener<LocalMedia>() {
                            @Override
                            public void onResult(List<LocalMedia> result) {
                                String imgNmae = System.currentTimeMillis() + ".jpg";
                                uploadMultiFile(result.get(0).getCutPath(), imgNmae);

                            }

                            @Override
                            public void onCancel() {
                            }
                        });


            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // 结果回调
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    String imgNmae = System.currentTimeMillis() + ".jpg";
                    uploadMultiFile(selectList.get(0).getPath(), imgNmae);

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 上传图片
     * @param imgUrl        图片地址   绝对路径
     * @param imgNmae       图片名字   时间戳.jpg
     */
    private void uploadMultiFile(String imgUrl, String imgNmae) {
        String imageType = "multipart/form-data";
        File file = new File(imgUrl);//imgUrl为图片位置
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpg"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("图片字段", imgNmae, fileBody)
                .addFormDataPart("imagetype", imageType)
                .build();
        final Request request = new Request.Builder()
                .url("上传服务器地址")
                .post(requestBody)
                .build();
        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = httpBuilder
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            String data = jsonObject.getString("data");
                            Log.e("data",data);
                            Glide.with(MainActivity.this).load("地址拼接返回路径"+data).into(head);

                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }

        });
    }

    /**
     * 因为要打开相机相册 所有需要申请权限
     */
    public void requestPermission() {
        XXPermissions.with(this)
                .permission(Permission.READ_EXTERNAL_STORAGE
                        , Permission.WRITE_EXTERNAL_STORAGE
                        , Permission.CAMERA
                )
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {


                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {

                    }
                });
    }


}

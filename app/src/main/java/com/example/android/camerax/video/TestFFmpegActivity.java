package com.example.android.camerax.video;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.camerax.video.lightcompressorlibrary.CompressionListener;
import com.example.android.camerax.video.lightcompressorlibrary.VideoCompressor;
import com.example.android.camerax.video.lightcompressorlibrary.VideoQuality;
import com.example.android.camerax.video.lightcompressorlibrary.config.AppSpecificStorageConfiguration;
import com.example.android.camerax.video.lightcompressorlibrary.config.Configuration;
import com.example.android.camerax.video.lightcompressorlibrary.config.SharedStorageConfiguration;
import com.example.android.camerax.video.utils.VideoCompressorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import io.microshow.rxffmpeg.RxFFmpegProgress;
import io.reactivex.functions.Consumer;

public class TestFFmpegActivity extends AppCompatActivity {

    Button button;

    Button compressMp4;
    Button btn_permission;
    String TAG = "TestFFmpeg";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testffmpeg);
        button = findViewById(R.id.button);
        compressMp4 = findViewById(R.id.buttonCompressMp4);
        btn_permission = findViewById(R.id.btn_permission);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMp4(TestFFmpegActivity.this);
//                runFFmpegRxJava();
            }
        });
        compressMp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                runCompressMp4();
//                runCompressMp3();

                runCompressMp41();
            }
        });
        btn_permission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPermission();
            }
        });
    }
    String permissionsSingle = Manifest.permission.READ_MEDIA_VIDEO;

    ActivityResultLauncher activityResultSingle = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {

        }
    });
    private void onPermission(){
        activityResultSingle.launch(permissionsSingle);

    }

    private void runCompressMp41() {
        File inputFile = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "input.mp4");

        if (!inputFile.exists()) {
            return;
        }
        VideoCompressorUtils.Companion.onCompressorVideo(getApplicationContext(), inputFile.getAbsolutePath());
    }


    private void startCompression() {
        // 输入视频路径（示例：从存储中选择视频）
        File inputFile = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "input.mp4");

//        String inputPath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/sample_video.mp4";
//        inputPath = file.getAbsolutePath();
//        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            Toast.makeText(this, "Input video not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 输出视频路径
        String outputPath = getExternalCacheDir().getAbsolutePath() + "/compressed_video_" + System.currentTimeMillis() + ".mp4";

        // 配置压缩参数
        Configuration configuration = new Configuration(VideoQuality.MEDIUM, // 压缩质量
                false,              // 是否流式视频
                null,               // 帧率（null 保持原帧率）
                false, false, 100.0, 100.0, null);

        // 配置存储路径
        AppSpecificStorageConfiguration storageConfiguration = new AppSpecificStorageConfiguration();
        SharedStorageConfiguration sharedStorageConfiguration = new SharedStorageConfiguration();
        // 准备视频路径列表（支持批量压缩）
        ArrayList<Uri> videoUris = new ArrayList<>();
        videoUris.add(Uri.parse(inputFile.getAbsolutePath()));

        // 执行压缩
        VideoCompressor.start(this, // 上下文
                videoUris, // 输入视频 URI 列表
                false, // 是否共享存储（false 使用应用专属存储）
                sharedStorageConfiguration, // 存储配置
                configuration, // 压缩配置
                new CompressionListener() {
                    @Override
                    public void onSuccess(int i, long l, @Nullable String s) {

                    }

                    @Override
                    public void onStart(int index) {
                        Log.d(TAG, "Compression started for video: " + index);
                    }

                    @Override
                    public void onProgress(int index, float progress) {
                        Log.d(TAG, "Compression progress: " + progress + "%");
                    }


                    @Override
                    public void onFailure(int index, String failureMessage) {
                        Log.e(TAG, "Compression failed: " + failureMessage);
                        runOnUiThread(() -> Toast.makeText(TestFFmpegActivity.this, "Compression failed: " + failureMessage, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onCancelled(int index) {
                        Log.d(TAG, "Compression cancelled for video: " + index);
                        runOnUiThread(() -> Toast.makeText(TestFFmpegActivity.this, "Compression cancelled", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void saveMp4(Context context) {
        String fileName = "input.mp4"; //assets目录下的资源名及后缀
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            File file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video");
            if (!file.exists()) { //判断一下是否存在了，避免重复复制了
                file.mkdirs(); //不存在，创建一个新的文件夹吧
            }
            File resultFile = new File(file + File.separator + fileName);
            if (resultFile.exists()) {
                file.delete();
            }

            //最终文件路径为："/storage/emulated/0/ResProvider/video/show.mp4"
            FileOutputStream fileOutputStream = new FileOutputStream(resultFile);//File.separator就是"/"
            //这里开始拷贝
            int len = -1;
            byte[] buffer = new byte[1024];
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.close();//用完了，记得关闭
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void runCompressMp4() {
        File file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "input.mp4");
        Log.d("sss1", "===" + file.getAbsolutePath());
        File outPut = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "output.mp4");
        String text = "ffmpeg -i " + file.getAbsolutePath() + " -codec:v libx264 -crf 30 -preset medium " + outPut;
        text = "ffmpeg -i " + file.getAbsolutePath() + " -vf scale=640:360 " + outPut;
        String[] commands = text.split(" ");
        //开始执行FFmpeg命令
        RxFFmpegInvoke.getInstance().runCommandRxJava(commands).subscribe(new Consumer<RxFFmpegProgress>() {
            @Override
            public void accept(RxFFmpegProgress rxFFmpegProgress) throws Exception {

            }
        });

    }


    private void compressVideo(String sourcePath, String outputPath) {

    }

    private void runCompressMp3() {


        File file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "input.mp4");

        File outPut = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "video" + File.separator + "output.mp3");

//        String text = "ffmpeg -i " + file.getAbsolutePath() + " -q:a 0 -map a " + outPut.getAbsolutePath();

//        String[] commands = text.split(" ");
//
//        File voicePut = new File(cacheDirPath + "output.pcm");

//        String[] commands = {"-y", "-i", file.getAbsolutePath(), // 输入视频文件路径
//                "-vn", // 移除视频
//                "-ac", "1", // 单声道
//                "-acodec", "pcm_s16le", // 设置音频编码为16位线性PCM
//                "-ar", "16000", // 设置音频采样率为16kHz
//                "-f", "s16le", // 设置输出格式为PCM raw little-endian格式
//                outPut.getAbsolutePath() // 输出PCM文件路径
//        };

        //开始执行FFmpeg命令
//        RxFFmpegInvoke.getInstance().runCommandRxJava(commands).subscribe(new Consumer<RxFFmpegProgress>() {
//            @Override
//            public void accept(RxFFmpegProgress rxFFmpegProgress) throws Exception {
//
//            }
//        });

    }


}

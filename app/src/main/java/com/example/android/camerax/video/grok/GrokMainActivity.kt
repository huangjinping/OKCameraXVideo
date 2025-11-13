package com.example.android.camerax.video.grok

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.android.camerax.video.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GrokMainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var btnRecord: Button
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grok)

        previewView = findViewById(R.id.previewView)
        btnRecord = findViewById(R.id.btnRecord)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        btnRecord.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // 配置 Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // 使用 4:3 作为默认，ViewPort 会裁剪为 1:1
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // 配置 Recorder 和 VideoCapture
            val qualitySelector = QualitySelector.from(
                Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
            )
            val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
            videoCapture = VideoCapture.withOutput(recorder)

            // 配置 ViewPort 以实现 1:1 正方形裁剪
            val viewPort = ViewPort.Builder(Rational(1, 1), previewView.display.rotation).build()

            // 使用 UseCaseGroup 将 ViewPort 应用到 Preview 和 VideoCapture
            val useCaseGroup = UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture!!)
                .setViewPort(viewPort).build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "绑定失败: ${exc.message}")
                Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        val name = "CameraX-Video-" + SimpleDateFormat(
            FILENAME_FORMAT, Locale.US
        ).format(System.currentTimeMillis()) + ".mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val videoFile = File(externalCacheDir, name)
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()



        recording = videoCapture.output.prepareRecording(this, fileOutputOptions)
            .withAudioEnabled() // 启用音频（需权限）
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        btnRecord.text = "停止录制"
                        Toast.makeText(baseContext, "录制开始", Toast.LENGTH_SHORT).show()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "视频保存: ${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraX1", msg)
                            Log.d("CameraX1", videoFile.absolutePath)

                            printLength(videoFile.absolutePath)
                            if (videoFile.exists()) {
                                Log.d("CameraX1", "文件存在")

                            } else {
                                Log.d("CameraX1", "文件不存在")

                            }

                        } else {
                            recording?.close()
                            recording = null
                            Log.e("CameraX1", "录制错误: ${recordEvent.error}")
                            Toast.makeText(baseContext, "录制失败", Toast.LENGTH_SHORT).show()
                        }
                        btnRecord.text = "开始录制"
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "权限被拒绝，无法使用相机", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    fun printLength(filePath: String) {
        val fileSize = getFileSize(filePath)

        if (fileSize != -1L) {
            Log.e("CameraX1", "文件大小: $fileSize 字节")

            // 可以转换为其他单位
            Log.e("CameraX1", "文件大小: ${fileSize / 1024.0} KB")
            Log.e("CameraX1", "文件大小: ${fileSize / (1024.0 * 1024)} MB")
        } else {
            Log.e("CameraX1", "文件不存在或不是有效的文件")
        }
    }

    fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        // 检查文件是否存在且是文件（不是目录）
        if (file.exists() && file.isFile) {
            return file.length() // 返回文件大小，单位为字节
        }
        return -1 // 表示文件不存在或不是文件
    }
}
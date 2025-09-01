package com.example.android.camerax.video.lightcompressorlibrary

import android.content.Context
import android.net.Uri
import com.example.android.camerax.video.lightcompressorlibrary.compressor.Compressor.compressVideo
import com.example.android.camerax.video.lightcompressorlibrary.compressor.Compressor.isRunning
import com.example.android.camerax.video.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.example.android.camerax.video.lightcompressorlibrary.config.Configuration
import com.example.android.camerax.video.lightcompressorlibrary.config.SharedStorageConfiguration
import com.example.android.camerax.video.lightcompressorlibrary.video.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job? = null

    /**
     * This function compresses a given list of [uris] of video files and writes the compressed
     * video file at [SharedStorageConfiguration.saveAt] directory, or at [AppSpecificStorageConfiguration.subFolderName]
     *
     * The source videos should be provided content uris.
     *
     * Only [sharedStorageConfiguration] or [appSpecificStorageConfiguration] must be specified at a
     * time. Passing both will throw an Exception.
     *
     * @param [context] the application context.
     * @param [uris] the list of content Uris of the video files.
     * @param [isStreamable] determines if the output video should be prepared for streaming.
     * @param [sharedStorageConfiguration] configuration for the path directory where the compressed
     * videos will be saved, and the name of the file
     * @param [appSpecificStorageConfiguration] configuration for the path directory where the compressed
     * videos will be saved, the name of the file, and any sub-folders name. The library won't create the subfolder
     * and will throw an exception if the subfolder does not exist.
     * @param [listener] a compression listener that listens to compression [CompressionListener.onStart],
     * [CompressionListener.onProgress], [CompressionListener.onFailure], [CompressionListener.onSuccess]
     * and if the compression was [CompressionListener.onCancelled]
     * @param [configureWith] to allow add video compression configuration that could be:
     * [Configuration.quality] to allow choosing a video quality that can be [VideoQuality.LOW],
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * [Configuration.isMinBitrateCheckEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * [Configuration.videoBitrateInMbps] which is a custom bitrate for the video. You might consider setting
     * [Configuration.isMinBitrateCheckEnabled] to `false` if your bitrate is less than 2000000.
     *  * [Configuration.keepOriginalResolution] to keep the original video height and width when compressing.
     * This defaults to `false`
     * [Configuration.videoHeight] which is a custom height for the video. Must be specified with [Configuration.videoWidth]
     * [Configuration.videoWidth] which is a custom width for the video. Must be specified with [Configuration.videoHeight]
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context,
        uris: List<Uri>,
        isStreamable: Boolean = false,
        sharedStorageConfiguration: SharedStorageConfiguration? = null,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration? = null,
        configureWith: Configuration,
        listener: CompressionListener,
    ) {
        // Only one is allowed
        assert(sharedStorageConfiguration == null || appSpecificStorageConfiguration == null)
        assert(configureWith.videoNames.size == uris.size)

        doVideoCompression(
            context,
            uris,
            isStreamable,
            sharedStorageConfiguration,
            appSpecificStorageConfiguration,
            configureWith,
            listener,
        )
    }

    /**
     * Call this function to cancel video compression process which will call [CompressionListener.onCancelled]
     */
    @JvmStatic
    fun cancel() {
        job?.cancel()
        isRunning = false
    }

    private fun doVideoCompression(
        context: Context,
        uris: List<Uri>,
        isStreamable: Boolean,
        sharedStorageConfiguration: SharedStorageConfiguration?,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration?,
        configuration: Configuration,
        listener: CompressionListener,
    ) {
        var streamableFile: File? = null
        for (i in uris.indices) {

            job = launch(Dispatchers.IO) {

//                val job = async { getMediaPath(context, uris[i]) }
//                val path = job.await()
//                val path =
//                    "/storage/emulated/0/Android/data/com.zandroid.example.camerax.video/cache/video/input.mp4";

                val path = uris[i].path
                val desFile = saveVideoFile(
                    context,
                    path,
                    sharedStorageConfiguration,
                    appSpecificStorageConfiguration,
                    isStreamable,
                    configuration.videoNames[i],
                    shouldSave = false
                )

                if (isStreamable) streamableFile = saveVideoFile(
                    context,
                    path,
                    sharedStorageConfiguration,
                    appSpecificStorageConfiguration,
                    null,
                    configuration.videoNames[i],
                    shouldSave = false
                )

                desFile?.let {
                    isRunning = true
                    listener.onStart(i)
                    val result = startCompression(
                        i,
                        context,
                        uris[i],
                        desFile.path,
                        streamableFile?.path,
                        configuration,
                        listener,
                    )

                    // Runs in Main(UI) Thread
                    if (result.success) {
                        val savedFile = saveVideoFile(
                            context,
                            result.path,
                            sharedStorageConfiguration,
                            appSpecificStorageConfiguration,
                            isStreamable,
                            configuration.videoNames[i],
                            shouldSave = true
                        )

                        listener.onSuccess(i, result.size, savedFile?.path)
                    } else {
                        listener.onFailure(i, result.failureMessage ?: "An error has occurred!")
                    }
                }
            }
        }
    }

    private suspend fun startCompression(
        index: Int,
        context: Context,
        srcUri: Uri,
        destPath: String,
        streamableFile: String? = null,
        configuration: Configuration,
        listener: CompressionListener,
    ): Result = withContext(Dispatchers.Default) {
        return@withContext compressVideo(
            index,
            context,
            srcUri,
            destPath,
            streamableFile,
            configuration,
            object : CompressionProgressListener {
                override fun onProgressChanged(index: Int, percent: Float) {
                    listener.onProgress(index, percent)
                }

                override fun onProgressCancelled(index: Int) {
                    listener.onCancelled(index)
                }
            },
        )
    }

    private fun saveVideoFile(
        context: Context,
        filePath: String?,
        sharedStorageConfiguration: SharedStorageConfiguration?,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration?,
        isStreamable: Boolean?,
        videoName: String,
        shouldSave: Boolean?
    ): File? {
        if (context.externalCacheDir != null) {
            // 输入视频路径（示例：从存储中选择视频）
            val inputFile: File =
                File(context.externalCacheDir!!.absolutePath + File.separator + System.currentTimeMillis() + "output.mp4")
            return inputFile;
        }
        return null;

    }


}

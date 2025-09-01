package com.example.android.camerax.video.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.android.camerax.video.lightcompressorlibrary.CompressionListener
import com.example.android.camerax.video.lightcompressorlibrary.VideoCompressor
import com.example.android.camerax.video.lightcompressorlibrary.VideoQuality
import com.example.android.camerax.video.lightcompressorlibrary.config.Configuration
import com.example.android.camerax.video.lightcompressorlibrary.config.SaveLocation
import com.example.android.camerax.video.lightcompressorlibrary.config.SharedStorageConfiguration

class VideoCompressorUtils {


    companion object {
        var TAG = "VCompressor";

        fun onCompressorVideo(context: Context, path: String) {

            Log.d(TAG, "-------onCompressorVideo-------" + path)

            val uris = mutableListOf<Uri>()
            var rui = path.toUri()
            uris.add(path.toUri())
            VideoCompressor.start(
                context = context, // => This is required
                uris = uris, // => Source can be provided as content uris
                isStreamable = false,
                // THIS STORAGE
//                sharedStorageConfiguration = SharedStorageConfiguration(
//                    saveAt = SaveLocation.movies, // => default is movies
//                    subFolderName = "my-videos1" // => optional
//                ),
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies, subFolderName = "my-demo-videos"
                ),
                // OR AND NOT BOTH
//                appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
//                    subFolderName = "my-videos1" // => optional
//                ),

                configureWith = Configuration(
                    videoNames = uris.map { uri -> uri.pathSegments.last() },
//                    videoNames = listOf<String>(), /*list of video names, the size should be similar to the passed uris*/
                    quality = VideoQuality.VERY_LOW,
                    isMinBitrateCheckEnabled = true,
                    videoBitrateInMbps = 1, /*Int, ignore, or null*/
                    disableAudio = false, /*Boolean, or ignore*/
                    keepOriginalResolution = false,/*Boolean, or ignore*/
                    videoWidth = 360.0, /*Double, ignore, or null*/
                    videoHeight = 480.0 /*Double, ignore, or null*/
                ), listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                        // Update UI with progress value
                        Log.d(TAG, "-------onProgress-------" + percent + "=======index===" + index)

                        print("-------onProgress-------" + percent)
                    }

                    override fun onStart(index: Int) {
                        // Compression start
                        print("-------onStart-------" + index)
                        Log.d(TAG, "-------onStart-------" + index)

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        // On Compression success
                        print("-------onSuccess-------" + index + "=========" + size + "==========" + path)
                        Log.d(
                            TAG,
                            "-------onSuccess-------" + index + "=========" + size + "==========" + path
                        )

                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        // On Failure
                        print("-------onFailure-------" + index + "=========" + failureMessage)
                        Log.d(TAG, "-------onFailure-------" + index + "=========" + failureMessage)

                    }

                    override fun onCancelled(index: Int) {
                        // On Cancelled
                        print("-------onCancelled-------" + index)

                        Log.d(TAG, "-------onCancelled-------" + index)
                    }

                })
        }

    }

}
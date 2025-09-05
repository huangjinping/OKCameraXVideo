package com.example.video_compress

import android.content.Context
import android.net.Uri
import android.util.Log
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.source.TrimDataSource
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.RemoveTrackStrategy
import com.otaliastudios.transcoder.strategy.TrackStrategy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Future


/**
 * VideoCompressPlugin
 */
class VideoCompressPlugin {


    companion object {
        fun compressVideo(context: Context, path: String, quality: Int, position: Long) {
            ThumbnailUtility("video_compress").getFileThumbnail(
                context, path!!, quality, position.toLong()
            )
        }

        fun compressVideoV2(context: Context, path1: String) {
            var transcodeFuture: Future<Void>? = null
            val path = path1;
            val quality = 1
            val deleteOrigin = false
            val startTime = null
            val duration = null
            val includeAudio = true
            val frameRate = 30
            val tempDir: String = context!!.getExternalFilesDir("video_compress")!!.absolutePath
            val out = SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(Date())
            val destPath: String =
                tempDir + File.separator + "VID_" + out + path.hashCode() + ".mp4"
            var videoTrackStrategy: TrackStrategy = DefaultVideoStrategy.atMost(340).build();
            val audioTrackStrategy: TrackStrategy

            when (quality) {

                0 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(720).build()
                }

                1 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(250).build()
                }

                2 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(640).build()
                }

                3 -> {

                    assert(value = frameRate != null)
                    videoTrackStrategy = DefaultVideoStrategy.Builder().keyFrameInterval(3f)
                        .bitRate(1280 * 720 * 4.toLong())
                        .frameRate(frameRate!!) // will be capped to the input frameRate
                        .build()
                }

                4 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(480, 640).build()
                }

                5 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(540, 960).build()
                }

                6 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(720, 1280).build()
                }

                7 -> {
                    videoTrackStrategy = DefaultVideoStrategy.atMost(1080, 1920).build()
                }
            }

            audioTrackStrategy = if (includeAudio) {
                val sampleRate = DefaultAudioStrategy.SAMPLE_RATE_AS_INPUT
                val channels = DefaultAudioStrategy.CHANNELS_AS_INPUT

                DefaultAudioStrategy.builder().channels(channels) // 指定AAC编码
//                    .bitRate(128_000)
                    .sampleRate(sampleRate).build()

            } else {
                RemoveTrackStrategy()
            }

            val dataSource = if (startTime != null || duration != null) {
                Log.d("vies", "=UriDataSource1====")
                val source = UriDataSource(context, Uri.parse(path))
                TrimDataSource(
                    source,
                    (1000 * 1000 * (startTime ?: 0)).toLong(),
                    (1000 * 1000 * (duration ?: 0)).toLong()
                )
            } else {
                Log.d("vies", "=UriDataSource====");

                UriDataSource(context, Uri.parse(path))

            }




            transcodeFuture = Transcoder.into(destPath!!).addDataSource(dataSource)
                .setAudioTrackStrategy(audioTrackStrategy).setVideoTrackStrategy(videoTrackStrategy)
                .setListener(object : TranscoderListener {
                    override fun onTranscodeProgress(progress: Double) {
//                        channel.invokeMethod("updateProgress", progress * 100.00)
                        Log.d("vies", "=onTranscodeProgress====" + progress);

                    }

                    override fun onTranscodeCompleted(successCode: Int) {
//                        channel.invokeMethod("updateProgress", 100.00)
//                        val json = Utility(channelName).getMediaInfoJson(context, destPath)
//                        json.put("isCancel", false)
//                        result.success(json.toString())
                        Log.d("vies", "=onTranscodeCompleted====" + destPath);
                        if (deleteOrigin) {
                            File(path).delete()
                        }
                    }

                    override fun onTranscodeCanceled() {
                        Log.d("vies", "=onTranscodeCanceled====");

                    }

                    override fun onTranscodeFailed(exception: Throwable) {
                        Log.d("vies", "=onTranscodeFailed====" + exception.message);

                    }
                }).transcode()
        }
    }

}

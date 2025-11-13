package com.example.android.camerax.video

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.android.camerax.video.databinding.FragmentVideoViewerBinding

/**
 * VideoViewerFragment:
 *      Accept MediaStore URI and play it with VideoView (Also displaying file size and location)
 *      Note: Might be good to retrieve the encoded file mime type (not based on file type)
 */
class VideoViewerActivity : AppCompatActivity() {


    companion object {
        fun startAction(context: Context, path: String) {
            var intent = Intent(context, VideoViewerActivity::class.java)
            intent.putExtra("path", path);
            context.startActivity(intent)
        }

    }

    private var binding: FragmentVideoViewerBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentVideoViewerBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        var path = intent.getStringExtra("path")
        Log.d("okhttps", "=====" + path);
        showVideo(path!!.toUri())
    }


    /**
     * A helper function to play the recorded video. Note that VideoView/MediaController auto-hides
     * the play control menus, touch on the video area would bring it back for 3 second.
     * This functionality not really related to capture, provided here for convenient purpose to view:
     *   - the captured video
     *   - the file size and location
     */
    private fun showVideo(uri: Uri) {
//        val fileSize = getFileSizeFromUri(uri)
//        if (fileSize == null || fileSize <= 0) {
//            Log.e("okhttps", "Failed to get recorded file size, could not be played!")
//            return
//        }

//        val filePath = getAbsolutePathFromUri(uri) ?: return
//        val fileInfo = "FileSize: $fileSize\n $filePath"
//        Log.i("VideoViewerFragment", fileInfo)
//        binding!!.videoViewerTips.text = fileInfo

        val mc = MediaController(this)
        binding!!.videoViewer.apply {
            setVideoURI(uri)
            setMediaController(mc)
            requestFocus()
        }.start()
        mc.show(0)
    }

    /**
     * A helper function to get the captured file location.
     */
    private fun getAbsolutePathFromUri(contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null
            )
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: RuntimeException) {
            Log.e(
                "VideoViewerFragment", String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(),
                    e.toString()
                )
            )
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * A helper function to retrieve the captured file size.
     */
    private fun getFileSizeFromUri(contentUri: Uri): Long? {
        val cursor = contentResolver.query(contentUri, null, null, null, null) ?: return null

        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()

        cursor.use {
            return it.getLong(sizeIndex)
        }
    }
}
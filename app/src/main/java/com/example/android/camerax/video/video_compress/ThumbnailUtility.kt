package com.example.video_compress

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ThumbnailUtility(channelName: String) {
    private val utility = Utility(channelName)


    fun getFileThumbnail(context: Context, path: String, quality: Int, position: Long) {
        val bmp = utility.getBitmap(path, position)

        val dir = context.getExternalFilesDir("video_compress")

        if (dir != null && !dir.exists()) dir.mkdirs()

        val file = File(
            dir, path.substring(
                path.lastIndexOf('/'), path.lastIndexOf('.')
            ) + ".jpg"
        )
        utility.deleteFile(file)

        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()

        try {
            file.createNewFile()
            file.writeBytes(byteArray)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        bmp.recycle()

        Log.d("FileThumbnail", "==" + file.absolutePath);
//        result.success(file.absolutePath)
    }
}
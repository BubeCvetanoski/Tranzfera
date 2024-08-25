package com.example.tranzfera.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object HelperFunctions {
    inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

    inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    fun ActivityResultLauncher<Array<String>>.requestPermissions() {
        val listOfPermissions: MutableList<String> = mutableListOf()

        if (SDK_INT >= Build.VERSION_CODES.S) {
            listOfPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            listOfPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            listOfPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            listOfPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            listOfPermissions.add(Manifest.permission.BLUETOOTH)
            listOfPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            listOfPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOfPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        else
            listOfPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (SDK_INT >= Build.VERSION_CODES.R && SDK_INT <= Build.VERSION_CODES.TIRAMISU)
            listOfPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

        launch(listOfPermissions.toTypedArray())
    }

    fun ByteArray.toBitmap(): Bitmap? {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }

    suspend fun saveMediaToStorage(bitmap: Bitmap, context: Context) {
        withContext(Dispatchers.IO) {
            val filename = "${System.currentTimeMillis()}.jpg"
            var fos: OutputStream? = null

            if (SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver?.also { resolver ->

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                withContext(Dispatchers.Main) {
                    context.showToast("Saved to Photos")
                }
            }
        }
    }

    fun Context.showToast(text: String?) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
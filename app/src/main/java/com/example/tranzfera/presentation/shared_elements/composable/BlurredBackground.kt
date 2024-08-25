package com.example.tranzfera.presentation.shared_elements.composable

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
import com.example.tranzfera.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlurredBackground() {
    val context = LocalContext.current

    val (screenWidth, screenHeight) = getScreenSize(context)
    val requestOptions = RequestOptions().override(screenWidth, screenHeight)

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(key1 = context) {

        val resizedBitmap = withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(R.drawable.background)
                .apply(requestOptions)
                .submit()
                .get()
        }
        bitmap = resizedBitmap
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Add the blurred background image
        bitmap?.let { resizedBitmap ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                LegacyBlurImage(resizedBitmap)
            } else {
                BlurImage(
                    resizedBitmap,
                    Modifier.blur(radiusX = 15.dp, radiusY = 15.dp)
                )
            }
        }

        // Add the layer of darkness
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black.copy(alpha = 0.35f))
        )
    }
}

@Composable
fun LegacyBlurImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    //Blurring the photo for lower Android SDK
    val renderScript = RenderScript.create(LocalContext.current)
    val bitmapAlloc = Allocation.createFromBitmap(renderScript, bitmap)
    ScriptIntrinsicBlur.create(renderScript, bitmapAlloc.element).apply {
        setRadius(15f)
        setInput(bitmapAlloc)
        forEach(bitmapAlloc)
    }
    bitmapAlloc.copyTo(bitmap)
    renderScript.destroy()

    BlurImage(bitmap, modifier)
}

@Composable
fun BlurImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

fun getScreenSize(context: Context): Pair<Int, Int> {
    val displayMetrics = DisplayMetrics()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val defaultDisplay: Display = windowManager.defaultDisplay ?: return Pair(1000, 1000)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        defaultDisplay.getRealMetrics(displayMetrics)
    } else {
        @Suppress("DEPRECATION")
        defaultDisplay.getMetrics(displayMetrics)
    }
    return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
}
package com.example.tranzfera.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tranzfera.ui.composable.BlurredBackground

@Composable
fun TransferScreen(

) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BlurredBackground()
    }
}
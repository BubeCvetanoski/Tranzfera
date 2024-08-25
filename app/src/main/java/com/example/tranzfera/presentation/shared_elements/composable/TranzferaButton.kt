package com.example.tranzfera.presentation.shared_elements.composable

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp

@Composable
fun TranzferaButton(
    onButtonClick: () -> Unit,
    text: String
) {
    Button(
        onClick = onButtonClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Black
        ),
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                ambientColor = White,
                spotColor = White,
                shape = CircleShape
            )
    ) {
        Text(text = text)
    }
}
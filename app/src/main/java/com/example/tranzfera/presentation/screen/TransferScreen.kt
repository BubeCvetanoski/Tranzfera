package com.example.tranzfera.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tranzfera.OnAction
import com.example.tranzfera.data.bluetooth.BluetoothData
import com.example.tranzfera.data.bluetooth.DataType
import com.example.tranzfera.presentation.event.UiAction
import com.example.tranzfera.presentation.state.TransferState
import com.example.tranzfera.ui.composable.BlurredBackground
import com.example.tranzfera.util.HelperFunctions.toBitmap

//todo once the connection is done, the messages chat should be reset
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TransferScreen(
    onAction: OnAction,
    state: TransferState
) {
    val context = LocalContext.current
    val message = rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val imageBytes = context
                .contentResolver
                .openInputStream(it)
                ?.use { inputStream ->
                    inputStream.readBytes()
                } ?: byteArrayOf()

            onAction(
                UiAction.OnSendData(
                    data = BluetoothData(
                        imageBytes = imageBytes,
                        isFromMySide = true
                    ),
                    dataType = DataType.ImageType
                )
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        BlurredBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Messages",
                color = White,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LaunchedEffect(state.data.size) {
                    if (state.data.isNotEmpty()) {
                        listState.animateScrollToItem(state.data.size - 1)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.data) { data ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChatMessage(
                                data = data,
                                modifier = Modifier.align(
                                    if (data.isFromMySide) Alignment.End
                                    else Alignment.Start
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = message.value,
                    onValueChange = { message.value = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(text = "Message")
                    }
                )
                IconButton(
                    onClick = {
                        imageLauncher.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = "Send image",
                        tint = White
                    )
                }
                IconButton(onClick = {
                    onAction(
                        UiAction.OnSendData(
                            data = BluetoothData(
                                message = message.value,
                                isFromMySide = true
                            ),
                            dataType = DataType.StringType
                        )
                    )
                    message.value = ""
                    keyboardController?.hide()
                }) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = White
                    )
                }
            }
        }
    }
}


@Composable
fun ChatMessage(
    data: BluetoothData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (data.isFromMySide) 15.dp else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if (data.isFromMySide) 0.dp else 15.dp
                )
            )
            .background(Color(0xFFcd8b76))
            .padding(16.dp)
    ) {
        Text(
            text = data.sender ?: "Unknown",
            fontSize = 10.sp,
            color = Color.Black
        )
        data.message?.let {
            Text(
                text = it,
                color = Color.Black,
                modifier = Modifier.widthIn(max = 250.dp)
            )
        }
        data.imageBytes?.let { imageBytes->
            val bitmap = imageBytes.toBitmap()

            bitmap?.let { image ->
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Sent image",
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .heightIn(max = 250.dp)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ChatMessagePreview() {
    ChatMessage(
        data = BluetoothData(
            message = "Hello World!",
            sender = "Pixel 6",
            isFromMySide = false
        )
    )
}
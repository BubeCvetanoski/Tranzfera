package com.example.tranzfera.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tranzfera.FoundBluetoothDevice
import com.example.tranzfera.R
import com.example.tranzfera.ui.composable.BlurredBackground
import com.example.tranzfera.ui.composable.TranzferaButton
import kotlin.reflect.KFunction1

@Composable
fun ConnectScreen(
    onBluetoothEnableClick: () -> Unit,
    isBluetoothEnabled: Boolean,
    onButtonScanClick: KFunction1<(String) -> Unit, Unit>,
    onButtonStopScanClick: KFunction1<(String) -> Unit, Unit>,
    scannedDevices: List<FoundBluetoothDevice>,
    pairedDevices: List<FoundBluetoothDevice>,
    connectedDevice: FoundBluetoothDevice?,
    onPairedDeviceClick: (FoundBluetoothDevice) -> Unit,
    onScannedDeviceClick: (FoundBluetoothDevice) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BlurredBackground()
        when (isBluetoothEnabled) {
            false -> ScreenViewWithTurnedBluetoothOFF(
                onBluetoothEnableClick = onBluetoothEnableClick,
                bluetoothEnabledState = "Bluetooth is OFF"
            )

            true -> ScreenViewWithTurnedBluetoothON(
                onButtonScanClick = onButtonScanClick,
                onButtonStopScanClick = onButtonStopScanClick,
                scannedDevices = scannedDevices,
                pairedDevices = pairedDevices,
                connectedDevice = connectedDevice,
                onPairedDeviceClick = onPairedDeviceClick,
                onScannedDeviceClick = onScannedDeviceClick
            )
        }
    }
}

@Composable
fun ScreenViewWithTurnedBluetoothOFF(
    onBluetoothEnableClick: () -> Unit,
    bluetoothEnabledState: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onBluetoothEnableClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.bluetooth),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp),
            colorFilter = ColorFilter.tint(Red)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = bluetoothEnabledState,
            color = Red,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Tap anywhere on the screen to turn it ON",
            color = White,
            fontSize = 14.sp,
        )
    }
}


@Composable
fun ScreenViewWithTurnedBluetoothON(
    onButtonScanClick: KFunction1<(String) -> Unit, Unit>,
    onButtonStopScanClick: KFunction1<(String) -> Unit, Unit>,
    scannedDevices: List<FoundBluetoothDevice>,
    pairedDevices: List<FoundBluetoothDevice>,
    connectedDevice: FoundBluetoothDevice?,
    onPairedDeviceClick: (FoundBluetoothDevice) -> Unit,
    onScannedDeviceClick: (FoundBluetoothDevice) -> Unit
) {
    var buttonText by remember {
        mutableStateOf("Scan")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ConnectedDevicesList(connectedDevice)
        Spacer(modifier = Modifier.height(5.dp))
        TranzferaButton(
            onButtonClick = {
                if (buttonText == "Scan") onButtonScanClick {
                    buttonText = it
                }
                else onButtonStopScanClick {
                    buttonText = it
                }
            },
            text = buttonText
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScannedDevicesList(
                scannedDevices = scannedDevices,
                onScannedDeviceClick = onScannedDeviceClick,
                modifier = Modifier.weight(1f)
            )
            PairedDevicesList(
                pairedDevices = pairedDevices,
                onPairedDeviceClick = onPairedDeviceClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ConnectedDevicesList(
    connectedDevice: FoundBluetoothDevice?
) {
    Text(
        text = "Connected device:",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = White
    )
    if (connectedDevice == null) {
        Text(
            text = "There is no connected device at the moment!",
            color = Red
        )
    } else {
        Text(
            text = connectedDevice.name + "\n" + connectedDevice.address,
            color = White
        )
    }
}

@Composable
fun PairedDevicesList(
    pairedDevices: List<FoundBluetoothDevice>,
    onPairedDeviceClick: (FoundBluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var shouldShowPairedDevices by remember {
        mutableStateOf(false)
    }
    val dropDownVector = if (!shouldShowPairedDevices) Icons.Default.ArrowDropDown
    else Icons.Default.ArrowDropUp

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row {
            Text(
                text = "Paired devices",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier
                    .clickable {
                        shouldShowPairedDevices = !shouldShowPairedDevices
                    }
            )
            Icon(
                imageVector = dropDownVector,
                contentDescription = "dropdown",
                tint = White,
                modifier = Modifier
                    .clickable {
                        shouldShowPairedDevices = !shouldShowPairedDevices
                    }
            )
        }
        if (shouldShowPairedDevices) {
            LazyColumn(
                modifier = modifier,
                content = {
                    items(pairedDevices) { device ->
                        Text(
                            text = device.name + "\n" + device.address,
                            color = White,
                            modifier = Modifier
                                .clickable { onPairedDeviceClick(device) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            )
        }
    }
}

@Composable
fun ScannedDevicesList(
    scannedDevices: List<FoundBluetoothDevice>,
    onScannedDeviceClick: (FoundBluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var shouldShowScannedDevices by remember {
        mutableStateOf(false)
    }
    val dropDownVector = if (!shouldShowScannedDevices) Icons.Default.ArrowDropDown
    else Icons.Default.ArrowDropUp

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row {
            Text(
                text = "Scanned devices",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier
                    .clickable {
                        shouldShowScannedDevices = !shouldShowScannedDevices
                    }
            )
            Icon(
                imageVector = dropDownVector,
                contentDescription = "dropdown",
                tint = White,
                modifier = Modifier
                    .clickable {
                        shouldShowScannedDevices = !shouldShowScannedDevices
                    }
            )
        }
        if (shouldShowScannedDevices) {
            LazyColumn(
                modifier = modifier,
                content = {
                    items(scannedDevices) { device ->
                        Text(
                            text = device.name + "\n" + device.address,
                            color = White,
                            modifier = Modifier
                                .clickable { onScannedDeviceClick(device) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            )
        }
    }
}
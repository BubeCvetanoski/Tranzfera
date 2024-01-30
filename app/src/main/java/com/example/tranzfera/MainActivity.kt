package com.example.tranzfera

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tranzfera.navigation.Screens
import com.example.tranzfera.navigation.composable.TranzferaNavigationBar
import com.example.tranzfera.screen.ConnectScreen
import com.example.tranzfera.screen.TransferScreen

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var requestBluetoothEnableLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestBluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(bluetoothHandler)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothEnableLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}

        requestBluetoothPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {}

        //Log.i("TEST", "TEST")

        bluetoothHandler = BluetoothHandler(
            context = this,
            intentLauncher = requestBluetoothEnableLauncher,
            permissionLauncher = requestBluetoothPermissionLauncher
        )

        setContent {
            val navController = rememberNavController()
            val state = viewModel.state.collectAsState().value

            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                bottomBar = {
                    TranzferaNavigationBar(
                        navController = navController
                    )
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screens.ConnectScreen.name,
                    modifier = Modifier.padding(paddingValues),
                ) {
                    composable(route = Screens.ConnectScreen.name) {
                        ConnectScreen(
                            onBluetoothEnableClick = viewModel::onBluetoothEnableClick,
                            isBluetoothEnabled = state.isBluetoothEnabled,
                            onButtonScanClick = viewModel::onButtonScanClick,
                            onButtonStopScanClick = viewModel::onButtonStopScanClick,
                            scannedDevices = state.scannedDevices,
                            pairedDevices = state.pairedDevices,
                            connectedDevice = state.connectedDevice,
                            onPairedDeviceClick = viewModel::connectToDevice,
                            onScannedDeviceClick = viewModel::pairDevice
                        )
                    }
                    composable(route = Screens.TransferScreen.name) {
                        TransferScreen(
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.clearBluetoothHandler()
    }

    override fun onResume() {
        super.onResume()

        viewModel.startBluetoothServer()
    }
}
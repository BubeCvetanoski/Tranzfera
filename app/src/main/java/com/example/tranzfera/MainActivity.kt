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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tranzfera.data.bluetooth.BluetoothHandler
import com.example.tranzfera.presentation.event.UiAction
import com.example.tranzfera.presentation.navigation.Screens
import com.example.tranzfera.presentation.navigation.composable.TranzferaNavigationBar
import com.example.tranzfera.presentation.screen.ConnectScreen
import com.example.tranzfera.presentation.screen.TransferScreen

typealias OnAction = (UiAction) -> Unit

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

        bluetoothHandler = BluetoothHandler(
            context = applicationContext,
            intentLauncher = requestBluetoothEnableLauncher,
            permissionLauncher = requestBluetoothPermissionLauncher
        )

        setContent {
            val navController = rememberNavController()
            val connectState by viewModel.connectState.collectAsStateWithLifecycle()
            val transferState by viewModel.transferState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onAction(UiAction.StartBluetoothServer)
            }

            LaunchedEffect(!connectState.isConnectedDevice) {
                viewModel.onAction(UiAction.ResetTransferredData)
            }

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
                            onAction = viewModel::onAction,
                            state = connectState
                        )
                    }
                    composable(route = Screens.TransferScreen.name) {
                        TransferScreen(
                            onAction = viewModel::onAction,
                            state = transferState
                        )
                    }
                }
            }
        }
    }
}
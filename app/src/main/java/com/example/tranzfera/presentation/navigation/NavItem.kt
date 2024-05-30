package com.example.tranzfera.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems: List<NavItem> = listOf(
    NavItem(
        label = "Connect",
        icon = Icons.Default.BluetoothConnected,
        route = Screens.ConnectScreen.name
    ),
    NavItem(
        label = "Transfer",
        icon = Icons.Default.Send,
        route = Screens.TransferScreen.name
    )
)

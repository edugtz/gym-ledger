package com.edu.gymledger.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.edu.gymledger.feature.dashboard.DashboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymLedgerApp() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GymLedger") }
            )
        }
    ) { innerPadding ->
        DashboardScreen(modifier = Modifier.padding(innerPadding))
    }
}

package com.edu.gymledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.edu.gymledger.app.GymLedgerApp
import com.edu.gymledger.design.theme.GymLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            GymLedgerTheme {
                GymLedgerApp()
            }
        }
    }
}

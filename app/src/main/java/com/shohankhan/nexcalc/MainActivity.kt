package com.shohankhan.nexcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shohankhan.nexcalc.core.nav.NexCalcApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NexCalcApplication).container
        setContent {
            NexCalcApp(container)
        }
    }
}

package com.example.pzrymyo_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pzrymyo_ai.ui.theme.PzryMYO_AITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PzryMYO_AITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    //sistem cunuklarin altinda kalmasin diye innerPadding
                    Box(modifier =Modifier.padding(innerPadding)){
                        ChatScreen()
                    }
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.MonoDoHomeScreen
import com.example.ui.theme.MonoDoTheme
import com.example.ui.theme.PaperCanvas
import com.example.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonoDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PaperCanvas
                ) {
                    val context = LocalContext.current
                    val viewModel: TaskViewModel = viewModel(
                        factory = TaskViewModel.provideFactory(context.applicationContext as Application)
                    )
                    MonoDoHomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}

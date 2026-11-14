package com.quiting.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quiting.app.ui.AppRoot
import com.quiting.app.ui.QuitingTheme
import com.quiting.app.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            QuitingTheme(style = vm.style, mode = vm.themeMode) {
                AppRoot(vm)
            }
        }
    }
}

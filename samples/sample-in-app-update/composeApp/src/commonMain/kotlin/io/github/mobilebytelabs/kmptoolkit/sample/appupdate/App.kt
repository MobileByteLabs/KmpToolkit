package io.github.mobilebytelabs.kmptoolkit.sample.appupdate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.toast.ToastHost
import com.mobilebytelabs.kmptoolkit.toast.rememberToastHostState

@Composable
fun App() {
    val toastState = rememberToastHostState()

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppUpdateScreen(toastState = toastState)

            ToastHost(
                hostState = toastState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

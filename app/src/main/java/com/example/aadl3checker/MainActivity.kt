package com.example.aadl3checker

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aadl3checker.ui.theme.AADL3CheckerTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startWebsiteCheckService(this) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isServiceRunning by remember { mutableStateOf(isMyServiceRunning(WebsiteCheckService::class.java, this)) }

            MainScreen(
                isServiceRunning = isServiceRunning,
                onStartService = {
                    checkAndRequestNotificationPermission {
                        isServiceRunning = true
                    }
                },
                onStopService = {
                    stopWebsiteCheckService()
                    isServiceRunning = false
                }
            )
        }
    }

    private fun stopWebsiteCheckService() {
        val serviceIntent = Intent(this, WebsiteCheckService::class.java)
        stopService(serviceIntent)
    }

    private fun checkAndRequestNotificationPermission(onPermissionGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startWebsiteCheckService(this) {
                    onPermissionGranted()
                }
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainScreen(isServiceRunning: Boolean, onStartService: () -> Unit, onStopService: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AADL3CheckerTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(text = stringResource(R.string.by_clicking_on_the_start_button_the_app_will_check_the_availability_of_the_aadl3_website_every_minute_and_show_a_notification_if_the_website_is_available))
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isServiceRunning) onStopService() else onStartService()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) Color.Red else Color.Blue
                )
            ) {
                Text(if (isServiceRunning) stringResource(R.string.stop_checking_aadl3_website) else stringResource(
                    R.string.start_checking_aadl3_website
                ))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.visit_github),
                modifier = Modifier
                    .clickable { uriHandler.openUri("https://github.com/YourUsername/YourRepository") }
                    .padding(16.dp),
                color = Color.Blue
            )
        }
    }
}

private fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return service.foreground
        }
    }
    return false
}

private fun startWebsiteCheckService(context: Context, onServiceStarted: () -> Unit) {
    val serviceIntent = Intent(context, WebsiteCheckService::class.java)
    context.startForegroundService(serviceIntent)
    onServiceStarted()
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun DefaultPreview() {
    AADL3CheckerTheme {
        MainScreen(false, {}, {})
    }
}

package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.altitude.AltitudeConverter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException



const val TAG = "Zach Location Test"

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            MyApplicationTheme {
                MyScaffold(fusedLocationClient)
            }
        }
    }
}

@Composable
fun MyScaffold(locationClient: FusedLocationProviderClient) {
    val scope = rememberCoroutineScope()
    val hostState = remember { SnackbarHostState() }
    val location = remember { mutableStateOf<Location?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(modifier = Modifier.safeContentPadding()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Button(
                onClick = {
                    if (isLoading.value) {
                        return@Button
                    }
                    isLoading.value = true
                    val locationListener = getLocation(context, locationClient)
                    if (locationListener == null) {
                        Log.e(TAG, "Does not have location permissions")
                        isLoading.value = false
                        return@Button
                    }
                    locationListener.addOnSuccessListener { loc: Location? ->
                        Log.e(TAG, location.toString() ?: "No location!")
                        isLoading.value = false
                        location.value = loc
                    }
                }
            ) {
                if (isLoading.value) Text("Getting Location...")
                else Text("Get Location")
            }
            if (location.value != null) Text(location.toString())
            Button(
                onClick = {
                    if (location.value == null) {
                        scope.launch {
                            hostState.showSnackbar("Get your location first")
                        }
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        convertWgs84(location.value!!, context)
                    }
                    else {
                        scope.launch{hostState.showSnackbar(
                            "WGS84 conversion not supported on this Android version"
                        )}
                    }
                }
            ) { Text("Convert to MSL") }
        }
    }
}

fun getLocation(context: Context, locationClient: FusedLocationProviderClient): Task<Location>? {
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    )
    val source = CancellationTokenSource()
    if (hasPermission == PackageManager.PERMISSION_GRANTED) {
        return locationClient.getCurrentLocation(100, source.token)
    }
    return null;
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun convertWgs84(location: Location, context: Context) {
    Log.e(TAG, "Has altitude: ${location.hasAltitude()}")
    Log.e(TAG, "Has MSL altitude: ${location.hasMslAltitude()}")
    val converter = AltitudeConverter()
    val didAdd = converter.tryAddMslAltitudeToLocation(location)
    if (!didAdd) {
        Log.e(TAG, "Adding MSL to location")
        runBlocking {
            withContext(Dispatchers.IO) {
                converter.addMslAltitudeToLocation(context, location)
            }
            Log.e(TAG, "Added MSL to location!")
            Log.e(TAG, location.toString())
        }
        Log.e(TAG, "Indeed")
        Log.e(TAG, location.toString())
    }
    else {
        Log.e(TAG, "Could not add MSL to location")
    }
}

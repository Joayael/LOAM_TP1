package com.JoaquinApp.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.JoaquinApp.myapplication.ui.theme.MyApplicationTheme

import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater

import android.view.View

import androidx.fragment.app.FragmentActivity

import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng

class MainActivity : FragmentActivity() {
    var mapHandler: MapHandler? = null
    var activeDisasterLocation: LatLng? = null
    private lateinit var disasterDetector: DisasterDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        disasterDetector = DisasterDetector(this)
        disasterDetector.startMonitoring()

        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            mapHandler?.onPermissionGranted()
        }
    }

    fun onDisasterDetected(location: LatLng) {
        activeDisasterLocation = location
        // Si el mapa ya está cargado, lo actualizamos inmediatamente
        mapHandler?.showDisasterOnMap(location)
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.home, null)
                        HomeHandler(ctx, view).setupHome()
                        view
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
                AppDestinations.MAP -> AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.map, null)
                        val handler = MapHandler(ctx, view)
                        (ctx as? MainActivity)?.let { activity ->
                            activity.mapHandler = handler
                        }
                        handler.setupMap(null)
                        view
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
                AppDestinations.NEWS -> AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.news, null)
                        NewsHandler(ctx, view).setupNews()
                        view
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
                AppDestinations.CHAT -> AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.chat, null)
                        ChatHandler(ctx, view).setupChat()
                        view
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            } 
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    MAP("Map", R.drawable.ic_map),
    NEWS("News", R.drawable.ic_news),
    CHAT("Chat", R.drawable.ic_chat),
}

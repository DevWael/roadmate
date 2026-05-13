package com.roadmate.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.ui.theme.RoadMateTheme
import com.roadmate.phone.navigation.RoadMateNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var vehicleRepository: VehicleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadMateTheme(isHeadUnit = false) {
                RoadMateNavHost(
                    vehicleRepository = vehicleRepository,
                )
            }
        }
    }
}

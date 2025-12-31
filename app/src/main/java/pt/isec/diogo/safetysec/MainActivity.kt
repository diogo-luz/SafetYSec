package pt.isec.diogo.safetysec

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.isec.diogo.safetysec.ui.theme.SafetYSecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i("API_KEY", BuildConfig.MAPS_API_KEY)

        enableEdgeToEdge()
        setContent {
            SafetYSecTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    // Coordenadas do ISEC
    val isecLocation = LatLng(40.1925, -8.4128)

    // Configuração da posição da câmara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(isecLocation, 15f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = isecLocation),
            title = "ISEC",
            snippet = "Engenharia de Coimbra"
        )
    }
}
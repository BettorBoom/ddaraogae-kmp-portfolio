package dev.ddaraogae.app

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.ddaraogae.domain.LocationSample
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.shared.AppRuntime
import dev.ddaraogae.shared.DdaraogaeApp
import dev.ddaraogae.shared.createAndroidRuntime
import kotlinx.coroutines.launch

class DdaraogaeApplication : Application() {
    lateinit var runtime: AppRuntime
        private set
    override fun onCreate() {
        super.onCreate()
        runtime = createAndroidRuntime(this)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var runtime: AppRuntime
    private lateinit var locationManager: LocationManager
    private var started = false
    private var listener: LocationListener? = null
    private var trackedWalk: Pair<String, Int>? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission()) syncLocation(runtime.walkState.value.session)
        else runtime.locationNotice.value = "위치 권한을 거절했어요. 시간은 계속 기록할 수 있어요."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Activity recreation must not recreate the user scope (ADR-004).
        runtime = (application as DdaraogaeApplication).runtime
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        setContent { DdaraogaeApp(runtime) {
            if (!hasLocationPermission()) permission.launch(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        } }
        lifecycleScope.launch { runtime.walkState.collect { syncLocation(it.session) } }
    }

    override fun onStart() {
        super.onStart()
        started = true
        syncLocation(runtime.walkState.value.session)
    }

    override fun onStop() {
        started = false
        stopLocation()
        if (!isChangingConfigurations) runtime.pauseForBackground()
        super.onStop()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun stopLocation() {
        listener?.let(locationManager::removeUpdates)
        listener = null
        trackedWalk = null
    }

    private fun syncLocation(session: WalkSession?) {
        val active = session?.takeIf { started && it.status == WalkStatus.Recording }
        val key = active?.let { it.id to it.segment }
        if (key == trackedWalk && listener != null) return
        stopLocation()
        if (active == null) return
        if (!hasLocationPermission()) {
            runtime.locationNotice.value = "위치 권한이 없어 시간만 기록해요."
            return
        }
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val provider = when {
            fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            runtime.locationNotice.value = "기기의 위치 서비스를 켜면 경로와 거리를 기록할 수 있어요."
            return
        }
        val captured = active.id to active.segment
        val next = LocationListener { location ->
            val sample = runCatching { LocationSample(location.latitude, location.longitude, location.time,
                if (location.hasAccuracy()) location.accuracy.toDouble() else 100.0) }.getOrNull()
            if (sample != null) runtime.appendLocation(captured.first, captured.second, sample)
        }
        try {
            locationManager.requestLocationUpdates(provider, 2_000L, 3f, next, Looper.getMainLooper())
            listener = next
            trackedWalk = captured
            runtime.locationNotice.value = if (fine) null else "대략적 위치가 허용됐어요. 정확도가 낮으면 경로가 기록되지 않을 수 있어요."
        } catch (_: SecurityException) {
            runtime.locationNotice.value = "위치 권한이 변경됐어요. 시간만 기록해요."
        } catch (_: IllegalArgumentException) {
            runtime.locationNotice.value = "위치 서비스를 시작할 수 없어 시간만 기록해요."
        }
    }
}

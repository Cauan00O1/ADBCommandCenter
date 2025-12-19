package com.joaomgcd.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdbDiscoveryManager"


@Parcelize
data class AdbService(val host: InetAddress, val port: Int): Parcelable


data class DiscoveredAdbServices(
    val pairingService: AdbService? = null,
    val connectService: AdbService? = null
) {
    val isComplete: Boolean get() = pairingService != null && connectService != null
}

private fun String.debug() = Log.d(TAG, this)
private fun String.info() = Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

@Singleton
@RequiresApi(Build.VERSION_CODES.R)
class AdbDiscoveryManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val _discoveredServices = MutableStateFlow(DiscoveredAdbServices())
    val discoveredServices = _discoveredServices.asStateFlow()
    val discoveredConnectService = discoveredServices.map { it.connectService }.distinctUntilChanged()
    val discoveredPairingService = discoveredServices.map { it.pairingService }.distinctUntilChanged()
    private val _connectServiceUpdates = MutableSharedFlow<AdbService?>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectServiceUpdates = _connectServiceUpdates.asSharedFlow()
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val multicastLock = wifiManager.createMulticastLock("adb_discovery_lock")

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val pairingListener = createDiscoveryListener(ServiceType.PAIRING)
    private val connectListener = createDiscoveryListener(ServiceType.CONNECT)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startDiscovery() {
        if (!_isSearching.compareAndSet(expect = false, update = true)) return

        "Starting discovery.".info()
        _connectServiceUpdates.resetReplayCache()
        multicastLock.setReferenceCounted(true)
        multicastLock.acquire()
        nsdManager.discoverServices(ServiceType.PAIRING.value, NsdManager.PROTOCOL_DNS_SD, pairingListener)
        nsdManager.discoverServices(ServiceType.CONNECT.value, NsdManager.PROTOCOL_DNS_SD, connectListener)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stopDiscovery() {
        if (!_isSearching.compareAndSet(expect = true, update = false)) return

        "Stopping discovery.".info()
        if (multicastLock.isHeld) {
            multicastLock.release()
        }
        try {
            nsdManager.stopServiceDiscovery(pairingListener)
            nsdManager.stopServiceDiscovery(connectListener)
        } catch (e: Exception) {
            "Exception while stopping service discovery".error(e)
        }
        _discoveredServices.value = DiscoveredAdbServices()
        _connectServiceUpdates.resetReplayCache()
    }

    private fun createDiscoveryListener(type: ServiceType): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onServiceFound(service: NsdServiceInfo) {
                "[${type.name}] onServiceFound: ${service.serviceName}".info()
                if (!service.serviceType.startsWith(type.value)) return

                "[${type.name}] Service type matches. Resolving...".debug()
                nsdManager.resolveService(service, createResolveListener(type))
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                "[${type.name}] onServiceLost: ${service.serviceName}".warn()
                _discoveredServices.update {
                    when (type) {
                        ServiceType.PAIRING -> it.copy(pairingService = null)
                        ServiceType.CONNECT -> it.copy(connectService = null)
                    }
                }
                if (type == ServiceType.CONNECT) {
                    _connectServiceUpdates.tryEmit(null)
                }
            }

            override fun onDiscoveryStarted(regType: String) {
                "[${type.name}] onDiscoveryStarted: $regType".info()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                "[${type.name}] onDiscoveryStopped: $serviceType".info()
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                "[${type.name}] onStartDiscoveryFailed for $serviceType. Error Code: $errorCode".error()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                "[${type.name}] onStopDiscoveryFailed for $serviceType. Error Code: $errorCode".error()
            }
        }
    }

    private fun createResolveListener(type: ServiceType): NsdManager.ResolveListener {
        val localAddresses by lazy { getLocalIpAddresses() }
        return object : NsdManager.ResolveListener {
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.host !in localAddresses) {
                    "[${type.name}] NOT LOCAL: onServiceResolved: ${serviceInfo.serviceName} -> ${serviceInfo.host}:${serviceInfo.port}".info()
                    return
                }

                "[${type.name}] SUCCESS: onServiceResolved: ${serviceInfo.serviceName} -> ${serviceInfo.host}:${serviceInfo.port}".info()
                val adbService = AdbService(serviceInfo.host, serviceInfo.port)
                _discoveredServices.update {
                    when (type) {
                        ServiceType.PAIRING -> it.copy(pairingService = adbService)
                        ServiceType.CONNECT -> it.copy(connectService = adbService)
                    }
                }
                if (type == ServiceType.CONNECT) {
                    _connectServiceUpdates.tryEmit(adbService)
                }
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                "[${type.name}] onResolveFailed for ${serviceInfo.serviceName}. Error Code: $errorCode".error()
            }
        }
    }

    private fun getLocalIpAddresses(): List<InetAddress> {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .toList()
        } catch (e: Exception) {
            "Could not enumerate network interfaces".error(e)
            emptyList()
        }
    }

    private enum class ServiceType(val value: String) {
        PAIRING("_adb-tls-pairing._tcp"),
        CONNECT("_adb-tls-connect._tcp")
    }
}
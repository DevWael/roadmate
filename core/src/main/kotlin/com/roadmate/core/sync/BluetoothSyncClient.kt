package com.roadmate.core.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BluetoothSyncClient(
    private val adapter: BluetoothAdapter?,
    private val ioDispatcher: CoroutineDispatcher,
) {
    @Inject constructor() : this(
        adapter = BluetoothAdapter.getDefaultAdapter(),
        ioDispatcher = Dispatchers.IO,
    )

    private var activeSocket: BluetoothSocket? = null

    suspend fun connect(): BluetoothSocket? {
        if (adapter == null || !adapter.isEnabled) {
            Timber.w("BluetoothSyncClient: adapter unavailable or disabled")
            return null
        }
        val bondedDevices = try {
            adapter.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            Timber.e(e, "BluetoothSyncClient: permission denied for bonded devices")
            return null
        }
        for (device in bondedDevices) {
            try {
                try { adapter.cancelDiscovery() } catch (_: SecurityException) {}
                val socket = device.createRfcommSocketToServiceRecord(BluetoothSyncServer.ROADMATE_UUID)
                val connected = withTimeoutOrNull(10_000L) {
                    withContext(ioDispatcher) { socket.connect() }
                }
                if (connected == null) {
                    try { socket.close() } catch (_: IOException) {}
                    continue
                }
                activeSocket?.close()
                activeSocket = socket
                Timber.i("BluetoothSyncClient: connected to ${device.address}")
                return socket
            } catch (e: IOException) {
                Timber.d("BluetoothSyncClient: ${device.address} — no RoadMate service")
            } catch (e: SecurityException) {
                Timber.e(e, "BluetoothSyncClient: permission denied for ${device.address}")
            }
        }
        Timber.i("BluetoothSyncClient: no head unit found among bonded devices")
        return null
    }

    fun disconnect() {
        try {
            activeSocket?.close()
        } catch (_: IOException) {}
        activeSocket = null
    }

    fun destroy() {
        disconnect()
    }
}

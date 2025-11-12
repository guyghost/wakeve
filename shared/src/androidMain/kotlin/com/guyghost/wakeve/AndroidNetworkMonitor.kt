package com.guyghost.wakeve

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Moniteur réseau Android
 */
class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline
    
    private val listeners = mutableListOf<(Boolean) -> Unit>()
    
    init {
        updateConnectivityStatus()
        registerNetworkCallback()
    }
    
    override fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    override fun addNetworkListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }
    
    override fun removeNetworkListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }
    
    private fun registerNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                updateConnectivityStatus()
            }
            
            override fun onLost(network: android.net.Network) {
                updateConnectivityStatus()
            }
            
            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateConnectivityStatus()
            }
        }
        
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
    }
    
    private fun updateConnectivityStatus() {
        val isOnline = isNetworkAvailable()
        _isOnline.postValue(isOnline)
        
        // Notifier les listeners
        listeners.forEach { it(isOnline) }
    }
}

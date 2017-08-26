package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.runCmd

object WifiNetwork {
	val MacAddressRegex = Regex("(\\w{2}):(\\w{2}):(\\w{2}):(\\w{2}):(\\w{2}):(\\w{2})")

	fun startHostedNetwork(ssid: String, password: String): Boolean {
		if (!runCmd("netsh", "wlan", "set", "hostednetwork", "mode=allow", "ssid=$ssid", "key=$password", "keyUsage=temporary").success) return false
		return runCmd("netsh", "wlan", "start", "hostednetwork").success
	}

	fun stopHostedNetwork(): Boolean {
		return runCmd("netsh", "wlan", "stop", "hostednetwork").success
	}

	//"netsh wlan show hostednetwork"
	fun checkHostedNetwork(): Boolean {
		// Since the result could be localized, we check for a mac address to determine whether it is startede or not
		return MacAddressRegex.find(runCmd("netsh", "wlan", "show", "hostednetwork").outputError) != null
	}

	fun getDeviceIp() {
		//"netsh wlan show hostednetwork"
		//arp -a
	}
}
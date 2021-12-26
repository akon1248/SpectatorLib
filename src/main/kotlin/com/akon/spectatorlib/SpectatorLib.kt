package com.akon.spectatorlib

import org.bukkit.plugin.java.JavaPlugin

class SpectatorLib : JavaPlugin() {

	companion object {

		@JvmStatic
		lateinit var manager: SpectatorManager
			private set

	}

	override fun onEnable() {
		manager = SpectatorManager(this)
	}

}

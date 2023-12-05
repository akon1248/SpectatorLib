package com.akon.spectatorlib

import com.comphenix.protocol.PacketType.Play.Server
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FuzzyReflection
import com.comphenix.protocol.utility.MinecraftVersion
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.PlayerInfoData
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.server.PluginDisableEvent

class SpectatorListener(private val manager: SpectatorManager): PacketAdapter(manager.plugin,
	Server.GAME_STATE_CHANGE,
	Server.LOGIN,
	Server.NAMED_ENTITY_SPAWN,
	Server.PLAYER_INFO,
	Server.RESPAWN
), Listener {

	companion object {

		//1.15以下ではクラスが存在しないので
		//lazyで使われない場合の初期化を防ぐ
		private val GAME_STATE_TYPE_CLASS by lazy { Server.GAME_STATE_CHANGE.packetClass.classes[0] }
		private val GAME_STATE_ID_FIELD by lazy {
			FuzzyReflection.fromClass(GAME_STATE_TYPE_CLASS, true)
				.getFieldByType("id", Int::class.javaPrimitiveType)
				.apply { isAccessible = true }
		}

		private fun getGameStateId(gameStatePacket: PacketContainer): Int {
			require(gameStatePacket.type == Server.GAME_STATE_CHANGE)
			return if (MinecraftVersion.BEE_UPDATE.atOrAbove()) {
				GAME_STATE_ID_FIELD.get(gameStatePacket.getSpecificModifier(GAME_STATE_TYPE_CLASS).read(0)) as Int
			} else {
				gameStatePacket.integers.read(0)
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	fun onGameModeChange(event: PlayerGameModeChangeEvent) {
		val player = event.player
		if (this.manager.isSpectator(player)) {
			val prevent = player.getMetadata("PreventDisablingSpectator").find {
				it.owningPlugin == this.plugin
			}?.asBoolean() == true
			if (prevent) {
				player.removeMetadata("PreventDisablingSpectator", this.plugin)
				return
			}
			this.manager.setSpectator(player, false)
		}
	}

	@EventHandler
	fun onDisable(event: PluginDisableEvent) {
		if (event.plugin == this.plugin) {
			HandlerList.getHandlerLists().forEach { it.unregister(this) }
			ProtocolLibrary.getProtocolManager().removePacketListener(this)
		}
	}

	override fun onPacketSending(event: PacketEvent) {
		val packet = event.packet
		val player = event.player
		when (event.packetType) {
			//ゲームモードの偽装
			Server.GAME_STATE_CHANGE -> if (this.manager.isSpectator(player) && getGameStateId(packet) == 3) {
				packet.float.write(0, 2.0F)
			}
			Server.LOGIN, Server.RESPAWN -> if (this.manager.isSpectator(player)) {
				val modifier = packet.gameModes
				modifier.write(0, EnumWrappers.NativeGameMode.ADVENTURE)
				if (MinecraftVersion.NETHER_UPDATE.atOrAbove()) {
					modifier.write(1, EnumWrappers.NativeGameMode.fromBukkit(this.manager.getPreviousMode(player)))
				}
			}
			//バニラのスペクテイターモードの時は見えないように
			Server.NAMED_ENTITY_SPAWN -> packet.getEntityModifier(event).read(0)?.let {
				if (it is Player && this.manager.isSpectator(it) && !this.manager.isSpectator(player)) event.isCancelled = true
			}
			Server.PLAYER_INFO -> {
				//プレイヤーリストのゲームモードの偽装
				//これを行わないとブロックをすり抜けられる
				val modifier = packet.playerInfoDataLists
				val index = if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) 1 else 0
				modifier.write(index, modifier.read(index)
					.map {
						if (Bukkit.getPlayer(it.profile.uuid)?.let(this.manager::isSpectator) == true)
							if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
								PlayerInfoData(it.profileId, it.latency, it.isListed, EnumWrappers.NativeGameMode.ADVENTURE, it.profile, it.displayName, it.remoteChatSessionData)
							} else {
								PlayerInfoData(it.profile, it.latency, EnumWrappers.NativeGameMode.ADVENTURE, it.displayName, it.profileKeyData)
						} else it
					}
				)
			}
		}
	}

}

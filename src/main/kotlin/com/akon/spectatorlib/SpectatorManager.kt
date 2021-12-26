package com.akon.spectatorlib

import com.comphenix.protocol.ProtocolLibrary
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class SpectatorManager(val plugin: Plugin) {

	init {
		SpectatorListener(this).let {
			Bukkit.getPluginManager().registerEvents(it, this.plugin)
			ProtocolLibrary.getProtocolManager().addPacketListener(it)
		}
	}

	private val spectatorKey = NamespacedKey(this.plugin, "spectator")

	/**
	 * スペクテイター状態を変更します
	 *
	 * @param player スペクテイター状態を変更するプレイヤー
	 * @param spectator プレイヤーをスペクテイター状態にするかどうか
	 * @return 現在のスペクテイター状態と入力された値が一致した場合false、そうでなければtrue
	 */
	fun setSpectator(player: Player, spectator: Boolean): Boolean {
		val previousMode = this.getPreviousMode(player)
		if ((previousMode != null) == spectator) return false
		val dataContainer = player.persistentDataContainer
		if (spectator) {
			val mode = player.gameMode
			//ここで先にPersistentDataを書き換えることでゲームモードを変更してもパケット介入でアドベンチャーモードに見える
			dataContainer.set(this.spectatorKey, PersistentDataType.INTEGER, mode.value)
			player.gameMode = GameMode.SPECTATOR
			player.isInvulnerable = true
			player.isInvisible = true
			player.allowFlight = true
			player.teleport(player.location.add(0.0, 0.01, 0.0))
			player.isFlying = true
		} else {
			player.isInvulnerable = false
			player.isInvisible = false
			dataContainer.remove(this.spectatorKey)
			player.gameMode = previousMode!!

		}
		return true
	}

	private fun getPreviousMode(player: Player) = player.persistentDataContainer.get(this.spectatorKey, PersistentDataType.INTEGER)?.let(GameMode::getByValue)

	/**
	 * プレイヤーがスペクテイター状態かどうかを確認します
	 *
	 * @param player スペクテイター状態を確認するプレイヤー
	 * @return 指定されたプレイヤーがスペクテイター状態かどうか
	 */
	fun isSpectator(player: Player) = this.getPreviousMode(player) != null

}
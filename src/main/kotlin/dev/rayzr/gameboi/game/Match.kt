package dev.rayzr.gameboi.game

import dev.rayzr.gameboi.manager.MatchManager
import net.dv8tion.jda.api.entities.MessageChannel
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

val MATCH_TIMEOUT = TimeUnit.MINUTES.toMillis(10)

class Match(val game: Game, val channel: MessageChannel) {
    val timer = Timer()
    var timeout: TimerTask? = null

    val players = mutableListOf<Player>()
    val renderContext = game.createRenderContext(this)
    var data: MatchData? = null

    private fun canJoin(player: Player) = player.currentMatch == null && !players.contains(player) && players.size < game.maxPlayers

    fun addPlayer(player: Player) {
        if (!canJoin(player)) {
            return
        }

        players.add(player)
        MatchManager[player.user] = this
        player.currentMatch = this

        if (players.size >= game.maxPlayers) {
            begin()
        }
    }

    private fun begin() {
        data = game.createData(this)
        game.begin(this)
        players.forEach {
            it.editData {
                updateStatBy("games-played.${game.name}", 1)
                updateStatBy("games-played.total", 1)
            }
        }

        // Start timer
        bumpTimeout()
    }

    fun end() {
        players.forEach {
            MatchManager.remove(it.user)
            it.currentMatch = null
        }

        timeout?.cancel()
    }

    fun bumpTimeout() {
        timeout?.cancel()
        timeout = timer.schedule(MATCH_TIMEOUT) {
            channel.sendMessage(":x: Your match has timed out!").queue()
            end()
        }
    }
}
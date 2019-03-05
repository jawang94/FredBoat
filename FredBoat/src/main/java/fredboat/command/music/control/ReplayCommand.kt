package fredboat.command.music.control


import fredboat.audio.player.GuildPlayer
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.sentinel.Guild
import fredboat.sentinel.Member
import fredboat.util.TextUtils
import fredboat.util.extension.escapeAndDefuse
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.regex.Pattern

class ReplayCommand : IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.USER

    override suspend fun invoke(context: CommandContext) {
        val player = Launcher.botController.playerRegistry.getExisting(context.guild)

        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("replayEmpty"))
            return
        }

        if (isOnCooldown(context.guild)) {
            return
        } else {
            guildIdToLastSkip[context.guild.id] = System.currentTimeMillis()
        }

        if (!context.hasArguments()) {
            replayLast(player, context)
        } else {
            HelpCommand.sendFormattedCommandHelp(context)
        }
    }

    /**
     * Specifies whether the <B>skip command </B>is on cooldown.
     *
     * @param guild The guild where the <B>skip command</B> was called.
     * @return `true` if the elapsed time since the <B>skip command</B> is less than or equal to
     * [.SKIP_COOLDOWN]; otherwise, `false`.
     */
    private fun isOnCooldown(guild: Guild): Boolean {
        val currentTIme = System.currentTimeMillis()
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.id, 0L) <= SKIP_COOLDOWN
    }

    private suspend fun replayLast(player: GuildPlayer, context: CommandContext) {
        val atc = player.playingTrack
        if (atc == null) {
            context.reply(context.i18n("replayTrackNotFound"))
        } else {
            val successMessage = context.i18nFormat("replaySuccess", 1,
                    TextUtils.escapeAndDefuse(atc.effectiveTitle))
            player.replayLastTrackForMemberPerms(context, listOf(atc.trackId), successMessage)
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} OR {0}{1} n OR {0}{1} n-m OR {0}{1} @Users...\n#"
        return usage + context.i18n("helpSkipCommand")
    }

    companion object {
        private const val TRACK_RANGE_REGEX = "^(0?\\d+)-(0?\\d+)$"
        private val trackRangePattern = Pattern.compile(TRACK_RANGE_REGEX)

        /**
         * Represents the relationship between a **guild's id** and **skip cooldown**.
         */
        private val guildIdToLastSkip = HashMap<Long, Long>()

        /**
         * The default cooldown for calling the [.onInvoke] method in milliseconds.
         */
        private const val SKIP_COOLDOWN = 500
    }
}
package dev.schlaubi.musicbot.module.gdpr

import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import dev.schlaubi.musicbot.core.io.findUser
import dev.schlaubi.musicbot.utils.database

fun GDPRModule.requestCommand() = ephemeralSubCommand {
    name = "request"
    description = "Requests your persistently stored data"

    action {
        respond {
            embed {
                val discordUser = user.asUser()
                val botUser = database.users.findUser(discordUser)
                author {
                    icon = discordUser.avatar.url
                    name = translate("commands.gdpr.request.title", arrayOf(discordUser.username))
                }

                field {
                    name = translate("commands.gdpr.request.id")
                    value = discordUser.id.asString
                }

                field {
                    name = translate("commands.gdpr.request.language")
                    value = botUser.language.getDisplayName(botUser.language)
                }

                if (botUser.defaultSchedulerSettings != null) {
                    field {
                        name = translate("commands.gdpr.request.scheduler_settings")
                        name = botUser.defaultSchedulerSettings.toString()
                    }
                }

                if (botUser.unoStats != null) {
                    field {
                        name = translate("commands.gdpr.request.uno_stats")
                        value = botUser.unoStats.toString()
                    }
                }
                if (botUser.quizStats != null) {
                    field {
                        name = translate("commands.gdpr.request.quiz_stats")
                        value = botUser.quizStats.toString()
                    }
                }
            }
        }
    }
}

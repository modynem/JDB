package Services;

import Componants.ErrorEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.EnumSet;
import java.util.Objects;

public class Channel {
    public void Lock (SlashCommandInteractionEvent event) {
        ErrorEmbed errorEmbed = new ErrorEmbed();

        assert event.getGuild() != null;

        TextChannel channel = event.getChannel().asTextChannel();
        Role everyone = Objects.requireNonNull(event.getGuild()).getPublicRole();

        channel.getManager()
                .putPermissionOverride(everyone, null, EnumSet.of(Permission.MESSAGE_SEND))
                .queue(
                        success -> event.replyEmbeds(
                                        new EmbedBuilder().setTitle("✅ Successfully locked!")
                                                .setColor(Color.green)
                                                .build()
                                )
                                .queue(),
                        error -> event.replyEmbeds(
                                        errorEmbed.Error("❌ Failed: "+ error.getMessage(), "", "", ""))
                                .setEphemeral(true)
                                .queue()
                );
    }

    public void Unlock (SlashCommandInteractionEvent event) {
        ErrorEmbed errorEmbed = new ErrorEmbed();

        assert event.getGuild() != null;

        TextChannel channel1 = event.getChannel().asTextChannel();
        Role everyone = Objects.requireNonNull(event.getGuild()).getPublicRole();

        channel1.getManager()
                .putPermissionOverride(everyone, EnumSet.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL), null)
                .queue(
                        success -> event.replyEmbeds(
                                        new EmbedBuilder().setTitle("✅ Successfully unlocked!")
                                                .setColor(Color.green)
                                                .build()
                                )
                                .queue(),
                        error -> event.replyEmbeds(
                                        errorEmbed.Error("❌ Failed: "+ error.getMessage(), "", "", ""))
                                .setEphemeral(true)
                                .queue()
                );
    }
}

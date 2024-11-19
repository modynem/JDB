package Events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.lang.management.ManagementFactory;

public class BotStatusMessage {
    public static void sendStatusEmbed(JDA jda, long guildId, long channelId) {
        try {
            // Get the specific guild and channel
            TextChannel channel = jda.getGuildById(guildId)
                    .getTextChannelById(channelId);

            // Create embed
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🤖 Bot Status")
                    .setColor(Color.lightGray)
                    .addField("Total Servers", String.valueOf(jda.getGuilds().size()), true)
                    .addField("Total Users", String.valueOf(
                            jda.getGuilds().stream()
                                    .mapToInt(guild -> guild.getMemberCount())
                                    .sum()
                    ), true)
                    .addField("Uptime", calculateUptime(), true)
                    .setFooter("Last updated", jda.getSelfUser().getAvatarUrl())
                    .setTimestamp(java.time.Instant.now());

            // Send the embed
            channel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String calculateUptime() {
        // Simple uptime calculation (you might want to enhance this)
        long uptimeMillis = System.currentTimeMillis() - ManagementFactory
                .getRuntimeMXBean()
                .getStartTime();

        long days = uptimeMillis / (24 * 60 * 60 * 1000);
        long hours = (uptimeMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);

        return days + " days, " + hours + " hours";
    }
}
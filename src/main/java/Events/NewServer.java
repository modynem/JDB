package Events;

import Database.MongoDB;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NewServer extends ListenerAdapter {
    private final MongoDB mongodb;
    private static final long UPDATE_DELAY = TimeUnit.SECONDS.toMillis(2);
    private long lastUpdateTime = 0;

    public NewServer(MongoDB mongodb) {
        this.mongodb = mongodb;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();

        // Find the best channel to send the sync message
        TextChannel channel = findSuitableChannel(guild);
        if (channel == null) return;

        // Send initial embed
        EmbedBuilder initialEmbed = new EmbedBuilder()
                .setTitle("Database Synchronization Started")
                .setDescription("Starting to sync server members...")
                .setColor(Color.lightGray)
                .addField("Status", "Initializing...", false)
                .setTimestamp(Instant.now())
                .setFooter("Server: " + guild.getName(), guild.getIconUrl());

        channel.sendMessageEmbeds(initialEmbed.build()).queue(statusMessage -> {
            // Load and process members
            guild.loadMembers().onSuccess(members ->
                    processMemberSync(members, statusMessage, guild));
        });
    }

    private TextChannel findSuitableChannel(Guild guild) {
        // Try to get system channel first
        TextChannel channel = guild.getSystemChannel();

        // If no system channel, try to find first channel bot can write to
        if (channel == null) {
            channel = guild.getTextChannels().stream()
                    .filter(textChannel -> textChannel.canTalk())
                    .findFirst()
                    .orElse(null);
        }
        return channel;
    }

    private void processMemberSync(List<Member> members, Message statusMessage, Guild guild) {
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger newEntriesCount = new AtomicInteger(0);
        int totalMembers = members.size();

        try {
            for (Member member : members) {
                // Skip bots
                if (member.getUser().isBot()) {
                    processedCount.incrementAndGet();
                    continue;
                }

                // Process member
                try {
                    // Check if member exists in database
                    Document existingData = mongodb.getDoc(member.getId(), guild.getId());

                    if (existingData == null) {
                        // Add new member to database
                        mongodb.makeNewData(member, guild);
                        newEntriesCount.incrementAndGet();
                    }

                    processedCount.incrementAndGet();

                    // Update status message periodically
                    if (shouldUpdateStatus()) {
                        updateStatusEmbed(statusMessage, processedCount.get(), totalMembers,
                                newEntriesCount.get(), guild);
                    }

                    // Small delay to prevent rate limiting
                    Thread.sleep(100);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Send final status
            sendFinalEmbed(statusMessage, processedCount.get(), totalMembers,
                    newEntriesCount.get(), guild);

        } catch (Exception e) {
            sendErrorEmbed(statusMessage, guild);
            e.printStackTrace();
        }
    }

    private boolean shouldUpdateStatus() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= UPDATE_DELAY) {
            lastUpdateTime = currentTime;
            return true;
        }
        return false;
    }

    private void updateStatusEmbed(Message message, int processed, int total,
                                   int newEntries, Guild guild) {
        double progress = (double) processed / total * 100;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Syncing Members")
                .setDescription("Adding members to database...")
                .setColor(Color.lightGray)
                .addField("Progress", String.format("%d/%d members (%.1f%%)",
                        processed, total, progress), false)
                .addField("New Entries", String.valueOf(newEntries), false)
                .setTimestamp(Instant.now())
                .setFooter("Server: " + guild.getName(), guild.getIconUrl());

        message.editMessageEmbeds(embed.build()).queue();
    }

    private void sendFinalEmbed(Message message, int processed, int total,
                                int newEntries, Guild guild) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Synchronization Completed")
                .setDescription("Successfully synced server members!")
                .setColor(Color.lightGray)
                .addField("Total Members Processed", processed + "/" + total, false)
                .addField("New Entries Added", String.valueOf(newEntries), false)
                .setTimestamp(Instant.now())
                .setFooter("Server: " + guild.getName(), guild.getIconUrl());

        message.editMessageEmbeds(embed.build()).queue();
    }

    private void sendErrorEmbed(Message message, Guild guild) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Synchronization Error")
                .setDescription("An error occurred while syncing members.")
                .setColor(Color.RED)
                .addField("Status", "Failed", false)
                .addField("Action Required", "Please contact bot administrators", false)
                .setTimestamp(Instant.now())
                .setFooter("Server: " + guild.getName(), guild.getIconUrl());

        message.editMessageEmbeds(embed.build()).queue();
    }
}
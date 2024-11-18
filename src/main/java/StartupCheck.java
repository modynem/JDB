import Database.MongoDB;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.awt.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StartupCheck extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger(StartupCheck.class.getName());
    private final MongoDB memberListener;
    private final Map<String, Long> lastUpdateTime = new HashMap<>();
    private static final long UPDATE_DELAY = TimeUnit.SECONDS.toMillis(5); // Increased delay between updates
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    public StartupCheck(MongoDB memberListener) {
        this.memberListener = memberListener;
    }

    @Override
    public void onReady(ReadyEvent event) {
        List<Guild> guilds = event.getJDA().getGuilds();
        AtomicInteger totalNewEntries = new AtomicInteger(0);
        AtomicInteger totalProcessedGuilds = new AtomicInteger(0);

        for (Guild guild : guilds) {
            processGuild(guild, totalNewEntries, totalProcessedGuilds, guilds.size());
        }
    }

    private void processGuild(Guild guild, AtomicInteger totalNewEntries, AtomicInteger totalProcessedGuilds, int totalGuilds) {
        TextChannel channel = guild.getSystemChannel() != null ?
                guild.getSystemChannel() :
                guild.getTextChannels().stream().findFirst().orElse(null);

        if (channel == null) {
            LOGGER.warning("No suitable channel found for guild: " + guild.getName());
            return;
        }

        EmbedBuilder initialEmbed = new EmbedBuilder()
                .setTitle("Database Sync Progress")
                .setColor(Color.lightGray)
                .setDescription("Starting database synchronization...")
                .addField("Status", "Initializing...", true)
                .setTimestamp(Instant.now());

        sendMessageWithRetry(channel, initialEmbed.build(), 0)
                .thenAccept(statusMessage -> {
                    guild.loadMembers().onSuccess(members -> {
                        processGuildMembers(
                                members,
                                guild,
                                statusMessage,
                                totalNewEntries,
                                totalProcessedGuilds,
                                totalGuilds
                        );
                    }).onError(error -> LOGGER.severe("Error loading members for guild " + guild.getName() + ": " + error.getMessage()));
                });
    }

    private void processGuildMembers(
            List<Member> members,
            Guild guild,
            Message statusMessage,
            AtomicInteger totalNewEntries,
            AtomicInteger totalProcessedGuilds,
            int totalGuilds
    ) {
        AtomicInteger newEntries = new AtomicInteger(0);
        AtomicInteger processedMembers = new AtomicInteger(0);

        for (Member member : members) {
            if (member.getUser().isBot()) continue;

            try {
                if (shouldUpdateStatus(guild.getId())) {
                    updateStatusMessageWithRetry(
                            statusMessage,
                            guild,
                            processedMembers.get(),
                            members.size(),
                            newEntries.get(),
                            0
                    );
                }

                processMember(member, guild, newEntries, totalNewEntries);
                processedMembers.incrementAndGet();

                // Add a small delay between processing members
                Thread.sleep(100);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing member " + member.getId(), e);
            }
        }

        finishGuildProcessing(statusMessage, guild, processedMembers.get(), members.size(),
                newEntries.get(), totalProcessedGuilds, totalGuilds);
    }

    private void processMember(Member member, Guild guild, AtomicInteger newEntries, AtomicInteger totalNewEntries) {
        try {
            Document data = memberListener.getDoc(member.getId(), guild.getId());
            if (data == null) {
                memberListener.makeNewData(member, guild);
                newEntries.incrementAndGet();
                totalNewEntries.incrementAndGet();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing member data for " + member.getId(), e);
        }
    }

    private void updateStatusMessageWithRetry(
            Message message,
            Guild guild,
            int processed,
            int total,
            int newEntries,
            int retryCount
    ) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Processing Guild: " + guild.getName())
                .setColor(Color.lightGray)
                .setDescription("Syncing member data with database...")
                .addField("Progress", processed + "/" + total + " members", true)
                .addField("New Entries", String.valueOf(newEntries), true)
                .setTimestamp(Instant.now())
                .setFooter("Processing...", null);

        message.editMessageEmbeds(embed.build()).queue(
                success -> {},
                error -> {
                    if (error instanceof ErrorResponseException && retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY);
                            updateStatusMessageWithRetry(message, guild, processed, total, newEntries, retryCount + 1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        LOGGER.severe("Failed to update status message after " + MAX_RETRIES + " retries: " + error.getMessage());
                    }
                }
        );
    }

    private void finishGuildProcessing(
            Message statusMessage,
            Guild guild,
            int processed,
            int total,
            int newEntries,
            AtomicInteger totalProcessedGuilds,
            int totalGuilds
    ) {
        totalProcessedGuilds.incrementAndGet();

        EmbedBuilder guildComplete = new EmbedBuilder()
                .setTitle("Guild Sync Completed")
                .setDescription("Bot Rebooted!")
                .setColor(Color.lightGray)
                .addField("Guild", guild.getName(), true)
                .addField("Members Processed", processed + "/" + total, true)
                .addField("New Entries", String.valueOf(newEntries), true)
                .setTimestamp(Instant.now());

        statusMessage.editMessageEmbeds(guildComplete.build()).queue(
                success -> {
                    if (totalProcessedGuilds.get() == totalGuilds &&
                            Objects.equals(statusMessage.getGuildId(), "1248222329398493275")) {
                        sendFinalReport(statusMessage, totalGuilds, totalProcessedGuilds.get());
                    }
                },
                error -> LOGGER.severe("Failed to send final guild update: " + error.getMessage())
        );
    }

    private void sendFinalReport(Message statusMessage, int totalGuilds, int totalNewEntries) {
        TextChannel reportChannel = statusMessage.getGuild().getTextChannelById("1307414538886582413");
        if (reportChannel != null) {
            EmbedBuilder finalReport = new EmbedBuilder()
                    .setTitle("Global Database Sync Completed")
                    .setDescription("Bot Rebooted!")
                    .setColor(Color.lightGray)
                    .addField("Total Guilds", String.valueOf(totalGuilds), true)
                    .addField("Total New Entries", String.valueOf(totalNewEntries), true)
                    .setTimestamp(Instant.now());

            sendMessageWithRetry(reportChannel, finalReport.build(), 0);
        }
    }

    private boolean shouldUpdateStatus(String guildId) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(guildId);

        if (lastUpdate == null || currentTime - lastUpdate >= UPDATE_DELAY) {
            lastUpdateTime.put(guildId, currentTime);
            return true;
        }
        return false;
    }

    private java.util.concurrent.CompletableFuture<Message> sendMessageWithRetry(TextChannel channel, net.dv8tion.jda.api.entities.MessageEmbed embed, int retryCount) {
        return channel.sendMessageEmbeds(embed).submit()
                .exceptionally(error -> {
                    if (error instanceof ErrorResponseException && retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY);
                            return sendMessageWithRetry(channel, embed, retryCount + 1).join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                    LOGGER.severe("Failed to send message after " + MAX_RETRIES + " retries: " + error.getMessage());
                    throw new RuntimeException(error);
                });
    }
}
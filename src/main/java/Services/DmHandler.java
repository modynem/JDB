package Services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DmHandler {
    // Rate limiting constants
    private static final int MAX_DMS_PER_MINUTE = 10;
    private static final int COOLDOWN_MINUTES = 5;
    private static final long UPDATE_INTERVAL = 2000;
    private static final int BATCH_SIZE = 1000;
    private static final long BATCH_COOLDOWN_MINUTES = 30; // Cooldown between batches

    private final Queue<Member> dmQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, Instant> lastDmTime = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;
    private Message progressMessage;
    private ScheduledExecutorService scheduler;
    private boolean isProcessing = false;

    private class DmProgress {
        private final AtomicInteger success = new AtomicInteger(0);
        private final AtomicInteger failed = new AtomicInteger(0);
        private final AtomicInteger processed = new AtomicInteger(0);
        private final int total;
        private final long startTime;
        private final int currentBatch;
        private final int totalBatches;
        private final Map<String, String> failureReasons = new ConcurrentHashMap<>();

        public DmProgress(int total, int currentBatch, int totalBatches) {
            this.total = total;
            this.startTime = System.currentTimeMillis();
            this.currentBatch = currentBatch;
            this.totalBatches = totalBatches;
        }

        public void addFailureReason(String userId, String reason) {
            failureReasons.put(userId, reason);
        }
    }

    public void HandleDm(SlashCommandInteractionEvent event) {
        assert event.getGuild() != null;

        event.deferReply(true).queue();

        try {
            Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
            String message = Objects.requireNonNull(event.getOption("message")).getAsString();

            if (!isMessageSafe(message)) {
                sendErrorEmbed(event, "Message contains prohibited content or is too long.");
                return;
            }

            Message.Attachment attachment = event.getOption("attachment") != null ?
                    Objects.requireNonNull(event.getOption("attachment")).getAsAttachment() : null;

            if (attachment != null && !isAttachmentSafe(attachment)) {
                sendErrorEmbed(event, "Attachment type not allowed or file too large.");
                return;
            }

            EmbedBuilder initialEmbed = new EmbedBuilder()
                    .setTitle("DM Task Initiated")
                    .setDescription("Loading member list...")
                    .setColor(Color.lightGray)
                    .setTimestamp(Instant.now());

            event.getHook().editOriginalEmbeds(initialEmbed.build()).queue(msg -> progressMessage = msg);

            if (role.isPublicRole()) {
                event.getGuild().loadMembers()
                        .onSuccess(members -> processInBatches(members, event, message, attachment, role))
                        .onError(error -> sendErrorEmbed(event, "Failed to load members: " + error.getMessage()));
            } else {
                event.getGuild().findMembers(member -> member.getRoles().contains(role))
                        .onSuccess(members -> processInBatches(members, event, message, attachment, role))
                        .onError(error -> sendErrorEmbed(event, "Failed to load members: " + error.getMessage()));
            }

        } catch (Exception e) {
            sendErrorEmbed(event, "An error occurred: " + e.getMessage());
        }
    }

    private void processInBatches(java.util.List<Member> members, SlashCommandInteractionEvent event,
                                  String message, Message.Attachment attachment, Role role) {
        java.util.List<Member> validMembers = members.stream()
                .filter(member -> !member.getUser().isBot())
                .filter(this::canReceiveDm)
                .collect(Collectors.toList());

        if (validMembers.isEmpty()) {
            sendErrorEmbed(event, "No valid members found to send messages to.");
            return;
        }

        int totalMembers = validMembers.size();
        int totalBatches = (int) Math.ceil((double) totalMembers / BATCH_SIZE);

        EmbedBuilder batchInfoEmbed = new EmbedBuilder()
                .setTitle("DM Task Information")
                .setDescription(String.format("Total members to process: %d\nTotal batches: %d\nBatch size: %d\nCooldown between batches: %d minutes",
                        totalMembers, totalBatches, BATCH_SIZE, BATCH_COOLDOWN_MINUTES))
                .setColor(Color.lightGray)
                .setTimestamp(Instant.now());

        event.getHook().editOriginalEmbeds(batchInfoEmbed.build()).queue(msg -> {
            processBatch(validMembers, 0, totalBatches, event, message, attachment, role);
        });
    }

    private void processBatch(java.util.List<Member> allMembers, int batchIndex, int totalBatches,
                              SlashCommandInteractionEvent event, String message, Message.Attachment attachment, Role role) {
        int startIndex = batchIndex * BATCH_SIZE;
        int endIndex = Math.min(startIndex + BATCH_SIZE, allMembers.size());
        java.util.List<Member> batchMembers = allMembers.subList(startIndex, endIndex);

        DmProgress progress = new DmProgress(batchMembers.size(), batchIndex + 1, totalBatches);
        dmQueue.clear(); // Clear any remaining members from previous batch
        dmQueue.addAll(batchMembers);

        if (!isProcessing) {
            isProcessing = true;
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
            scheduler = Executors.newSingleThreadScheduledExecutor();
            processDmQueue(message, attachment, progress, event, role, () -> {
                // Batch completion callback
                if (batchIndex + 1 < totalBatches) {
                    // Schedule next batch after cooldown
                    EmbedBuilder cooldownEmbed = new EmbedBuilder()
                            .setTitle("Batch Cooldown")
                            .setDescription(String.format("Batch %d/%d completed. Waiting %d minutes before starting next batch...",
                                    batchIndex + 1, totalBatches, BATCH_COOLDOWN_MINUTES))
                            .setColor(Color.ORANGE)
                            .setTimestamp(Instant.now());
                    event.getHook().editOriginalEmbeds(cooldownEmbed.build()).queue();

                    CompletableFuture.delayedExecutor(BATCH_COOLDOWN_MINUTES, TimeUnit.MINUTES).execute(() -> {
                        processBatch(allMembers, batchIndex + 1, totalBatches, event, message, attachment, role);
                    });
                }
            });
        }
    }

    private void processDmQueue(String message, Message.Attachment attachment, DmProgress progress,
                                SlashCommandInteractionEvent event, Role role, Runnable onBatchComplete) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (dmQueue.isEmpty()) {
                    if (progress.processed.get() >= progress.total) {
                        scheduler.shutdown();
                        isProcessing = false;
                        onBatchComplete.run();
                    }
                    return;
                }

                if (canSendDm()) {
                    Member member = dmQueue.poll();
                    if (member != null) {
                        sendDmToMember(member, message, attachment, progress, event, role);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 60000 / MAX_DMS_PER_MINUTE, TimeUnit.MILLISECONDS);
    }

    private boolean canSendDm() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);
        lastDmTime.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        return lastDmTime.size() < MAX_DMS_PER_MINUTE;
    }

    private void sendDmToMember(Member member, String message, Message.Attachment attachment,
                                DmProgress progress, SlashCommandInteractionEvent event, Role role) {
        member.getUser().openPrivateChannel().queue(
                channel -> {
                    CompletableFuture<Message> sendFuture;

                    if (attachment != null) {
                        sendFuture = attachment.getProxy().download()
                                .thenCompose(is -> {
                                    try {
                                        Path tempFile = Files.createTempFile("attachment", attachment.getFileName());
                                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                                        return channel.sendMessage(message)
                                                .addFiles(FileUpload.fromData(tempFile))
                                                .submit()
                                                .thenApply(msg -> {
                                                    try {
                                                        Files.delete(tempFile);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    return msg;
                                                });
                                    } catch (IOException e) {
                                        throw new CompletionException(e);
                                    }
                                });
                    } else {
                        sendFuture = channel.sendMessage(message).submit();
                    }

                    sendFuture.whenComplete((msg, error) -> {
                        if (error != null) {
                            progress.failed.incrementAndGet();
                            progress.addFailureReason(member.getId(), "Failed to send DM: " + error.getMessage());
                        } else {
                            progress.success.incrementAndGet();
                        }
                        progress.processed.incrementAndGet();
                        lastDmTime.put(member.getIdLong(), Instant.now());
                        updateProgress(progress, new ArrayList<>(), event, role);
                    });
                },
                error -> {
                    progress.failed.incrementAndGet();
                    progress.processed.incrementAndGet();
                    progress.addFailureReason(member.getId(), "Cannot open DM channel: " + error.getMessage());
                    updateProgress(progress, new ArrayList<>(), event, role);
                }
        );
    }

    private void updateProgress(DmProgress progress, List<String> remainingMembers,
                                SlashCommandInteractionEvent event, Role role) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(progress.processed.get() >= progress.total ?
                        String.format("Batch %d/%d Completed", progress.currentBatch, progress.totalBatches) :
                        String.format("Batch %d/%d Progress", progress.currentBatch, progress.totalBatches))
                .setDescription(String.format("Sending messages to %s members", role.getName()))
                .addField("Batch Progress", String.format("%d/%d (%d%%)",
                        progress.processed.get(), progress.total,
                        (progress.processed.get() * 100) / progress.total), true)
                .addField("Success/Failed", String.format(Locale.US,"✅ %d | ❌ %d",
                        progress.success.get(), progress.failed.get()), true)
                .addField("Batch Information", String.format("Current Batch: %d/%d",
                        progress.currentBatch, progress.totalBatches), false)
                .setColor(progress.processed.get() >= progress.total ? Color.GREEN : Color.lightGray)
                .setTimestamp(Instant.now());

        if (!remainingMembers.isEmpty()) {
            String remainingList = remainingMembers.stream()
                    .limit(10)
                    .collect(Collectors.joining("\n"));
            if (remainingMembers.size() > 10) {
                remainingList += String.format("\n...and %d more", remainingMembers.size() - 10);
            }
            embed.addField("Currently Processing", remainingList, false);
        }

        embed.addField("Rate Limit Status",
                String.format("Sending %d messages per minute\nCooldown: %d minutes if limit reached",
                        MAX_DMS_PER_MINUTE, COOLDOWN_MINUTES), false);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void sendErrorEmbed(SlashCommandInteractionEvent event, String errorMessage) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Error")
                .setDescription(errorMessage)
                .setColor(Color.RED)
                .setTimestamp(Instant.now());
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private boolean isMessageSafe(String message) {
        // Null or empty message check
        if (message == null || message.trim().isEmpty()) return false;

        // Check message length
        if (message.length() > 2000) return false;
        // Check for excessive capitalization
        int capsCount = (int) message.chars().filter(Character::isUpperCase).count();
        if (capsCount > message.length() * 0.7) return false;
        // Check for emoji spam
        long emojiCount = message.codePoints()
                .filter(cp -> {
                    // Check if the codepoint is an emoji
                    return (cp >= 0x1F600 && cp <= 0x1F64F) || // Emoticons
                            (cp >= 0x1F300 && cp <= 0x1F5FF) || // Misc Symbols and Pictographs
                            (cp >= 0x1F680 && cp <= 0x1F6FF) || // Transport and Map Symbols
                            (cp >= 0x2600 && cp <= 0x26FF)   || // Misc symbols
                            (cp >= 0x2700 && cp <= 0x27BF)   || // Dingbats
                            (cp >= 0xFE00 && cp <= 0xFE0F)   || // Variation Selectors
                            (cp >= 0x1F900 && cp <= 0x1F9FF);   // Supplemental Symbols and Pictographs
                })
                .count();

        // Allow up to 15 emojis instead of 10
        if (emojiCount > 15) return false;

        // Relaxed link validation
        String lowerMessage = message.toLowerCase();

        // Allow Roblox game links with very permissive validation
        if (lowerMessage.contains("roblox.com/games")) {
            return true;
        }
        System.out.println(4);
        // Block Discord invite links
        if (lowerMessage.matches(".*discord\\.(gg|me|com/invite).*")) return false;

        return true;
    }

    private boolean isAttachmentSafe(Message.Attachment attachment) {
        if (attachment.getSize() > 8_388_608) return false;

        String fileName = attachment.getFileName().toLowerCase();
        Set<String> allowedExtensions = Set.of(
                ".jpg", ".jpeg", ".png", ".gif", ".pdf",
                ".txt", ".doc", ".docx", ".xlsx", ".pptx"
        );

        return allowedExtensions.stream().anyMatch(fileName::endsWith);
    }

    private boolean canReceiveDm(Member member) {
        return !member.getUser().isBot() && !member.getUser().isSystem();
    }
}
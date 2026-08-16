package Commands;

import Database.MongoDB;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.util.List;
import java.util.*;

public class SetupSystem extends ListenerAdapter {
    String mongoUri = "";
    String databaseName = "JDB";
    String collectionName = "Setup";
    private final Map<Long, SetupData> setupStates = new HashMap<>();
    final MongoDB mongoDB = new MongoDB(mongoUri, databaseName, collectionName);

    public static class SetupData {
        int stage = 0;
        String notificationChannelId;
        String adminConfirmationId;
        ArrayList<String> for_hire = new ArrayList<>();
        ArrayList<String> hiring = new ArrayList<>();
        ArrayList<String> selling = new ArrayList<>();
        long messageId;
        InteractionHook lastHook;

        String[] Channels = {
                "scripter",
                "building",
                "graphics",
                "modeling",
                "animating",
                "programming",
                "interface",
                "vfx",
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("setup_marketplace")) {
            event.deferReply(true).queue(hook -> {
                SetupData setupData = new SetupData();
                setupData.lastHook = hook;
                setupStates.put(Objects.requireNonNull(event.getGuild()).getIdLong(), setupData);
                sendStage1Message(hook, event.getChannel());
            });
        }
    }

    private void sendStage1Message(InteractionHook hook, MessageChannelUnion channel) {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("channel-select-notifications")
                .setPlaceholder("Select notifications channel");

        channel.asTextChannel().getGuild().getTextChannels().forEach(textChannel ->
                menuBuilder.addOption(textChannel.getName(), textChannel.getId(), "Set " + textChannel.getName() + " as notifications channel")
        );

        hook.sendMessage("**Server Setup - Stage 1/4**")
                .addActionRow(menuBuilder.build())
                .addActionRow(
                        Button.success("confirm-1", "Confirm"),
                        Button.danger("cancel-1", "Cancel")
                ).setEphemeral(true)
                .queue(message -> {
                    SetupData setupData = setupStates.get(channel.asTextChannel().getGuild().getIdLong());
                    setupData.messageId = message.getIdLong();
                });
    }

    private void sendStage2Message(InteractionHook hook) {
        hook.sendMessage("**Server Setup - Stage 2/4** \nMake a new category **For-Hire**")
                .addActionRow(
                        Button.success("create-2", "Create"),
                        Button.danger("cancel-2", "Cancel")
                )
                .setEphemeral(true)
                .queue();
    }

    private void sendStage3Message(InteractionHook hook) {
        hook.sendMessage("**Server Setup - Stage 3/4** \nMake a new category **Hiring**")
                .addActionRow(
                        Button.success("create-3", "Create"),
                        Button.danger("cancel-3", "Cancel")
                )
                .setEphemeral(true)
                .queue();
    }

    private void sendStage4Message(InteractionHook hook) {
        hook.sendMessage("**Server Setup - Stage 4/4** \nMake a new category **Selling**")
                .addActionRow(
                        Button.success("Create-4", "Create"),
                        Button.danger("cancel-4", "Cancel")
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        SetupData setupData = setupStates.get(guild.getIdLong());
        if (setupData == null) return;

        if (event.getComponentId().equals("channel-select-notifications")) {
            setupData.notificationChannelId = event.getValues().get(0);
            event.deferEdit().queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        SetupData setupData = setupStates.get(guild.getIdLong());
        if (setupData == null) return;

        event.deferEdit().queue(hook -> {
            setupData.lastHook = hook;

            switch (event.getComponentId()) {
                case "confirm-1":
                    if (setupData.notificationChannelId == null) {
                        hook.sendMessage("Please select a channel first!").setEphemeral(true).queue();
                        return;
                    }
                    sendStage2Message(hook);
                    break;

                case "create-2":
                    sendStage3Message(hook);
                    break;

                case "create-3":
                    sendStage4Message(hook);
                    break;

                case "create-4":
                    finishSetup(event, setupData);
                    break;

                case "cancel-1":
                case "cancel-2":
                case "cancel-3":
                case "cancel-4":
                    cancelSetup(event, hook);
                    break;
            }
        });
    }

    private void finishSetup(ButtonInteractionEvent event, SetupData setupData) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        TextChannel channel = event.getChannel().asTextChannel();
        TextChannel notifChannel = guild.getTextChannelById(setupData.notificationChannelId);
        if (notifChannel == null) return;

        guild.createCategory("For-Hire")
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(),
                        List.of(Permission.VIEW_CHANNEL),
                        List.of(Permission.MESSAGE_SEND)
                )
                .queue(category -> {
                    if (setupData.adminConfirmationId == null) {
                        guild.createRole()
                                .setName("Applications")
                                .queue(role -> {
                                    guild.createCategory("Applications")
                                            .addRolePermissionOverride(guild.getPublicRole().getIdLong(),
                                                    Collections.emptyList(),
                                                    Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                                            )
                                            .addRolePermissionOverride(role.getIdLong(),
                                                    Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY),
                                                    Collections.emptyList()
                                            )
                                            .queue(appCategory -> {
                                                appCategory.createTextChannel("posts")
                                                        .queue(channel1 -> {
                                                            setupData.adminConfirmationId = channel1.getId();
                                                            sendCompletionMessage(channel, notifChannel, setupData);
                                                        });
                                            });
                                });

                        for (String channelName : setupData.Channels) {
                            category.createTextChannel(channelName + "-hirable")
                                    .queue(channel1 -> setupData.for_hire.add(channel1.getId()));
                        }
                    } else {
                        sendCompletionMessage(channel, notifChannel, setupData);
                    }
                });
    }

    private void sendCompletionMessage(TextChannel channel, TextChannel notifChannel, SetupData setupData) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Setup Completed!")
                .setDescription(String.format("""
                        Setup completed successfully!
                        Notification Channel: %s
                        For-Hire created successfully
                        Hiring created successfully
                        Selling created successfully
                        """,
                        notifChannel.getAsMention()
                ))
                .setColor(Color.lightGray);

        setupData.lastHook.sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        setupStates.remove(channel.getGuild().getIdLong());
    }

    private void cancelSetup(ButtonInteractionEvent event, InteractionHook hook) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        hook.sendMessage("Setup cancelled. Use `/setup_marketplace` to start again.")
                .setEphemeral(true)
                .queue();
        setupStates.remove(guild.getIdLong());
    }
}

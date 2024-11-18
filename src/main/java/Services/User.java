package Services;

import Database.MongoDB;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.bson.Document;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class User {
    // Store commands and their descriptions
    private static class CommandInfo {
        String name;
        String description;

        public CommandInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    private final List<User.CommandInfo> commands;

    public User() {
        commands = new ArrayList<>();
        // Add all your commands and their descriptions here
        commands.add(new User.CommandInfo("help", "shows the help message"));
        commands.add(new User.CommandInfo("devex_currency", "convert currency to Robux"));
        commands.add(new User.CommandInfo("devex_robux", "convert Robux to currency"));
        commands.add(new User.CommandInfo("kick", "kick a member"));
        commands.add(new User.CommandInfo("warn", "warn a member"));
        commands.add(new User.CommandInfo("get_id", "get server member user ID"));
        commands.add(new User.CommandInfo("game_info", "get game information by universeId"));
        commands.add(new User.CommandInfo("player_info", "get roblox player information by userName e.g. [Followers]"));
        commands.add(new User.CommandInfo("role", "assign a role to a user"));
        commands.add(new User.CommandInfo("lock", "lock the current channel"));
        commands.add(new User.CommandInfo("send_dm", "send a private for a specific role"));
        commands.add(new User.CommandInfo("user_info", "get server member information"));
        commands.add(new User.CommandInfo("gamepasses", "get player gamepasses list by userName"));
        commands.add(new User.CommandInfo("banner", "get an user banner"));
        commands.add(new User.CommandInfo("icon", "get an user icon"));
        commands.add(new User.CommandInfo("server", "see all current server status"));
        commands.add(new User.CommandInfo("ping", "see current bot ping"));
        commands.add(new User.CommandInfo("unlock", "UnLock the current channel"));
        commands.add(new User.CommandInfo("ban", "ban a specific user"));

    }

    public void Kick (SlashCommandInteractionEvent event) {
        assert event.getGuild() != null;

        String reason = event.getOption("reason") == null ? "No reason provided" : Objects.requireNonNull(event.getOption("reason")).getAsString();
        event.reply("Kicking %s for `%s`".formatted(Objects.requireNonNull(Objects.requireNonNull(event.getOption("user")).getAsMember()).getAsMention(), reason)).queue();
        Objects.requireNonNull(Objects.requireNonNull(event.getOption("user")).getAsMember()).kick().queue();
    }

    public void Warn (SlashCommandInteractionEvent event, String mongoUri, String databaseName) {
        assert event.getGuild() != null;

        String collectionName = "members";
        MongoDB memberListener = new MongoDB(mongoUri, databaseName, collectionName);

        Member user1 = Objects.requireNonNull(event.getOption("user")).getAsMember();
        assert user1 != null;
        String reason1 = event.getOption("reason") == null ? "No reason provided" : Objects.requireNonNull(event.getOption("reason")).getAsString();

        Document data1 = memberListener.getDoc(user1.getId(), Objects.requireNonNull(event.getGuild()).getId());

        try {
            if (data1 != null) {
                // Get current warns count
                int currentWarns = Integer.parseInt(data1.get("warns").toString());
                int newWarns = currentWarns + 1;

                // Update the warns in database
                memberListener.getCollection().updateOne(
                        Filters.and(
                                Filters.eq("userId", user1.getId()),
                                Filters.eq("guildId", event.getGuild().getId())
                        ),
                        Updates.set("warns", newWarns)
                );

                if (newWarns >= 3) {
                    // Create the initial reply message
                    event.reply("Banning %s %s".formatted(
                            user1.getAsMention(),
                            "he received `3 or more warns`"
                    )).queue(interactionHook -> {
                        // Get the message through the hook and add reaction
                        interactionHook.retrieveOriginal().queue(message -> {
                            // Reset warns in database
                            memberListener.getCollection().updateOne(
                                    Filters.and(
                                            Filters.eq("userId", user1.getId()),
                                            Filters.eq("guildId", event.getGuild().getId())
                                    ),
                                    Updates.set("warns", 0)
                            );

                            // Send DM to banned user
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("Banned")
                                    .setDescription(String.format("you have banned by @%s for `%s`, you can rejoin after `1 week`!",
                                            Objects.requireNonNull(event.getMember()).getUser().getName(),
                                            reason1))
                                    .setColor(Color.red)
                                    .setThumbnail("https://i.postimg.cc/KzffsS2w/giphy.gif")
                                    .setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());

                            // First open private channel
                            user1.getUser().openPrivateChannel().queue(privateChannel -> {
                                // Then send the embed
                                privateChannel.sendMessageEmbeds(embed.build()).queue(
                                        dmSuccess -> {
                                            // After DM is sent successfully, execute the ban
                                            System.out.println("Successfully sent private message to " + user1.getUser().getName());

                                            user1.ban(7, TimeUnit.DAYS).queue(
                                                    success -> {
                                                        // Update the reaction to indicate success
                                                        message.clearReactions().queue();
                                                        message.addReaction(Emoji.fromUnicode("✅")).queue();
                                                    },
                                                    error -> {
                                                        // Update the reaction to indicate failure
                                                        message.addReaction(Emoji.fromUnicode("❌")).queue();
                                                        System.err.println("Failed to ban user: " + error.getMessage());
                                                    }
                                            );
                                        },
                                        dmError -> {
                                            // If DM fails, still execute the ban but log the error
                                            System.err.println("Failed to send private message to " + user1.getUser().getName() +
                                                    ": " + dmError.getMessage());

                                            user1.ban(7, TimeUnit.DAYS).queue(
                                                    success -> {
                                                        message.clearReactions().queue();
                                                        message.addReaction(Emoji.fromUnicode("✅")).queue();
                                                    },
                                                    error -> {
                                                        message.addReaction(Emoji.fromUnicode("❌")).queue();
                                                        System.err.println("Failed to ban user: " + error.getMessage());
                                                    }
                                            );
                                        }
                                );
                            }, error -> {
                                // If we can't open DM channel, still execute the ban but log the error
                                System.err.println("Could not open private channel with " + user1.getUser().getName() +
                                        ": " + error.getMessage());

                                user1.ban(7, TimeUnit.DAYS).queue(
                                        success -> {
                                            message.clearReactions().queue();
                                            message.addReaction(Emoji.fromUnicode("✅")).queue();
                                        },
                                        error2 -> {
                                            message.addReaction(Emoji.fromUnicode("❌")).queue();
                                            System.err.println("Failed to ban user: " + error2.getMessage());
                                        }
                                );
                            });
                        });
                    });

                    return;
                } else {
                    // Update warn message with reaction
                    event.reply("Warning %s for `%s` - He now has `%d warns`".formatted(
                            user1.getAsMention(),
                            reason1,
                            newWarns
                    )).queue(interactionHook -> {
                        interactionHook.retrieveOriginal().queue(message -> {
                            message.addReaction(Emoji.fromUnicode("⚠️")).queue();
                        });
                    });
                }
            } else {
                // Create new member data
                event.reply("Couldn't find data for this member, Creating new member data...")
                        .setEphemeral(true)
                        .queue();

                MessageCreateAction message = event.getChannel().sendMessage(
                        String.format("Making a new data for member %s right now!", user1.getAsMention())
                );

                message.queue(sentMessage -> {
                    // Add "processing" reaction
                    sentMessage.addReaction(Emoji.fromUnicode("⏳")).queue();
                    try {
                        // Create new data
                        memberListener.makeNewData(event.getMember(), event.getGuild());

                        // Get the newly created document
                        Document data2 = memberListener.getDoc(user1.getId(), event.getGuild().getId());
                        if (data2 != null) {
                            // Update warns count
                            memberListener.getCollection().updateOne(
                                    Filters.and(
                                            Filters.eq("userId", user1.getId()),
                                            Filters.eq("guildId", event.getGuild().getId())
                                    ),
                                    Updates.set("warns", 1)
                            );

                            // Edit message to show success
                            MessageEditData editData = new MessageEditBuilder()
                                    .setContent(String.format("Created new data for %s with 1 warn!", user1.getAsMention()))
                                    .build();

                            sentMessage.editMessage(editData).queue(
                                    success -> {
                                        // Clear processing reaction and add success reaction
                                        sentMessage.clearReactions().queue();
                                        sentMessage.addReaction(Emoji.fromUnicode("✅")).queue();
                                    },
                                    error -> {
                                        sentMessage.clearReactions().queue();
                                        sentMessage.addReaction(Emoji.fromUnicode("❌")).queue();
                                    }
                            );
                        }
                    } catch (Exception e) {
                        // Handle database operation error
                        sentMessage.clearReactions().queue();
                        sentMessage.addReaction(Emoji.fromUnicode("❌")).queue();
                        event.reply("Error creating member data: " + e.getMessage())
                                .setEphemeral(true)
                                .queue();
                    }
                });
            }
            Document data2 = memberListener.getDoc(user1.getId(), Objects.requireNonNull(event.getGuild()).getId());

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Warning")
                    .setDescription(String.format("you have warned by @%s for `%s` and now you have `%s Warns`!", Objects.requireNonNull(event.getMember()).getUser().getName(), reason1, data2.get("warns")))
                    .setColor(Color.red)
                    .setThumbnail("https://i.postimg.cc/KzffsS2w/giphy.gif")
                    .setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());

            user1.getUser().openPrivateChannel()
                    .queue(privateChannel -> {
                                privateChannel.sendMessageEmbeds(embed.build())
                                        .queue(
                                                success -> System.out.println("Successfully sent private message to " + user1.getUser().getName()),
                                                error -> System.err.println("Failed to send private message to " + user1.getUser().getName() + ": " + error.getMessage())
                                        );
                            },
                            error -> System.err.println("Could not open private channel with " + user1.getUser().getName() + ": " + error.getMessage())
                    );
        } catch (Exception error) {
            // Log the error and send a message
            error.printStackTrace();
            event.reply("Error: " + error.getMessage())
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void GetID (SlashCommandInteractionEvent event) {
        assert event.getGuild() != null;
        Member user3 = Objects.requireNonNull(event.getOption("user")).getAsMember();
        assert user3 != null;
        event.reply("```"+user3.getUser().getId()+"```").queue();
    }

    public void HandleRole(SlashCommandInteractionEvent event) {
        assert event.getGuild() != null;

        Member member = Objects.requireNonNull(event.getOption("user")).getAsMember();
        Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        Member bot = Objects.requireNonNull(event.getGuild()).getSelfMember();

        if (member == null) {
            event.reply("❌ Invalid user or role!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check if member already has the role
        if (member.getRoles().contains(role)) {
            event.reply("❌ The member already has the role " + role.getName())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check if the bot's role is lower than the target member's highest role
        if (bot.getRoles().isEmpty() || member.getRoles().stream()
                .anyMatch(memberRole -> memberRole.getPosition() > bot.getRoles().get(0).getPosition())) {
            event.reply("❌ The member has a position higher than the bot it self!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check if the role to be added is higher than the bot's highest role
        if (role.getPosition() > bot.getRoles().get(0).getPosition()) {
            event.reply("❌ You can't give a role for someone has a position higher than the bot it self!")
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (role.getPosition() == bot.getRoles().get(0).getPosition()) {
            event.reply("❌ You can't assign a role that has the same position as the bot's highest role!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check if the target member's highest role is higher than the command user's highest role
        Member commandUser = event.getMember();
        assert commandUser != null;
        if (!commandUser.hasPermission(Permission.MANAGE_ROLES) && member.getRoles().stream()
                .anyMatch(memberRole -> memberRole.getPosition() >= commandUser.getRoles().get(0).getPosition())) {
            event.reply("❌ you don't have permission to use this command!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.getGuild().addRoleToMember(member, role).queue(
                success -> event.reply("✅ Added role " + role.getName() + " to " + member.getEffectiveName())
                        .setEphemeral(true)
                        .queue(),
                error -> event.reply("❌ Error: " + error.getMessage())
                        .setEphemeral(true)
                        .queue()
        );
    }

    public void Help(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📚 Bot Commands")
                .setDescription("Here's a list of all available commands!")
                .setColor(Color.lightGray)
                .setImage("https://i.postimg.cc/jjgZ8djJ/7.png")
                .setFooter("JDB",
                        event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        // Add all commands to the embed
        StringBuilder commandList = new StringBuilder();
        for (User.CommandInfo cmd : commands) {
            commandList.append("**/" + cmd.name + "** - " + cmd.description + "\n");
        }

        embed.addField("Available Commands", commandList.toString(), false);

        // Add some useful information
        embed.addField("Need Support?",
                "Join our server: [PRIME&STUDIO](https://discord.gg/NA7tdD54WY)",
                false);

        // Set timestamp
        embed.setTimestamp(Instant.now());

        // Reply with the embed
        event.replyEmbeds(embed.build()).queue();
    }

    public void UserInfo (SlashCommandInteractionEvent event, String mongoUri, String databaseName) {
        DateConverter dateConverter = new DateConverter();

        assert event.getGuild() != null;

        event.deferReply().queue();

        MongoDB mongo = new MongoDB(mongoUri, databaseName, "members");
        Member user2 = Objects.requireNonNull(event.getOption("user")).getAsMember();
        StringBuilder rolesList = new StringBuilder();

        assert user2 != null;

        Activity activity = user2.getActivities().stream().findFirst().orElse(null);

        for (Role role : user2.getRoles()) {
            rolesList.append(role.getName() + "\n");
        }

        profileBanner(user2, banner -> {
            net.dv8tion.jda.api.interactions.components.buttons.Button card = Button.link(String.format("https://discordlookup.com/user/%s",user2.getId()), "Card!");

            EmbedBuilder embed2 = new EmbedBuilder()
                    .setTitle(user2.getUser().getEffectiveName())
                    .setColor(Color.lightGray)
                    .addField("\uD83E\uDDD1 Username","```" + user2.getUser().getName()+ "```", true)
                    .addField("\uD83C\uDD94 User ID", "```" + user2.getId() + "```", true)
                    .addField("\uD83D\uDCC5 Join Date", dateConverter.formatDateOnly(user2.getTimeJoined().toString()), true)
                    .addField("\uD83D\uDD0B Current Status", user2.getOnlineStatus().toString(), true)
                    .addField("\uD83D\uDCDC Roles", rolesList.isEmpty() ? "No roles" : rolesList.toString(), true)
                    .addField("\uD83D\uDCC5 Account Creation Date", dateConverter.formatDateOnly(user2.getTimeCreated().toString()), true)
                    .addField("\uD83D\uDE80 Acitivity", activity == null ? "No activities" : activity.getType().toString(), true)
                    .addField("\uD83D\uDCCA Database", mongo.getDoc(user2.getId(), event.getGuild().getId()) == null ? "❌ Couldn't find the user at the database" : "✅ User found at the database", true)
                    .setThumbnail(user2.getUser().getAvatarUrl());
            if (banner != null) {
                embed2.setImage(banner);
            } else {
                embed2.setFooter("This user doesn't have a banner set!", "https://i.postimg.cc/4xM1t22z/source-4.gif");
            }

            event.getHook().editOriginalEmbeds(embed2.build())
                    .setActionRow(card)
                    .queue();
        });
    }

    public void UserBaner (SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getOption("user")).getAsMember();
        assert member != null;
        profileBanner(member, banner -> {
            if (banner != null) {
                // Use the banner URL here
                EmbedBuilder embed2 = new EmbedBuilder()
                        .setTitle("Banner")
                        .setColor(Color.lightGray)
                        .setImage(banner)
                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());

                event.replyEmbeds(embed2.build()).queue();
            } else {
                EmbedBuilder embed2 = new EmbedBuilder()
                        .setTitle("Banner")
                        .setDescription("This user doesn't have a banner!")
                        .setColor(Color.lightGray)
                        .setImage("https://i.postimg.cc/SNggyF3r/giphy-1.gif")
                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());

                event.replyEmbeds(embed2.build()).queue();
            }
        });
    }

    public void UserIcon(SlashCommandInteractionEvent event) {
        Member member1 = Objects.requireNonNull(event.getOption("user")).getAsMember();
        assert member1 != null;

        EmbedBuilder embed2 = new EmbedBuilder()
                .setTitle("Icon")
                .setColor(Color.lightGray)
                .setImage(member1.getEffectiveAvatarUrl())
                .setFooter("JDB",
                        event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        event.replyEmbeds(embed2.build()).complete();
    }

    public void Ban (SlashCommandInteractionEvent event) {
        Member selectedMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
        Integer Time = Objects.requireNonNull(event.getOption("user")).getAsInt();
        selectedMember.ban(Time, TimeUnit.DAYS).queue();
    }

    public void profileBanner(Member member, Consumer<String> callback) {
        member.getUser().retrieveProfile().queue(
                profile -> callback.accept(profile.getBannerUrl()),
                error -> callback.accept(null) // Handle error case
        );
    }
}

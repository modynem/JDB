package Commands;

import API.Authenticated;
import API.UserInformation;
import Database.MongoDB;
import Services.DateConverter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SlashCommands extends ListenerAdapter {
    private final OkHttpClient client = new OkHttpClient();
    private static final String API_KEY = "f56644dfe146f75c12c7461f"; // Get from exchangerate-api.com or similar service
    private final String mongoUri = "mongodb+srv://JDB:%40ModyNegm00@cluster0.x7cbu.mongodb.net/";
    private final String databaseName = "JDB";
    DateConverter dateConverter = new DateConverter();

    // Store commands and their descriptions
    private static class CommandInfo {
        String name;
        String description;

        public CommandInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    private final List<CommandInfo> commands;

    public SlashCommands() {
        commands = new ArrayList<>();
        // Add all your commands and their descriptions here
        commands.add(new CommandInfo("help", "shows the help message"));
        commands.add(new CommandInfo("devex_currency", "convert currency to Robux"));
        commands.add(new CommandInfo("devex_robux", "convert Robux to currency"));
        commands.add(new CommandInfo("kick", "kick a member"));
        commands.add(new CommandInfo("warn", "warn a member"));
        commands.add(new CommandInfo("get_id", "get server member user ID"));
        commands.add(new CommandInfo("game_info", "get game information by universeId"));
        commands.add(new CommandInfo("player_info", "get roblox player information by userName e.g. [Followers]"));
        commands.add(new CommandInfo("role", "assign a role to a user"));
        commands.add(new CommandInfo("lock", "lock the current channel"));
        commands.add(new CommandInfo("send_dm", "send a private for a specific role"));
        commands.add(new CommandInfo("user_info", "get server member information"));
        commands.add(new CommandInfo("gamepasses", "get player gamepasses list by userName"));
        commands.add(new CommandInfo("banner", "get an user banner"));
        commands.add(new CommandInfo("icon", "get an user icon"));
        commands.add(new CommandInfo("server", "see all current server status"));
        commands.add(new CommandInfo("ping", "see current bot ping"));
        commands.add(new CommandInfo("unlock", "UnLock the current channel"));
        commands.add(new CommandInfo("ban", "ban a specific user"));
        //commands.add(new CommandInfo("embed", "create custom embed"));
        // Add more commands as needed
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "devex_currency":
                DevexCurrency(event);
                break;
            case "devex_robux":
                DevexRobux(event);
                break;
            case "kick":
                assert event.getGuild() != null;

                String reason = event.getOption("reason") == null ? "No reason provided" : Objects.requireNonNull(event.getOption("reason")).getAsString();
                event.reply("Kicking %s for `%s`".formatted(Objects.requireNonNull(Objects.requireNonNull(event.getOption("user")).getAsMember()).getAsMention(), reason)).queue();
                Objects.requireNonNull(Objects.requireNonNull(event.getOption("user")).getAsMember()).kick().queue();
                break;
            case "warn":
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
                break;
            case "get_id":
                assert event.getGuild() != null;

                Member user = Objects.requireNonNull(event.getOption("user")).getAsMember();
                assert user != null;
                event.reply("```"+user.getUser().getId()+"```").queue();
                break;
            case "game_info":
                try {
                    Authenticated api = new Authenticated("_|WARNING:-DO-NOT-SHARE-THIS.--Sharing-this-will-allow-someone-to-log-in-as-you-and-to-steal-your-ROBUX-and-items.|_299A39DD1F93211A2711E1D3344DE9AB7A12D847A471DDE249E7CA4E3561DF19379BC482301E4AF9ED0BC02524CB423DE6E01F4F2C53FF6F3C5EBF88C321E16B245941238367A13020C682AFDEF5BDAA66D86F8252EFB94703642AEA950A908EC435EE55EEA83750CD91FEE6FE2427151BF7B0B82D979305B0960CA6A4E110FA91F87E24450967A60CE2EC900593B54CFCABF9DBB192DDA41DC991363E52400A28A4861B8AB2D113D3591743D9727A2BFC0512A7AAB1FAEE2E4A328A48D085FA3ED5FEC6D35A62CACF46D00BF9735FBFDB6C164A3ECAFCC692C684462A605D3890831BA2896727BE83A050975EB880184EDD5DC0B2E7C001EACE16AFBBA09DD386DBCD5DD08C5E7AF09164A5FBB11C930D729FB5CC8DBBA076074074662496160534CE089ACB4AEE6D45997FBB79843AC0413DA5B0609A8BEE813346876246A12F0A25A96550899A56D3ED2983DA1F76EA0C834C69A5BDA98AA4BC50B11B8B0ED6C868BC9074D99A643871CAE4538B3B80A378995BBD9D5C74DD831A5115B228C3A5347E4E8BEF346A9D39121B60FFBC76AD4A8D36D61D812B9A4817319D760B12128AA82DE18E6016DE74CAB8E5DD50544B808B85D529260EC568E328FFC4EEADE5576BA2ECCDCBBF98831FBBD8A3EDB19EB17BE8272A00F09FCB71098DD8E774E1EADEEA7221EC112AAC09C58D394203AE14FC67BD2FC1EF34941A0277B628CC74B022C9E0AF050BC82BC3815F1FC135544C850034A8586AF3ABEACC1DF6BCB43ED28AA2A5A077C704D454BA0F7D4AD99C53145751DA9400F3EA21BFA69BEFA1E9662DEAFE8FFFAB0C9B9C7688DF5F803757429BD13BC4F980F51039BA58F30EE2EA9E99B7E2FFF2B7C4A1DF5D49D08ED742249961C6ADAED09778C3BDD90A9995E91589EDC78A52100C692C46D868263B659DD375EC4A9139299F43548881D5D93107C749C1A007458DCC633C6B52D2B4E979F4207E65C3622760A31225A0834A3CA9C8711C5E607A6E8A0A28710EC141B6DE33DB044E04301E6E4B3705A7AAA37F6738400055E784690760292C149BD533A0D88E72391C1F65298704389999A460C78BD3E5B1A4567073DB6849E6D15C53A1");
                    long Placeid = (Long) Objects.requireNonNull(event.getOption("place_id", 0, OptionMapping::getAsLong));
                    Long universeId = api.getUniverseID(Placeid);
                    JSONArray data = api.getGameInfo(universeId);
                    System.out.println(data);
                    JSONObject firstGameNode = (JSONObject) data.get(0);
                    Button button = Button.link("https://www.roblox.com/games/"+Placeid, "Play!");
                    Button button2 = Button.link("https://romonitorstats.com/experience/"+Placeid, "Stats!");

                    event.replyEmbeds(createGameInfoEmbed(firstGameNode, universeId))
                            .setActionRow(button, button2)
                            .complete();
                } catch (Exception e) {
                    event.replyEmbeds(
                            errorEmbed("Error", e.getMessage() +"\n\nhere is how you can get the game placeId: ", "https://i.postimg.cc/Wpw38Rxz/Screenshot-2024-11-10-085903.png", "https://i.postimg.cc/g0tJpYc2/source.gif")
                    ).complete();
                }
                break;
            case "player_info":
                event.deferReply().queue();

                String username = (String) Objects.requireNonNull(event.getOption("user_name", "", OptionMapping::getAsString));
                UserInformation userInformation = new UserInformation();
                try {

                    JSONObject userInfo = userInformation.userInfo(username);
                    Button button = Button.link(String.format("https://www.roblox.com/users/%s/profile",userInfo.get("id")), "Profile!");

                    //ArrayList userStats = userInformation.userStats(userId);
                    String CreatedDateformatted = DateConverter.convertISOToFormattedDate(userInfo.get("created").toString(), "yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(CreatedDateformatted,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    event.getHook().sendMessageEmbeds(
                            new EmbedBuilder().setTitle(userInfo.get("displayName").toString())
                                    .setDescription(userInfo.get("description").toString())
                                    .setColor(Color.lightGray)
                                    .addField("\uD83C\uDD94 User ID", "```"+userInfo.get("id").toString()+"```", true)
                                    .addField("\uD83D\uDCC5 Created On", dateTime.format(DateTimeFormatter.ofPattern("E, MMM dd yyyy HH:mm")), true)
                                    .addField("\uD83C\uDFAE IsBanned", userInfo.get("isBanned").toString(), true)
                                    .addField("✅ HasVerifiedBadge", userInfo.get("hasVerifiedBadge").toString(), true)

                                    .addField("\uD83D\uDD14 Followers", userInfo.get("followers").toString(), true)
                                    .addField("\uD83D\uDC65 Friends", userInfo.get("friends").toString(), true)
                                    .addField("\uD83D\uDC65 Following", userInfo.get("following").toString(), true)


                                    .setThumbnail(userInfo.get("thumpnail").toString())
                                    .build()
                            ).setActionRow(button)
                            .queue();
                } catch (Exception e) {
                    event.getHook().sendMessageEmbeds(
                            errorEmbed("Error", e.getMessage(), "", "https://i.postimg.cc/g0tJpYc2/source.gif")
                    ).queue();
                }
                break;
            case "role":
                assert event.getGuild() != null;

                HandleRole(event);
                break;
            case "lock":
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
                                        errorEmbed("❌ Failed: "+ error.getMessage(), "", "", ""))
                                            .setEphemeral(true)
                                            .queue()
                        );
                break;
            case "send_dm":
                assert event.getGuild() != null;
                DmHandler dmHandler = new DmHandler();
                dmHandler.HandleDm(event);
                break;
            case "help":
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("📚 Bot Commands")
                        .setDescription("Here's a list of all available commands!")
                        .setColor(Color.lightGray)
                        .setImage("https://i.postimg.cc/jjgZ8djJ/7.png")
                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());

                // Add all commands to the embed
                StringBuilder commandList = new StringBuilder();
                for (CommandInfo cmd : commands) {
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
                break;
            case "user_info":
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
                    Button card = Button.link(String.format("https://discordlookup.com/user/%s",user2.getId()), "Card!");

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
                break;

            case "gamepasses":
                event.deferReply().queue();
                UserInformation userInformation1 = new UserInformation();

                String username1 = (String) Objects.requireNonNull(event.getOption("user_name", "", OptionMapping::getAsString));
                try {
                    Long UserID = userInformation1.userIdByUsername(username1);
                    ArrayList<Long> experiences = userInformation1.getExperiences(UserID);
                    List<List<Object>> gamepassesId = userInformation1.getGamepasses(experiences);
                    EmbedBuilder passses = new EmbedBuilder()
                            .setTitle("\uD83D\uDCB8 Gamepasses")
                            .setColor(Color.lightGray)
                            .setFooter("JDB",
                                    event.getJDA().getSelfUser().getEffectiveAvatarUrl());
                    for (List<Object> pass : gamepassesId) {
                        passses.addField(pass.get(2).toString(), String.format("Price: %s | [LINK](%s)", pass.get(1).toString(), "https://www.roblox.com/game-pass/" + pass.get(0)), true);
                    }
                    event.getHook().sendMessageEmbeds(passses.build()).queue();
                } catch (Exception e) {
                    event.getHook().sendMessageEmbeds(
                            errorEmbed("Error", e.getMessage().length() > 2000 ? "something went wrong" : e.getMessage(), "", "https://i.postimg.cc/g0tJpYc2/source.gif")
                    ).queue();
                }

                break;

            case "banner":
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
                break;
            case "icon":
                Member member1 = Objects.requireNonNull(event.getOption("user")).getAsMember();
                assert member1 != null;

                EmbedBuilder embed2 = new EmbedBuilder()
                        .setTitle("Icon")
                        .setColor(Color.lightGray)
                        .setImage(member1.getEffectiveAvatarUrl())
                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());

                event.replyEmbeds(embed2.build()).complete();
                break;

            case "server":
                Guild guild = event.getGuild();
                assert guild != null;
                Integer Online = 0;

                for (Member mem : guild.getMembers()) {
                    if (mem.getOnlineStatus() == OnlineStatus.ONLINE) {
                        Online++;
                    }
                }

                EmbedBuilder embed3 = new EmbedBuilder()
                        .setTitle(guild.getName())
                        .setDescription(guild.getDescription())
                        .setColor(Color.lightGray)
                        .setThumbnail(guild.getIconUrl())
                        .addField("\uD83C\uDD94 Server ID", "```"+guild.getId()+"```", true)
                        .addField("\uD83D\uDCC5 Created On", dateConverter.formatDateOnly(guild.getTimeCreated().toString()), true)
                        .addField("\uD83D\uDC51 Owned by", Objects.requireNonNull(guild.getOwner()).getAsMention(), true)

                        .addField(String.format("\uD83D\uDC65 Members (%s)", guild.getMembers().size()), String.format("**%s** Online \n**%s** Boosts \uD83C\uDF1F", Online, guild.getBoostCount()), true)
                        .addField(String.format("\uD83D\uDCAC Channels (%s)", guild.getChannels().size()), String.format("**%s** Text | **%s** Voice", guild.getTextChannels().size(), guild.getVoiceChannels().size()), true)
                        .addField("\uD83C\uDF0E Others", "Verification Level: " + guild.getVerificationLevel(), true)

                        .addField(String.format("\uD83D\uDD10 Roles (%s)", guild.getRoles().size()), "", true)

                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());
                if (event.getGuild().getBannerUrl() != null) {
                    embed3.setImage(event.getGuild().getBannerUrl());
                }
                event.replyEmbeds(embed3.build()).queue();
                break;
            case "ping":
                assert event.getGuild() != null;

                long ping = event.getJDA().getGatewayPing();
                EmbedBuilder embed4 = new EmbedBuilder()
                        .setTitle("🏓 Pong!")
                        .setDescription("Gateway Ping: " + ping + "ms")
                        .setThumbnail("https://i.postimg.cc/SKc8x3DR/source-5.gif")
                        .setColor(Color.lightGray)
                        .setFooter("JDB",
                                event.getJDA().getSelfUser().getEffectiveAvatarUrl());

                event.replyEmbeds(embed4.build()).queue();
                break;
            case "unlock":
                assert event.getGuild() != null;

                TextChannel channel1 = event.getChannel().asTextChannel();
                Role everyone1 = Objects.requireNonNull(event.getGuild()).getPublicRole();

                channel1.getManager()
                        .putPermissionOverride(everyone1, EnumSet.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL), null)
                        .queue(
                                success -> event.replyEmbeds(
                                                new EmbedBuilder().setTitle("✅ Successfully unlocked!")
                                                        .setColor(Color.green)
                                                        .build()
                                        )
                                        .queue(),
                                error -> event.replyEmbeds(
                                                errorEmbed("❌ Failed: "+ error.getMessage(), "", "", ""))
                                        .setEphemeral(true)
                                        .queue()
                        );
                break;
            case "ban":
                Member selectedMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
                Integer Time = Objects.requireNonNull(event.getOption("user")).getAsInt();
                selectedMember.ban(Time, TimeUnit.DAYS).queue();
                break;
        }
    }

    private void DevexCurrency(SlashCommandInteractionEvent event) {
        try {
            double amount = event.getOption("amount", 0.0, OptionMapping::getAsDouble);
            String targetCurrency = event.getOption("currency", "USD", OptionMapping::getAsString);

            // Fetch latest exchange rate
            String rate = getExchangeRate(targetCurrency);
            double exchangeRate = Double.parseDouble(rate);

            event.reply(String.format(Locale.US, "`%.2f %s` converts to `%.2f Robux`",
                            amount,
                            targetCurrency,
                            Math.round(amount/exchangeRate / 3.5 * 1000 * 100.0) / 100.0))
                    .queue();

        } catch (Exception e) {
            event.reply("❌ Error processing currency conversion: " + e.getMessage())
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void DevexRobux(SlashCommandInteractionEvent event) {
        try {
            double amount = event.getOption("amount", 0.0, OptionMapping::getAsDouble);
            String targetCurrency = event.getOption("currency", "USD", OptionMapping::getAsString);

            // Fetch latest exchange rate
            String rate = getExchangeRate(targetCurrency);
            double exchangeRate = Double.parseDouble(rate);
            double postTaxAmount = (amount / 1000 * 3.5 * exchangeRate) * 70 / 100;


            event.reply(String.format(Locale.US, "`%.2f` converts to `%.2f %s` (`%s %s` post Roblox tax)",
                            amount,
                            (amount / 1000) * 3.5 * exchangeRate,
                            targetCurrency,
                            postTaxAmount,
                            targetCurrency))
                    .queue();
        } catch (Exception e) {
            event.reply("❌ Error processing currency conversion: " + e.getMessage())
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void HandleRole(SlashCommandInteractionEvent event) {
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

        private void processInBatches(List<Member> members, SlashCommandInteractionEvent event,
                                      String message, Message.Attachment attachment, Role role) {
            List<Member> validMembers = members.stream()
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

        private void processBatch(List<Member> allMembers, int batchIndex, int totalBatches,
                                  SlashCommandInteractionEvent event, String message, Message.Attachment attachment, Role role) {
            int startIndex = batchIndex * BATCH_SIZE;
            int endIndex = Math.min(startIndex + BATCH_SIZE, allMembers.size());
            List<Member> batchMembers = allMembers.subList(startIndex, endIndex);

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
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastUpdateTime >= UPDATE_INTERVAL || progress.processed.get() >= progress.total) {
                lastUpdateTime = currentTime;

                long elapsedTime = currentTime - progress.startTime;
                long estimatedTimeRemaining = progress.processed.get() > 0 ?
                        (elapsedTime / progress.processed.get()) * (progress.total - progress.processed.get()) : 0;

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
                        .addField("Estimated Time Remaining", formatTime(estimatedTimeRemaining), true)
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

    private String getExchangeRate(String targetCurrency) throws Exception {
        // Build API request URL (using exchangerate-api.com as an example)
        String url = String.format("https://v6.exchangerate-api.com/v6/%s/latest/USD", API_KEY);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("❌ Failed to fetch exchange rate");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONObject rates = json.getJSONObject("conversion_rates");

            return rates.get(targetCurrency).toString();
        }
    }
    private MessageEmbed createGameInfoEmbed(JSONObject firstGameNode, long universeId) {
        JSONObject creator = (JSONObject) firstGameNode.get("creator");

        String url = String.format("https://thumbnails.roblox.com/v1/games/multiget/thumbnails?universeIds=%s&format=Png&size=480x270", universeId);
        String GameIconUrl = String.format("https://thumbnails.roblox.com/v1/games/icons?universeIds=%s&returnPolicy=PlaceHolder&size=420x420&format=Png&isCircular=false", universeId);
        String Icon;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Request request2 = new Request.Builder()
                .url(GameIconUrl)
                .build();

        try (Response response = client.newCall(request2).execute()) {
            if (!response.isSuccessful()) throw new Exception("❌ Failed to fetch game info");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            JSONObject Table = data.getJSONObject(0);

            Icon = Table.get("imageUrl").toString();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("❌ Failed to fetch game info");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            JSONObject thumps = (JSONObject) data.get(0);
            JSONArray thumpnails = (JSONArray) thumps.get("thumbnails");
            JSONObject thumpnail = (JSONObject) thumpnails.get(0);
            String imageUrl = thumpnail.get("imageUrl").toString();
            String UpdateDateformatted = DateConverter.convertISOToFormattedDate(firstGameNode.get("updated").toString(), "yyyy-MM-dd HH:mm:ss");


            return new EmbedBuilder()
                    .setTitle(firstGameNode.get("name").toString())
                    //.setDescription(firstGameNode.get("description").toString())
                    .setColor(Color.lightGray)
                    .addField("CCU", firstGameNode.get("playing").toString(), true)
                    .addField("Visits", firstGameNode.get("visits").toString(), true)
                    .addField("Favorites", firstGameNode.get("favoritedCount").toString(), true)

                    .addField("Genre", firstGameNode.get("genre").toString(), true)
                    .addField("MaxPlayers", firstGameNode.get("maxPlayers").toString(), true)
                    .addField("CopyingAllowed", firstGameNode.get("copyingAllowed").toString(), true)

                    .addField("UniverseAvatarType", firstGameNode.get("universeAvatarType").toString(), true)
                    .addField("Updated", UpdateDateformatted, true)
                    .addField("StudioAccessToApisAllowed", firstGameNode.get("studioAccessToApisAllowed").toString(), true)
                    .addField("UniverseID", "```" + universeId + "```", true)

                    .addField("Creator Name", creator.get("name").toString(), true)
                    .addField("Creator Type", creator.get("type").toString(), true)
                    .addField("HasVerifiedBadge", creator.get("hasVerifiedBadge").toString(), true)

                    .setImage(imageUrl)
                    .setThumbnail(Icon == null ? "" : Icon)

                    .build();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void profileBanner(Member member, Consumer<String> callback) {
        member.getUser().retrieveProfile().queue(
                profile -> callback.accept(profile.getBannerUrl()),
                error -> callback.accept(null) // Handle error case
        );
    }

    private MessageEmbed errorEmbed(String Title, String Desc, String Image, String Thumpnail) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(Title)
                .setDescription(Desc.isEmpty() ? null : "❌ Error: " + Desc)
                .setColor(Color.red)
                .setImage(Image.isEmpty() ? null : Image)
                .setThumbnail(Thumpnail.isEmpty() ? null : Thumpnail)
                .build();
        return embed;
    }
}

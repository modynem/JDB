import Commands.SlashCommands;
import Database.MongoDB;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bson.Document;


public class JDB extends ListenerAdapter {
    private final MongoDB mongoDB;
    private static final String Version = "23A712T";

    public JDB(MongoDB mongoDB) {
        this.mongoDB = mongoDB;
    }

    public static void main(String[] args) throws InterruptedException {
        String token = "MTMwNDMwMzc0NzQwMDIwODQzNA.GoA2aV.xcKW0mrLrxXjWwzJwO6hcEzXwUAcssbdENnfPA";
        String mongoUri = "mongodb+srv://JDB:%40ModyNegm00@cluster0.x7cbu.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        String databaseName = "JDB";
        String collectionName = "members";
        final MongoDB mongoDB = new MongoDB(mongoUri, databaseName, collectionName);

        // Create bot instance with MongoDB
        JDB jdb = new JDB(mongoDB);

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_PRESENCES,  // This is crucial for status
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL) // Enable member chunking for all guilds
                .setActivity(Activity.of(Activity.ActivityType.PLAYING,
                        "Better Basketball Player?",
                        "https://www.roblox.com/games/18932416849/UPD2-Better-Basketball-Player"))
                .addEventListeners(new SlashCommands())
                .addEventListeners(new NewServer(mongoDB))
                r.addEventListeners(new StartupCheck(mongoDB))
                //.addEventListeners(new SetupSystem())
                .addEventListeners(jdb) // Add the main bot listener
                .build().awaitReady();

        System.out.println(Version);
        // Register slash commands globally
        jda.updateCommands().addCommands(
                Commands.slash("devex_currency", "convert currency to Robux")
                        .addOption(OptionType.NUMBER, "amount", "enter the amount of USD to convert", true)
                        .addOptions(new OptionData(OptionType.STRING, "currency", "select your local currency", false)
                                .addChoice("US Dollar (USD)", "USD")
                                .addChoice("Euro (EUR)", "EUR")
                                .addChoice("British Pound (GBP)", "GBP")
                                .addChoice("Japanese Yen (JPY)", "JPY")
                                .addChoice("Canadian Dollar (CAD)", "CAD")
                                .addChoice("Australian Dollar (AUD)", "AUD")
                                .addChoice("Swiss Franc (CHF)", "CHF")
                                .addChoice("Chinese Yuan (CNY)", "CNY")
                                .addChoice("Indian Rupee (INR)", "INR")
                                .addChoice("Brazilian Real (BRL)", "BRL")
                                .addChoice("Egyptian Pound (EGP)", "EGP")
                        ),
                Commands.slash("devex_robux", "convert Robux to currency")
                        .addOption(OptionType.INTEGER, "amount", "enter the amount of Robux to convert", true)
                        .addOptions(new OptionData(OptionType.STRING, "currency", "select your local currency", false)
                                .addChoice("US Dollar (USD)", "USD")
                                .addChoice("Euro (EUR)", "EUR")
                                .addChoice("British Pound (GBP)", "GBP")
                                .addChoice("Japanese Yen (JPY)", "JPY")
                                .addChoice("Canadian Dollar (CAD)", "CAD")
                                .addChoice("Australian Dollar (AUD)", "AUD")
                                .addChoice("Swiss Franc (CHF)", "CHF")
                                .addChoice("Chinese Yuan (CNY)", "CNY")
                                .addChoice("Indian Rupee (INR)", "INR")
                                .addChoice("Brazilian Real (BRL)", "BRL")
                                .addChoice("Egyptian Pound (EGP)", "EGP")
                        ),
                Commands.slash("kick", "kick a member")
                        .addOption(OptionType.USER, "user", "the user to kick", true)
                        .addOption(OptionType.STRING, "reason", "reason for kicking", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)),

                Commands.slash("warn", "warn a member")
                        .addOption(OptionType.USER, "user", "the user to kick", true)
                        .addOption(OptionType.STRING, "reason", "reason for kicking", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)),

                Commands.slash("get_id", "get server member user ID")
                        .addOption(OptionType.USER, "user", "the server member to get him/her id", true),

                Commands.slash("game_info", "get game information by universeId")
                        .addOption(OptionType.INTEGER, "place_id", "the placeId of game you want to get info of", true),

                Commands.slash("player_info", "get roblox player information by userName e.g. [Followers]")
                        .addOption(OptionType.STRING, "user_name", "the userName of player you want to get info of", true),

//                Commands.slash("ask_ai", "ask AI about any thing")
//                        .addOption(OptionType.STRING, "prompt", "what do you want to ask the AI about", true),

                Commands.slash("role", "assign a role to a user")
                        .addOption(OptionType.USER, "user", "user to assign role to", true)
                        .addOption(OptionType.ROLE, "role", "role to assign", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)),

                Commands.slash("lock", "lock the current channel")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),

                Commands.slash("send_dm", "send a private for a specific role")
                        .addOption(OptionType.ROLE, "role", "role that you want to send in dm to", true)
                        .addOption(OptionType.STRING, "message", "message you want to send", true)
                        .addOption(OptionType.ATTACHMENT, "attachment", "add attachment e.g. [Image]", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

//                Commands.slash("setup_marketplace", "setup your server marketplace")
//                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))

                //NEW
                Commands.slash("help", "shows the help message"),

                Commands.slash("user_info", "get server member information")
                        .addOption(OptionType.USER, "user", "the user you want to get his/her information", true),

                Commands.slash("gamepasses", "get player gamepasses list by userName")
                        .addOption(OptionType.STRING, "user_name", "the userName of player you want to get info of", true),
                        //.addOption(OptionType.INTEGER, "robux", "set a specific robux amount that you want to show all gamepasses has the same price", false),

                Commands.slash("banner", "get an user banner")
                        .addOption(OptionType.USER, "user", "the user that you want to get him/her banner", true),

                Commands.slash("icon", "get an user icon")
                        .addOption(OptionType.USER, "user", "the user that you want to get him/her icon", true),

                Commands.slash("server", "see all current server status")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)),

                Commands.slash("ping", "see current bot ping")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)),

                Commands.slash("unlock", "UnLock the current channel")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),

                Commands.slash("ban", "ban a specific user")
                        .addOption(OptionType.USER, "user", "the user that you want to ban", true)
                        .addOption(OptionType.INTEGER, "time", "the amount of time for the ban [IN DAYS]", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))

//                Commands.slash("embed", "create custom embed")
//                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
//                        .addOption(OptionType.STRING, "title", "the embed title", true)
//                        .addOption(OptionType.STRING, "description", "the embed description", false)
//                        .addOption(OptionType.STRING, "thumpnail", "the embed thumpnail", false)
//                        .addOption(OptionType.STRING, "image", "the embed image", false)
//                        .addOption(OptionType.STRING, "footer_text", "the embed footer-text", false)
//                        .addOption(OptionType.STRING, "footer_image", "the embed footer-image", false)

        ).queue(
                success2 -> System.out.println("Commands registered successfully!"),
                error -> System.out.println("Error registering commands: " + error.getMessage())
        );
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        try {
            // Get member and guild data
            var member = event.getMember();
            var guild = event.getGuild();

            // Check if member already has data in database
            Document existingData = mongoDB.getDoc(member.getId(), guild.getId());

            if (existingData == null) {
                // Create new data for the member if none exists
                System.out.println("Creating new database entry for member " +
                        member.getUser().getName() + " in guild " + guild.getName());
                mongoDB.makeNewData(member, guild);
            } else {
                System.out.println("Member " + member.getUser().getName() +
                        " already has data in guild " + guild.getName());
            }

        } catch (Exception e) {
            System.err.println("Error handling member join event for guild " +
                    event.getGuild().getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Member member = event.getMember();
        Message message = event.getMessage();
        String messageContent = message.getContentRaw();

        assert member != null;
        if (messageContent.contains("السلام عليكم")) {
            message
                    .reply("و عليكم السلام!")
                    .queue();
            message
                    .addReaction(Emoji.fromUnicode("\uD83D\uDC96"))
                    .queue();
        } else if (messageContent.contains("و عليكم السلام")) {
            message
                    .addReaction(Emoji.fromUnicode("\uD83D\uDC96"))
                    .queue();
        } else if (messageContent.contains("help") || messageContent.contains("مساعد")) {
            message
                    .reply("you can get help by using `/help` for the commands list!")
                    .queue();
            message
                    .addReaction(Emoji.fromUnicode("ℹ️"))
                    .queue();
        } else if (messageContent.contains("!version") || messageContent.contains("!اصدار")) {
            message
                    .reply(String.format("current version is `%s`!", Version))
                    .queue();
            message
                    .addReaction(Emoji.fromUnicode("\uD83E\uDD16"))
                    .queue();
        }
    }

}
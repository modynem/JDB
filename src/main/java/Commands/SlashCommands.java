package Commands;

import API.GameInformation;
import API.UserInformation;
import Componants.ErrorEmbed;
import Services.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommands extends ListenerAdapter {
    private final String mongoUri = "";
    private final String databaseName = "JDB";
    DateConverter dateConverter = new DateConverter();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "devex_currency":
                Devex devex = new Devex();
                devex.DevexCurrency(event);
                break;
            case "devex_robux":
                Devex devex1 = new Devex();
                devex1.DevexRobux(event);
                break;
            case "kick":
                User user = new User();
                user.Kick(event);
                break;
            case "warn":
                User user1 = new User();
                user1.Warn(event, mongoUri, databaseName);
                break;
            case "get_id":
                User user2 = new User();
                user2.GetID(event);
                break;
            case "game_info":
                GameInformation gameInformation = new GameInformation();
                gameInformation.GetGameInfo(event);
                break;
            case "player_info":
                UserInformation userInformation = new UserInformation();
                userInformation.PlayerInfo(event);
                break;
            case "role":
                User user3 = new User();
                user3.HandleRole(event);
                break;
            case "lock":
                Channel channel = new Channel();
                channel.Lock(event);
                break;
//            case "send_dm":
//                 DmHandler dmHandler = new DmHandler();
//                dmHandler.HandleDm(event);
//                break;
            case "help":
                User user4 = new User();
                user4.Help(event);
                break;
            case "user_info":
                User user5 = new User();
                user5.UserInfo(event, mongoUri, databaseName);
                break;
            case "gamepasses":
                UserInformation userInformation1 = new UserInformation();
                userInformation1.PlayerGamepasses(event);
                break;
            case "banner":
                User user6 = new User();
                user6.UserBaner(event);
                break;
            case "icon":
                User user7 = new User();
                user7.UserIcon(event);
                break;
            case "server":
                Guild guild = new Guild();
                guild.GuildInfo(event);
                break;
            case "ping":
                Self self = new Self();
                self.Ping(event);
                break;
            case "unlock":
                Channel channel1 = new Channel();
                channel1.Unlock(event);
                break;
            case "ban":
                User user8 = new User();
                user8.Ban(event);
                break;

            default:
                ErrorEmbed errorEmbed = new ErrorEmbed();

                event.replyEmbeds(errorEmbed.Error("Error", "Undefind command", "", "https://i.postimg.cc/zG1ySbDT/giphy.gif")).queue();
        }
    }
}

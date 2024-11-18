package Services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class Self extends ListenerAdapter {
    public void Ping (SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        EmbedBuilder embed4 = new EmbedBuilder()
                .setTitle("🏓 Pong!")
                .setDescription("Gateway Ping: " + ping + "ms")
                .setThumbnail("https://i.postimg.cc/SKc8x3DR/source-5.gif")
                .setColor(Color.lightGray)
                .setFooter("JDB",
                        event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        event.replyEmbeds(embed4.build()).queue();
    }

}

package Services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Objects;

public class Guild {
    public void GuildInfo(SlashCommandInteractionEvent event) {
        DateConverter dateConverter = new DateConverter();

        net.dv8tion.jda.api.entities.Guild guild = event.getGuild();
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
    }
}

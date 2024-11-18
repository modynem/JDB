package Componants;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

public class ErrorEmbed {
    public MessageEmbed Error(String Title, String Desc, String Image, String Thumpnail) {
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

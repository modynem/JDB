package Componants;

import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;


public class Embed {
    public EmbedBuilder creatEmbed(String Title, String Desc, JSONArray Fields, String Image, String Thump, Color Color){
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(Title);
        if (!Desc.isEmpty()) {
            embed.setDescription(Desc);
        }
        if (!Fields.isEmpty() || !Fields.isNull(0)) {
            for (int i = 0; i < Fields.length(); i++) {
                JSONObject field = (JSONObject) Fields.get(i);
                embed.addField(field.get("name").toString(), field.get("value").toString(), (Boolean) field.get("inLine"));
            }
        }
        if (!Image.isEmpty()) {
            embed.setImage(Image);
        }
        if (!Thump.isEmpty()) {
            embed.setThumbnail(Thump);
        }
        embed.setColor(Color == null ? java.awt.Color.lightGray : Color);
        embed.build();

        return embed;
    }
}

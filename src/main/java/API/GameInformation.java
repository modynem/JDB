package API;

import Componants.ErrorEmbed;
import Services.DateConverter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.Objects;

public class GameInformation {
    private final OkHttpClient client = new OkHttpClient();

    public void GetGameInfo(SlashCommandInteractionEvent event) {
        ErrorEmbed errorEmbed = new ErrorEmbed();
        try {
            Authenticated api = new Authenticated("");
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
                    errorEmbed.Error("Error", e.getMessage() + "\n\nhere is how you can get the game placeId: ", "https://i.postimg.cc/Wpw38Rxz/Screenshot-2024-11-10-085903.png", "https://i.postimg.cc/g0tJpYc2/source.gif")
            ).complete();
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
}

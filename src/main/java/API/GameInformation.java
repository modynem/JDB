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

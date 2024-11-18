package API;

import Componants.ErrorEmbed;
import Services.DateConverter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UserInformation extends ListenerAdapter {
    private final OkHttpClient client = new OkHttpClient();


    public Long userIdByUsername(String username) throws Exception {
        String url = String.format("https://users.roblox.com/v1/users/search?keyword=%s", username);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to get userId!");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            JSONObject table = data.getJSONObject(0);

            return table.getLong("id");
        }catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
    public JSONObject userInfo(String username) throws Exception {
        Long userId = userIdByUsername(username);
        System.out.println(userId);
        String url = "https://users.roblox.com/v1/users/" + userId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch user info");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            ArrayList<Object> userstats = userStats(userId);
            String thumpUrl = getUserThumpnail(userId);

            json.put("friends", userstats.get(0));
            json.put("followers", userstats.get(1));
            json.put("following", userstats.get(2));

            json.put("thumpnail", thumpUrl);

            return json;
        }catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public ArrayList<Object> userStats(Long userId) throws  Exception {
        String FriendsCountAPI = String.format("https://friends.roblox.com/v1/users/%s/friends/count", userId);
        String FollowersCountAPI = String.format("https://friends.roblox.com/v1/users/%s/followers/count", userId);
        String FollowingCountAPI = String.format("https://friends.roblox.com/v1/users/%s/followings/count", userId);

        ArrayList<Object> table = new ArrayList<>();

        Request friendsCount = new Request.Builder()
                .url(FriendsCountAPI)
                .build();
        Request followersCount = new Request.Builder()
                .url(FollowersCountAPI)
                .build();
        Request followingCount = new Request.Builder()
                .url(FollowingCountAPI)
                .build();

        try (Response response = client.newCall(friendsCount).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch player friends count");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            table.add(0,json.get("count"));
        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }

        try (Response response = client.newCall(followersCount).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch player followers count");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            table.add(1, json.get("count"));
        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }

        try (Response response = client.newCall(followingCount).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch player following count");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            table.add(2, json.get("count"));
        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }

        return table;
    }

    private String getUserThumpnail(Long userID) throws Exception {
        String ThumpUrl = String.format("https://thumbnails.roblox.com/v1/users/avatar?userIds=%s&size=720x720&format=Png&isCircular=true", userID);

        Request request = new Request.Builder()
                .url(ThumpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to get player thumpnail");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            String imageUrl = data.getJSONObject(0).get("imageUrl").toString();

            return imageUrl;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public ArrayList<Long> getExperiences(Long userId) throws Exception {

        String gamepassUrl = String.format("https://games.roblox.com/v2/users/%s/games?accessFilter=2&limit=50&sortOrder=Asc", userId);
        System.out.println("Requesting URL: " + gamepassUrl);

        ArrayList<Long> experiences = new ArrayList<>();

        try {
            Request gameRequest = new Request.Builder()
                    .url(gamepassUrl)
                    .build();

            System.out.println("Sending request...");

            try (Response response = client.newCall(gameRequest).execute()) {
                if (!response.isSuccessful()) throw new Exception("Failed to fetch player experiences");

                assert response.body() != null;
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray data = json.getJSONArray("data");

                for (int i = 0; i < data.length(); i++) {
                    JSONObject child = data.getJSONObject(i);
                    Long id = child.getLong("id");
                    experiences.add(id);
                    System.out.println("UniverseID " + id);
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return experiences;
    }

    public List<List<Object>> getGamepasses(ArrayList<Long> experiences) throws Exception {
        List<List<Object>> gamepassIds = new ArrayList<>();
        boolean reachedLimit = false;

        for (Long universeId : experiences) {
            if (reachedLimit) break;

            try {
                String url = String.format("https://games.roblox.com/v1/games/%s/game-passes?limit=50&sortOrder=1", universeId);
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) continue;

                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray data = jsonResponse.getJSONArray("data");

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject gamepass = data.getJSONObject(i);
                        Long id = Long.parseLong(gamepass.get("id").toString());
                        if (!gamepass.isNull("price")) {
                            Long price = gamepass.isNull("price") | gamepass.getLong("price") <= 0 ? 0L : Long.parseLong(gamepass.get("price").toString());
                            String name = gamepass.getString("name");
                            System.out.println(id);

                            gamepassIds.add(Arrays.asList(id, price, name));
                        }
                        // Check if we've reached 50 gamepasses
                        if (gamepassIds.size() >= 50) {
                            reachedLimit = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error fetching gamepasses for universe " + universeId + ": " + e.getMessage());
                continue; // Continue with next universe instead of throwing exception
            }
        }

        return gamepassIds;
    }

    public void PlayerInfo(SlashCommandInteractionEvent event) {
        ErrorEmbed errorEmbed = new ErrorEmbed();

        event.deferReply().queue();

        String username = (String) Objects.requireNonNull(event.getOption("user_name", "", OptionMapping::getAsString));
        try {

            JSONObject userInfo = userInfo(username);
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
                    errorEmbed.Error("Error", e.getMessage(), "", "https://i.postimg.cc/g0tJpYc2/source.gif")
            ).queue();
        }
    }

    public void PlayerGamepasses(SlashCommandInteractionEvent event) {
        ErrorEmbed errorEmbed = new ErrorEmbed();

        event.deferReply().queue();
        String username1 = (String) Objects.requireNonNull(event.getOption("user_name", "", OptionMapping::getAsString));
        try {
            Long UserID = userIdByUsername(username1);
            ArrayList<Long> experiences = getExperiences(UserID);
            List<List<Object>> gamepassesId = getGamepasses(experiences);
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
                    errorEmbed.Error("Error", e.getMessage().length() > 2000 ? "something went wrong" : e.getMessage(), "", "https://i.postimg.cc/g0tJpYc2/source.gif")
            ).queue();
        }
    }
}

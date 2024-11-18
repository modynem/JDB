package API;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class Authenticated {
    private final OkHttpClient client = new OkHttpClient();
    private String roblosecurity; // Store your .ROBLOSECURITY cookie

    public Authenticated(String roblosecurity) {
        this.roblosecurity = roblosecurity;
    }

    public Long getUniverseID(Long placeID) {
        String url = "https://games.roblox.com/v1/games/multiget-place-details?placeIds=" + placeID;

        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", ".ROBLOSECURITY=" + roblosecurity)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to get game universeId: " + response.code() + " - " + response.message());
            }

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONArray jsonArray = new JSONArray(responseBody);
            JSONObject json = jsonArray.getJSONObject(0);
            return json.getLong("universeId");
        } catch (IOException e) {
            throw new RuntimeException("Failed to get game universeId", e);
        }
    }

    public JSONArray getGameInfo(Long universeId) {
        String url = "https://games.roblox.com/v1/games?universeIds=" + universeId;

        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", ".ROBLOSECURITY=" + roblosecurity)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get game info", e);
        }
    }
}
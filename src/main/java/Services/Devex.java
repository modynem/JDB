package Services;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.Locale;

public class Devex {
    private static final String API_KEY = "f56644dfe146f75c12c7461f"; // Get from exchangerate-api.com or similar service
    private final OkHttpClient client = new OkHttpClient();

    public void DevexCurrency(SlashCommandInteractionEvent event) {
        try {
            double amount = event.getOption("amount", 0.0, OptionMapping::getAsDouble);
            String targetCurrency = event.getOption("currency", "USD", OptionMapping::getAsString);

            // Fetch latest exchange rate
            String rate = getExchangeRate(targetCurrency);
            double exchangeRate = Double.parseDouble(rate);

            event.reply(String.format(Locale.US, "`%.2f %s` converts to `%.2f Robux`",
                            amount,
                            targetCurrency,
                            Math.round(amount/exchangeRate / 3.5 * 1000 * 100.0) / 100.0))
                    .queue();

        } catch (Exception e) {
            event.reply("❌ Error processing currency conversion: " + e.getMessage())
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void DevexRobux(SlashCommandInteractionEvent event) {
        try {
            double amount = event.getOption("amount", 0.0, OptionMapping::getAsDouble);
            String targetCurrency = event.getOption("currency", "USD", OptionMapping::getAsString);

            // Fetch latest exchange rate
            String rate = getExchangeRate(targetCurrency);
            double exchangeRate = Double.parseDouble(rate);
            double postTaxAmount = (amount / 1000 * 3.5 * exchangeRate) * 70 / 100;


            event.reply(String.format(Locale.US, "`%.2f` converts to `%.2f %s` (`%s %s` post Roblox tax)",
                            amount,
                            (amount / 1000) * 3.5 * exchangeRate,
                            targetCurrency,
                            postTaxAmount,
                            targetCurrency))
                    .queue();
        } catch (Exception e) {
            event.reply("❌ Error processing currency conversion: " + e.getMessage())
                    .setEphemeral(true)
                    .queue();
        }
    }

    private String getExchangeRate(String targetCurrency) throws Exception {
        // Build API request URL (using exchangerate-api.com as an example)
        String url = String.format("https://v6.exchangerate-api.com/v6/%s/latest/USD", API_KEY);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("❌ Failed to fetch exchange rate");

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONObject rates = json.getJSONObject("conversion_rates");

            return rates.get(targetCurrency).toString();
        }
    }
}

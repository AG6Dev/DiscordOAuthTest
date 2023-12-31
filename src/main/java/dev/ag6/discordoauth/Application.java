package dev.ag6.discordoauth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.*;
import io.javalin.Javalin;

public class Application {
    private static final String DISCORD_REDIRECT_URI = "https://discord.com/api/oauth2/authorize?client_id=1145769001557954580&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Foauth2%2Fdiscord&response_type=code&scope=identify%20guilds%20email";

    public static void main(String[] args) {
        Javalin javalin = Javalin.create().start(8080);
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        javalin.get("/oauth2/discord/", ctx -> {
            String code = ctx.queryParam("code");
            if (code != null) {
                RequestBody body = new FormEncodingBuilder()
                        .add("client_id", args[0])
                        .add("client_secret", args[1])
                        .add("scope", "identify guilds email")
                        .add("redirect_uri", "http://localhost:8080/oauth2/discord")
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .build();

                Request tokenRequest = new Request.Builder()
                        .url("https://discord.com/api/oauth2/token")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .post(body)
                        .build();

                Response response = client.newCall(tokenRequest).execute();
                String responseJson = response.body().string();
                System.out.println(responseJson);
                JsonObject jsonObject = gson.fromJson(responseJson, JsonObject.class);

                if(jsonObject.get("error") != null) {
                    ctx.result("Error: " + jsonObject.get("error").getAsString() + "  " + jsonObject.get("error_description").getAsString());
                    return;
                }

                String accessToken = jsonObject.get("access_token").getAsString();
                String tokenType = jsonObject.get("token_type").getAsString();

                Request infoRequest = new Request.Builder()
                        .url("https://discord.com/api/users/@me")
                        .header("Authorization", tokenType + " " + accessToken)
                        .build();
                Response infoResponse = client.newCall(infoRequest).execute();
                String infoResponseJson = infoResponse.body().string();
                JsonObject infoJsonObject = gson.fromJson(infoResponseJson, JsonObject.class);
                System.out.println(infoResponseJson);


                Request guildRequest = new Request.Builder()
                        .url("https://discord.com/api/users/@me/guilds")
                        .header("Authorization", tokenType + " " + accessToken)
                        .build();
                Response guildResponse = client.newCall(guildRequest).execute();
                String guildResponseJson = guildResponse.body().string();
                System.out.println(guildResponseJson);

                ctx.result("Hello " + infoJsonObject.get("username").getAsString() + "!");
            } else {
                ctx.redirect(DISCORD_REDIRECT_URI);
            }
            ctx.redirect(DISCORD_REDIRECT_URI);
        });
    }
}

package com.testeBanda.testador.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GlpiAPI {


    private static String url = "http://glpi.mp.rs.gov.br/apirest.php/initSession";
    private static String user_token = "k9TLu3w0pbeMD5Wnlby7UyhV3UmjMqtGJ8kpUXwf";
    private static String appToken = "jxtxZdirlRYzy4uHPvlgOH7wQV8jlXIeoaFjiDds";

    public GlpiAPI(String url, String userToken) {
        this.url = url;
        this.user_token = userToken;
    }

    public static String getSessionToken() throws IOException, InterruptedException {
        URI uri = URI.create(url);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("App-Token", appToken)
                .header("Authorization", "user_token " + user_token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response;
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response.body();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        GlpiAPI.getSessionToken();
    }
}

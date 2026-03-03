package com.testeBanda.testador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class GraficosService {

    private final HttpClient httpClient;

    public GraficosService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String pegarUnidadeSmoke(String cidade){
        String url = "http://linux61.mp.rs.gov.br/smokeping/?target="+ cidade.charAt(0) +"." +cidade;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Erro ao enviar uma URL " + url, e);
        }
        long timestamp = System.currentTimeMillis();
        return "http://linux61.mp.rs.gov.br/smokeping/images/" +
                cidade.charAt(0) + "/" +
                cidade + "_last_10800.png?" + timestamp;
    }

    public String cacti(String id) {
        Instant now = Instant.now();
        long graphEnd = now.getEpochSecond();
        long graphStart = now.minus(3, ChronoUnit.HOURS).getEpochSecond();
        String urlStr = "http://linux61.mp.rs.gov.br/cacti/graph_json.php?rra_id=0&local_graph_id="+id+"&" +
                "graph_start=" + graphStart +
                "&graph_end=" + graphEnd +
                "&graph_height=120&graph_width=500";
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(conn.getInputStream());

            String base64Image = rootNode.path("image").asText();

            return "<img src='data:image/png;base64," + base64Image + "' alt='Gráfico Cacti'  width=\"100%\"/>";

        } catch (Exception e) {
            e.printStackTrace();
            return "<p>Erro ao buscar imagem: " + e.getMessage() + "</p>";
        }
    }


}

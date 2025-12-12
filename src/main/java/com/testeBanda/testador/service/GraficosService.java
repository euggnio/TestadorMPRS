package com.testeBanda.testador.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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

    public String pegarUnidadeSmoke(String cidade){
        System.out.println(cidade);
        String url = "http://linux61.mp.rs.gov.br/smokeping/?target="+ cidade.charAt(0) +"." +cidade;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Resposta da URL: " + response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao executar SMOKE";
        }
        return "SMOKE OK";
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
            System.out.println(rootNode);

            String base64Image = rootNode.path("image").asText();

            return "<img src='data:image/png;base64," + base64Image + "' alt='Gráfico Cacti'  width=\"100%\"/>";

        } catch (Exception e) {
            e.printStackTrace();
            return "<p>Erro ao buscar imagem: " + e.getMessage() + "</p>";
        }
    }


}

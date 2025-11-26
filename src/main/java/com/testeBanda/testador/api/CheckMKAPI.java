package com.testeBanda.testador.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testeBanda.testador.models.Queda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class CheckMKAPI {

    @Value("${checkmk.username}")
    private String user;
    @Value("${checkmk.password}")
    private String password;

    public Long getUptimePosQueda(Queda queda){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://zeus.mp.rs.gov.br/atena/check_mk/api/1.0/domain-types/metric/actions/get/invoke"))
                .header("Authorization", "Basic " + this.getAuth())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(getJsonUptime(queda)))
                .build();
        JsonNode node = null;
        long uptime;
        try{
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.body());
            node = rootNode.path("metrics");
            uptime = node.get(0).path("data_points").get(0).asLong();
        }catch (Exception e) {
            System.out.println("ERRO CHECKMK API: " + e.getMessage());
            uptime = -1;
        }
        client.close();
        return uptime;
    }

    private String getJsonUptime(Queda queda) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String isoTime = formatter.format(queda.getDataUp(1));
            return """
        {
          "time_range": {
            "start": "%s",
            "end": "%s"
          },
          "reduce": "max",
          "site": "atena",
          "host_name": "%s",
          "service_description": "Uptime",
          "type": "single_metric",
          "metric_id": "uptime"
        }
        """.formatted(isoTime, isoTime, queda.getCidade().checkMKid);
    }

    private String getAuth(){
        String auth = user +":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }
}

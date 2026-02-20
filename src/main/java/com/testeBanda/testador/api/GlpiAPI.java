package com.testeBanda.testador.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

@Slf4j
@Service
public class GlpiAPI {
    private final HttpClient httpClient;
    @Value("${glpi.startSessionUrl}")
    private String startSessionUrl;
    @Value("${glpi.ticketUrl}")
    private String TicketUrl;
    @Value("${glpi.userToken}")
    private String user_token;
    @Value("${glpi.appToken}")
    private String appToken;
    private String tokenInUse = "";

    @Autowired
    public GlpiAPI(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized void getSessionToken() throws IOException, InterruptedException {
        log.info("Requisitando novo token de sessão");
        if ( !Objects.equals(tokenInUse, "") && isTokenValido() ) {
            log.info("Token ainda é válido");
            return;
        }
        log.warn("Token expirado, enviando nova requisição");
        URI uri = URI.create(startSessionUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("App-Token", appToken)
                .header("Authorization", "user_token " + user_token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response;
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = new ObjectMapper().readTree(response.body());
        log.info("novo token {}", json.get("session_token").asText());
        this.tokenInUse = json.get("session_token").asText();
    }

    private boolean isTokenValido() {
        String url = "http://glpi.mp.rs.gov.br/apirest.php/getMyEntities";
        HttpResponse<String> response = sendHttp(url, "", true, true);
        return response.statusCode() == 200;
    }

    public String createGlpiTicket(String unidade) {
        log.info("Criando chamado GLPI referente à unidade: {}", unidade);
        HttpResponse<String> response = sendHttp(TicketUrl, createGlpiJson(unidade), true, false);
        if ( response.statusCode() != 201 ) {
            log.error("Chamado não foi criado");
            return "ERRO";
        }
        try {
            JsonNode json = new ObjectMapper().readTree(response.body());
            JsonNode ticket = json.get("id");
            if ( ticket == null || ticket.isNull() ) {
                log.error("Resposta do GLPI não contém ID do ticket. Body: {}", response.body());
                return "ERRO";
            }
            log.info("Chamado criado com sucesso - GLPI {}", ticket);
            return ticket.asText();
        } catch (JsonProcessingException e) {
            log.error("Chamado não foi criado {}", e.getMessage());
            return "ERRO";
        }
    }

    public void insertFollowUpTicket(String ticket, String content) {
        log.info("Inserindo FollowUp ao chamado {} : {}", ticket, content);
        String uri = TicketUrl + "/" + ticket + "/ITILFollowup";
        HttpResponse<String> response = sendHttp(uri, addFollowUpJson(ticket, content), true, false);
        if ( response.statusCode() == 201 ) {
            log.info("FollowUp foi adicionado ao chamado {} com sucesso", ticket);
        } else {
            log.error("FollowUp não foi adicionado ao chamado {}", ticket);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao adicionar FollowUp ao chamado " + ticket);
        }
    }

    public void closeGlpiTicket(String ticket) {
        log.info("Fechando chamado {}", ticket);
        String uri = TicketUrl + "/" + ticket;
        JsonNode json;
        try {
            HttpResponse<String> response = sendHttp(uri, closeTicketJson(), false, false);
            if ( response.statusCode() != 200 ) {
                log.error("Falha no fechamento do ticket {}", ticket);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Falha no fechamento do ticket - PERMISSÃO");
            }

            json = new ObjectMapper().readTree(response.body());
            boolean sucess = false;

            if ( json.isArray() && json.has(0) ) {
                JsonNode firstElement = json.get(0);
                sucess = firstElement.get(ticket).asBoolean();
            }
            if ( sucess && response.statusCode() == 200 ) {
                log.info("Chamado {} fechado com sucesso", ticket);
            }
        } catch (Exception e) {
            log.error("Falha no fechamento do ticket {}", ticket, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha no fechamento do ticket");
        }
    }

    private HttpResponse<String> sendHttp(String url, String json, boolean isPost, boolean passValidationToken) {
        try {
            if ( !passValidationToken ) {
                getSessionToken();
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("App-Token", appToken)
                    .header("session-token", tokenInUse)
                    .header("Content-Type", "application/json");
            if ( isPost ) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(json));
            } else {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(json));
            }
            HttpRequest request = requestBuilder.build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Erro na requisição : ", e);
            throw new RuntimeException(e);
        }
    }

    public void assignTicketToUser(String ticket, String user) {
        String uri = TicketUrl + "/" + ticket;
        HttpResponse<String> response = sendHttp(uri, assignUserToTicketJson(user), false, false);
        if ( response.statusCode() != 200 ) {
            log.error("Falha na atribuição do ticket {} - ERRO:  {}", ticket, response.statusCode());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Falha na atribuição do ticket");
        }
        try{
            JsonNode json = new ObjectMapper().readTree(response.body());
            if(json.get(0) == null ) {
                log.error("Falha na atribuição do ticket {}", ticket);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falha na atribuição do ticket - Chamado não existe ");
            }

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao fazer assign usuario {}", e);
        }

        log.info("Usuario {} foi assignado com sucesso", user);
    }

    public String assignUserToTicketJson(String user) {
        String json = """
                {
                    "input": {
                        "_users_id_assign": %s
                    }
                }
                """;
        return String.format(json, user);
    }

    public String closeTicketJson() {
        return """
                        {
                          "input": {
                            "status": 6,
                            "solution": "<p>Chamado finallizado</p>",
                            "content": "<p>Chamado finallizado</p>",
                            "solutiontypes_id": 1,
                            "solutiontemplates_id": 5
                          }
                        }
                """;
    }

    public String createGlpiJson(String nomeDoHost) {
        String json = """
                {
                    "input": {
                        "name": "Unidade sem conexão - %s",
                        "content": "Unidade de %s está sem conexão de internet",
                        "itilcategories_id": 472,
                        "type": 1,
                        "urgency": 3,
                        "impact": 3,
                        "priority": 3,
                        "_groups_id_assign": 843,
                        "entities_id": 0
                    }
                }
                """;
        return String.format(json, nomeDoHost, nomeDoHost);
    }

    public String addFollowUpJson(String ticket, String content) {
        String json = """
                    {
                        "input": {
                            "itemtype":"Ticket",
                            "items_id":%s,
                            "content": "%s"
                        }
                    }
                """;
        return String.format(json, ticket, content);
    }

}

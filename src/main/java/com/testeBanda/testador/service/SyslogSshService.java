package com.testeBanda.testador.service;

import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.repository.CidadesRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SyslogSshService {

    private static final Logger log = LoggerFactory.getLogger(SyslogSshService.class);

    private static final String HOST = "syslog.mp.rs.gov.br";
    private static final String USER = "eugeniocr";

    private static final String KEY =
            System.getProperty("user.home") + "/.ssh/vigia_syslog_rsa";

    private static final String BASE_DIR = "/var/log/syslogcentral";

    private String opcaoPubkey = "PubkeyAcceptedKeyTypes"; // default para OpenSSH 7.0–8.4

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+([\\w\\-]+)\\[(\\w+)\\]\\s+(\\d+)\\s+(.*)$"
    );

    private static final String[] SECOES = {
            "User:", "Client Machine:", "NAS:", "RADIUS Client:", "Authentication Details:"
    };

    private static final String[] CAMPOS_USER = {
            "Security ID:", "Account Name:", "Account Domain:", "Fully Qualified Account Name:"
    };
    private static final String[] CAMPOS_CLIENT_MACHINE = {
            "Security ID:", "Account Name:", "Fully Qualified Account Name:", "OS-Version:",
            "Called Station Identifier:", "Calling Station Identifier:"
    };
    private static final String[] CAMPOS_NAS = {
            "NAS IPv4 Address:", "NAS IPv6 Address:", "NAS Identifier:", "NAS Port-Type:", "NAS Port:"
    };
    private static final String[] CAMPOS_RADIUS = {
            "Client Friendly Name:", "Client IP Address:"
    };
    private static final String[] CAMPOS_AUTH = {
            "Connection Request Policy Name:", "Network Policy Name:", "Authentication Provider:",
            "Authentication Server:", "Authentication Type:", "EAP Type:", "Account Session Identifier:"
    };

    // ---------- Cache por cidade ----------

    private static class CacheEntry {
        List<String> linhas;
        String ultimoTimestamp;
        Instant carregadoEm;
        int consultasVaziasConsecutivas = 0;

        CacheEntry(List<String> linhas, String ultimoTimestamp) {
            this.linhas = linhas;
            this.ultimoTimestamp = ultimoTimestamp;
            this.carregadoEm = Instant.now();
        }
    }

    private final CidadesRepository cidadesRepository;
    private final Map<String, CacheEntry> cachePorCidade = new ConcurrentHashMap<>();
    private final Map<String, Lock> locksPorCidade = new ConcurrentHashMap<>();

    public SyslogSshService(CidadesRepository cidadesRepository) {
        this.cidadesRepository = cidadesRepository;
    }

    @PostConstruct
    public void detectarSsh() {
        try {
            Process p = new ProcessBuilder("ssh", "-V").start();
            String stderr = new String(p.getErrorStream().readAllBytes());
            Matcher m = Pattern.compile("OpenSSH_(\\d+)\\.(\\d+)").matcher(stderr);
            if (m.find()) {
                int major = Integer.parseInt(m.group(1));
                int minor = Integer.parseInt(m.group(2));
                // 8.5+ renomeou PubkeyAcceptedKeyTypes → PubkeyAcceptedAlgorithms
                if (major > 8 || (major == 8 && minor >= 5)) {
                    opcaoPubkey = "PubkeyAcceptedAlgorithms";
                }
            }
            log.info("SSH detectado: {} | opção pubkey: {}", stderr.trim(), opcaoPubkey);
        } catch (Exception e) {
            log.warn("Não detectou versão SSH, usando default: {}", opcaoPubkey);
        }
    }

    // ---------- Helpers ----------

    private String extrairSubnet(String ip) {
        if (ip == null || ip.isBlank()) return null;
        String[] o = ip.split("\\.");
        return o.length >= 3 ? o[0] + "." + o[1] + "." + o[2] : null;
    }

    private String extrairTimestamp(String linha) {
        Matcher m = HEADER_PATTERN.matcher(linha);
        return m.matches() ? m.group(1) : null;
    }

    private String normalizarTimestamp(String ts) {
        if (ts == null || ts.isBlank()) return "";
        String[] partes = ts.trim().split("\\s+");
        if (partes.length < 3) return "";
        String mesNum = mesParaNumero(partes[0]);
        String dia = partes[1].length() == 1 ? "0" + partes[1] : partes[1];
        return mesNum + "-" + dia + "-" + partes[2];
    }

    private String mesParaNumero(String mes) {
        return switch (mes.toLowerCase()) {
            case "jan" -> "01"; case "feb" -> "02"; case "mar" -> "03";
            case "apr" -> "04"; case "may" -> "05"; case "jun" -> "06";
            case "jul" -> "07"; case "aug" -> "08"; case "sep" -> "09";
            case "oct" -> "10"; case "nov" -> "11"; case "dec" -> "12";
            default -> "00";
        };
    }

    private String formatarDataParaLog(String dataIso) {
        LocalDate d = LocalDate.parse(dataIso);
        String nomeMes = d.getMonth().name().substring(0, 1).toUpperCase()
                + d.getMonth().name().substring(1).toLowerCase();
        return nomeMes + " " + d.getDayOfMonth();
    }

    // ---------- Construção do comando SSH ----------

    private String montarComandoRemoto(String subnet, String mesDiaInicio, String mesDiaFim) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("cd ").append(BASE_DIR).append(" && cat NPS*/all.log");
        cmd.append(" | LC_ALL=C sed 's/[^\\x00-\\x7F]//g'");
        cmd.append(" | grep \"").append(subnet.replace(".", "\\.")).append("\\..*Wireless\"");

        // LIMITAÇÃO: o log do NPS não tem ano — apenas "Aug 26 08:03:55".
        // Isso é aceitável para logs recentes (< 12 meses), mas pode causar
        // resultados incorretos se alguém tentar filtrar datas de anos diferentes.
        if (mesDiaInicio != null || mesDiaFim != null) {
            String[] partesInicio = mesDiaInicio != null ? mesDiaInicio.split(" ") : new String[]{"", "1"};
            String[] partesFim = mesDiaFim != null ? mesDiaFim.split(" ") : new String[]{"", "31"};
            String mes = partesInicio[0];
            int diaInicio = Integer.parseInt(partesInicio[1]);
            int diaFim = Integer.parseInt(partesFim[1]);

            cmd.append(" | awk '$1==\"").append(mes).append("\"")
                    .append(" && $2+0>=").append(diaInicio)
                    .append(" && $2+0<=").append(diaFim).append("'");
        }

        cmd.append(" | sort");
        return cmd.toString();
    }

    private String montarComandoWcL(String subnet) {
        return "cd " + BASE_DIR + " && cat NPS*/all.log"
                + " | LC_ALL=C sed 's/[^\\x00-\\x7F]//g'"
                + " | grep \"" + subnet.replace(".", "\\.") + "\\..*Wireless\""
                + " | wc -l";
    }

    // ---------- Execução SSH ----------

    private List<String> executarSsh(String comandoRemoto) throws Exception {
        log.debug("SSH key: {}", KEY);
        log.debug("SSH comando remoto: {}", comandoRemoto);

        ProcessBuilder pb = new ProcessBuilder(
                "ssh",
                "-o", "IdentitiesOnly=yes",
                "-o", opcaoPubkey + "=+ssh-rsa",
                "-o", "BatchMode=yes",
                "-o", "ConnectTimeout=15",
                "-i", KEY,
                USER + "@" + HOST,
                comandoRemoto
        );

        Process process = pb.start();
        @SuppressWarnings("unchecked")
        List<String>[] resultados = new ArrayList[]{new ArrayList<>(), new ArrayList<>()};

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                resultados[0] = reader.lines()
                        .filter(l -> l != null && !l.isBlank())
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                resultados[1] = errReader.lines().collect(Collectors.toList());
            } catch (Exception ignored) {}
        });

        stdoutThread.start();
        stderrThread.start();
        stdoutThread.join();
        stderrThread.join();

        String erro = String.join("\n", resultados[1]);
        int exit = process.waitFor();
        if (exit != 0 && resultados[0].isEmpty()) {
            log.error("SSH falhou (exit={}): {}", exit, erro);
            throw new RuntimeException("Falha ao executar SSH (exit=" + exit + "): " + erro);
        }
        log.debug("SSH ok, {} linhas recebidas", resultados[0].size());
        return resultados[0];
    }

    // ---------- Cache info ----------

    public List<Map<String, Object>> infosCache() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> e : cachePorCidade.entrySet()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("cidade", e.getKey());
            info.put("linhas", e.getValue().linhas.size());
            info.put("ultimoTimestamp", e.getValue().ultimoTimestamp);
            info.put("carregadoEm", e.getValue().carregadoEm.toString());
            lista.add(info);
        }
        return lista;
    }

    // ---------- Ponto de entrada: busca por cidade ----------

    public String buscarLogsPorCidade(String nomeCidade, String dataInicio, String dataFim) {
        Cidades cidade = cidadesRepository.findById(nomeCidade).orElse(null);
        if (cidade == null || cidade.ip == null) {
            return paginaErro(new RuntimeException("Cidade não encontrada ou sem IP: " + nomeCidade));
        }
        String subnet = extrairSubnet(cidade.ip);
        if (subnet == null) {
            return paginaErro(new RuntimeException("IP inválido para " + nomeCidade + ": " + cidade.ip));
        }

        Lock lock = locksPorCidade.computeIfAbsent(nomeCidade, k -> new ReentrantLock());
        lock.lock();
        try {
            String mesDiaInicio = dataInicio != null ? formatarDataParaLog(dataInicio) : null;
            String mesDiaFim = dataFim != null ? formatarDataParaLog(dataFim) : null;

            boolean buscaIncremental = cachePorCidade.containsKey(nomeCidade)
                    && mesDiaInicio == null && mesDiaFim == null;

            if (buscaIncremental) {
                CacheEntry cache = cachePorCidade.get(nomeCidade);

                // Opção A: buscar SEM filtro de mês/dia no servidor.
                // Apenas grep subnet + Wireless. O filtro timestamp > watermark
                // é feito inteiramente em Java, eliminando o bug de virada de mês.
                String cmd = montarComandoRemoto(subnet, null, null);
                List<String> novasLinhas = executarSsh(cmd);

                String wmNormalizado = normalizarTimestamp(cache.ultimoTimestamp);
                List<String> filtradas = novasLinhas.stream()
                        .filter(l -> {
                            String ts = extrairTimestamp(l);
                            return ts != null && normalizarTimestamp(ts).compareTo(wmNormalizado) > 0;
                        })
                        .toList();

                if (!filtradas.isEmpty()) {
                    List<String> todas = new ArrayList<>(cache.linhas);
                    todas.addAll(filtradas);
                    todas.sort(Comparator.naturalOrder());
                    cache.linhas = todas;
                    cache.ultimoTimestamp = extrairTimestamp(todas.get(todas.size() - 1));
                    cache.carregadoEm = Instant.now();
                    cache.consultasVaziasConsecutivas = 0;

                } else {
                    cache.consultasVaziasConsecutivas++;

                    // Detecção de rotação: após 3 consultas vazias consecutivas,
                    // verificar se o volume total de logs no servidor encolheu.
                    if (cache.consultasVaziasConsecutivas >= 3) {
                        String wcCmd = montarComandoWcL(subnet);
                        List<String> wcResult = executarSsh(wcCmd);
                        int totalRemoto = Integer.parseInt(wcResult.get(0).trim());

                        if (totalRemoto < cache.linhas.size()) {
                            // Rotação detectada: arquivo encolheu → refazer tudo do zero
                            String cmdCompleto = montarComandoRemoto(subnet, null, null);
                            List<String> linhasCompletas = executarSsh(cmdCompleto);
                            String novoTs = linhasCompletas.isEmpty() ? null
                                    : extrairTimestamp(linhasCompletas.get(linhasCompletas.size() - 1));
                            cache.linhas = linhasCompletas;
                            cache.ultimoTimestamp = novoTs;
                            cache.carregadoEm = Instant.now();
                        }
                        cache.consultasVaziasConsecutivas = 0;
                    }
                }

            } else {
                String cmd = montarComandoRemoto(subnet, mesDiaInicio, mesDiaFim);
                List<String> linhas = executarSsh(cmd);

                String ultimoTs = linhas.isEmpty() ? null
                        : extrairTimestamp(linhas.get(linhas.size() - 1));
                cachePorCidade.put(nomeCidade, new CacheEntry(linhas, ultimoTs));
            }

            CacheEntry entry = cachePorCidade.get(nomeCidade);
            return renderizarCidadeHtml(nomeCidade, entry != null ? entry.linhas : new ArrayList<>());

        } catch (Exception e) {
            return paginaErro(e);
        } finally {
            lock.unlock();
        }
    }

    // ---------- Ponto de entrada: shell HTML (sem chamada SSH) ----------

    public String buscarLogsHtml() {
        List<Cidades> cidades = cidadesRepository.findAll();

        StringBuilder html = new StringBuilder();
        html.append(cabecalhoHtml());

        html.append("<div class=\"seletor-container\">");
        html.append("<label for=\"seletor-cidade\">Selecione a cidade:</label> ");
        html.append("<select id=\"seletor-cidade\" onchange=\"carregarCidade(this.value)\">");
        html.append("<option value=\"\">-- Selecione --</option>");
        for (Cidades c : cidades) {
            if (c.ip != null && !c.ip.isBlank()) {
                html.append("<option value=\"").append(escapeHtml(c.nome)).append("\">")
                        .append(escapeHtml(c.nome))
                        .append(" (").append(escapeHtml(c.ip)).append(")</option>");
            }
        }
        html.append("</select>");
        html.append("<button class=\"atualizar-btn\" onclick=\"atualizarCidade()\" title=\"Atualizar logs da cidade selecionada\">Atualizar</button>");
        html.append("</div>");

        html.append("<div class=\"filtro-data-container\">");
        html.append("<label for=\"data-inicio\">De:</label>");
        html.append("<input type=\"date\" id=\"data-inicio\" onchange=\"recarregarSeCidadeSelecionada()\">");
        html.append("<label for=\"data-fim\">Até:</label>");
        html.append("<input type=\"date\" id=\"data-fim\" onchange=\"recarregarSeCidadeSelecionada()\">");
        html.append("</div>");

        html.append("<div id=\"logs-container\"></div>");

        html.append(rodapeHtml());
        return html.toString();
    }

    // ---------- Renderização de uma cidade ----------

    private String renderizarCidadeHtml(String nomeCidade, List<String> linhas) {
        StringBuilder html = new StringBuilder();
        if (linhas.isEmpty()) {
            html.append("<p class=\"vazio\">Nenhum registro encontrado para ")
                    .append(escapeHtml(nomeCidade)).append(".</p>");
        } else {
            for (String linha : linhas) {
                html.append(renderizarLinha(linha));
            }
        }
        return html.toString();
    }

    // ---------- Parsing das linhas do NPS ----------

    private String renderizarLinha(String linhaOriginal) {
        Matcher m = HEADER_PATTERN.matcher(linhaOriginal);

        if (!m.matches()) {
            return "<div class=\"log-card log-desconhecido\"><pre>"
                    + escapeHtml(linhaOriginal) + "</pre></div>";
        }

        String data = m.group(1);
        String hostLog = m.group(2);
        String status = m.group(4);
        String eventId = m.group(5);
        String resto = m.group(6);

        int idxUser = resto.indexOf("User:");
        String descricao = idxUser >= 0 ? resto.substring(0, idxUser) : resto;

        Map<String, Map<String, String>> blocos = idxUser >= 0
                ? separarSecoes(resto.substring(idxUser))
                : new LinkedHashMap<>();

        boolean negado = status.toLowerCase().contains("fail");

        StringBuilder card = new StringBuilder();
        card.append("<div class=\"log-card ").append(negado ? "negado" : "concedido").append("\">");

        card.append("<div class=\"log-topo\">")
                .append("<span class=\"badge ").append(negado ? "badge-negado" : "badge-ok").append("\">")
                .append(negado ? "ACESSO NEGADO" : "ACESSO CONCEDIDO").append("</span>")
                .append("<span class=\"data\">").append(escapeHtml(data)).append("</span>")
                .append("<span class=\"host\">").append(escapeHtml(hostLog)).append("</span>")
                .append("<span class=\"evento\">Evento ").append(escapeHtml(eventId)).append("</span>")
                .append("</div>");

        card.append("<p class=\"descricao\">").append(escapeHtml(descricao.trim())).append("</p>");

        card.append("<div class=\"log-grid\">");
        card.append(renderizarBloco("Usuário", blocos.get("User:")));
        card.append(renderizarBloco("Estação Cliente", blocos.get("Client Machine:")));
        card.append(renderizarBloco("NAS", blocos.get("NAS:")));
        card.append(renderizarBloco("Cliente RADIUS", blocos.get("RADIUS Client:")));
        card.append(renderizarBloco("Autenticação", blocos.get("Authentication Details:")));
        card.append("</div>");

        card.append("</div>");
        return card.toString();
    }

    private Map<String, Map<String, String>> separarSecoes(String corpo) {
        Map<String, Map<String, String>> resultado = new LinkedHashMap<>();
        List<int[]> posicoes = new ArrayList<>();

        for (int i = 0; i < SECOES.length; i++) {
            int pos = corpo.indexOf(SECOES[i]);
            if (pos >= 0) posicoes.add(new int[]{i, pos});
        }
        posicoes.sort((a, b) -> Integer.compare(a[1], b[1]));

        for (int k = 0; k < posicoes.size(); k++) {
            int secaoIdx = posicoes.get(k)[0];
            int inicio = posicoes.get(k)[1] + SECOES[secaoIdx].length();
            int fim = (k + 1 < posicoes.size()) ? posicoes.get(k + 1)[1] : corpo.length();
            String conteudo = corpo.substring(inicio, fim);
            resultado.put(SECOES[secaoIdx], extrairCampos(conteudo, camposDaSecao(SECOES[secaoIdx])));
        }
        return resultado;
    }

    private String[] camposDaSecao(String secao) {
        switch (secao) {
            case "User:": return CAMPOS_USER;
            case "Client Machine:": return CAMPOS_CLIENT_MACHINE;
            case "NAS:": return CAMPOS_NAS;
            case "RADIUS Client:": return CAMPOS_RADIUS;
            case "Authentication Details:": return CAMPOS_AUTH;
            default: return new String[0];
        }
    }

    private Map<String, String> extrairCampos(String texto, String[] labels) {
        Map<String, String> campos = new LinkedHashMap<>();
        List<int[]> posicoes = new ArrayList<>();

        for (int i = 0; i < labels.length; i++) {
            int pos = texto.indexOf(labels[i]);
            if (pos >= 0) posicoes.add(new int[]{i, pos});
        }
        posicoes.sort((a, b) -> Integer.compare(a[1], b[1]));

        for (int k = 0; k < posicoes.size(); k++) {
            int labelIdx = posicoes.get(k)[0];
            int inicio = posicoes.get(k)[1] + labels[labelIdx].length();
            int fim = (k + 1 < posicoes.size()) ? posicoes.get(k + 1)[1] : texto.length();
            String valor = texto.substring(inicio, fim).trim();
            campos.put(labels[labelIdx].replace(":", ""), valor.isEmpty() ? "-" : valor);
        }
        return campos;
    }

    private String renderizarBloco(String titulo, Map<String, String> campos) {
        if (campos == null || campos.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"bloco\"><h4>").append(escapeHtml(titulo)).append("</h4><ul>");
        for (Map.Entry<String, String> e : campos.entrySet()) {
            sb.append("<li><span class=\"chave\">").append(escapeHtml(e.getKey())).append(":</span> ")
                    .append("<span class=\"valor\">").append(escapeHtml(e.getValue())).append("</span></li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ---------- HTML ----------

    private String cabecalhoHtml() {
        return "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"><title>Logzeitor NPS - Wireless</title>"
                + "<style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;background:#f4f6f8;margin:0;padding:24px;color:#1f2933;}"
                + "h1{font-size:20px;margin-bottom:16px;position:relative;}"
                + ".vazio{color:#6b7280;}"
                + ".seletor-container{margin-bottom:12px;padding:12px 16px;background:#fff;border-radius:8px;"
                + "box-shadow:0 1px 3px rgba(0,0,0,0.08);display:flex;align-items:center;gap:12px;}"
                + ".seletor-container label{font-weight:600;font-size:14px;color:#374151;}"
                + ".seletor-container select{padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;"
                + "font-size:14px;background:#fff;cursor:pointer;min-width:300px;}"
                + ".atualizar-btn{padding:8px 16px;background:#10b981;color:#fff;border:none;border-radius:6px;"
                + "cursor:pointer;font-weight:600;font-size:13px;}"
                + ".atualizar-btn:hover{background:#059669;}"
                + ".filtro-data-container{margin-bottom:20px;padding:12px 16px;background:#fff;border-radius:8px;"
                + "box-shadow:0 1px 3px rgba(0,0,0,0.08);display:flex;align-items:center;gap:12px;}"
                + ".filtro-data-container label{font-weight:600;font-size:14px;color:#374151;}"
                + ".filtro-data-container input{padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:14px;}"
                + ".cache-badge{display:inline-block;background:#3b82f6;color:#fff;font-size:11px;"
                + "padding:2px 8px;border-radius:999px;cursor:pointer;vertical-align:middle;margin-left:8px;}"
                + ".cache-badge:hover{background:#2563eb;}"
                + ".cache-tooltip{position:fixed;background:#1e293b;color:#e2e8f0;padding:12px 16px;"
                + "border-radius:8px;font-size:12px;z-index:100;max-width:400px;"
                + "box-shadow:0 4px 12px rgba(0,0,0,0.2);}"
                + ".cache-tooltip table{width:100%;border-collapse:collapse;}"
                + ".cache-tooltip th{text-align:left;color:#94a3b8;padding:4px 8px 4px 0;font-weight:500;}"
                + ".cache-tooltip td{padding:4px 8px 4px 0;border-top:1px solid #334155;}"
                + ".log-card{background:#fff;border-radius:10px;padding:16px 20px;margin-bottom:14px;"
                + "box-shadow:0 1px 3px rgba(0,0,0,0.08);border-left:5px solid #9ca3af;}"
                + ".log-card.negado{border-left-color:#dc2626;}"
                + ".log-card.concedido{border-left-color:#16a34a;}"
                + ".log-topo{display:flex;flex-wrap:wrap;gap:12px;align-items:center;margin-bottom:8px;font-size:13px;color:#4b5563;}"
                + ".badge{font-weight:600;font-size:12px;padding:2px 10px;border-radius:999px;color:#fff;}"
                + ".badge-negado{background:#dc2626;}"
                + ".badge-ok{background:#16a34a;}"
                + ".descricao{margin:6px 0 12px 0;color:#374151;font-size:14px;}"
                + ".log-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;}"
                + ".bloco h4{margin:0 0 6px 0;font-size:13px;color:#111827;border-bottom:1px solid #e5e7eb;padding-bottom:4px;}"
                + ".bloco ul{list-style:none;margin:0;padding:0;font-size:12.5px;}"
                + ".bloco li{margin-bottom:3px;word-break:break-word;}"
                + ".chave{color:#6b7280;}"
                + ".valor{color:#111827;font-weight:500;}"
                + ".log-desconhecido pre{white-space:pre-wrap;font-size:12px;}"
                + "</style></head><body>"
                + "<h1>Logzeitor NPS - Rede Wireless <span id=\"cache-badge\" class=\"cache-badge\" "
                + "onmouseenter=\"mostrarInfoCache(event)\" "
                + "onmouseleave=\"esconderInfoCache()\">0</span></h1>"
                + "<div id=\"cache-tooltip\" class=\"cache-tooltip\" style=\"display:none;\"></div>";
    }

    private String rodapeHtml() {
        return "<script>"
                + "function carregarCidade(nome) {"
                + "  var container = document.getElementById('logs-container');"
                + "  if (!nome) { container.innerHTML = ''; return; }"
                + "  var di = document.getElementById('data-inicio').value;"
                + "  var df = document.getElementById('data-fim').value;"
                + "  var url = '/teste/eventos/cidade/' + encodeURIComponent(nome);"
                + "  var params = [];"
                + "  if (di) params.push('dataInicio=' + di);"
                + "  if (df) params.push('dataFim=' + df);"
                + "  if (params.length) url += '?' + params.join('&');"
                + "  container.innerHTML = '<p class=\"vazio\">Carregando...</p>';"
                + "  fetch(url).then(function(r) { return r.text(); }).then(function(html) {"
                + "    container.innerHTML = html;"
                + "    atualizarBadgeCache();"
                + "  }).catch(function(err) {"
                + "    container.innerHTML = '<p style=\"color:#dc2626\">Erro ao carregar: ' + err + '</p>';"
                + "  });"
                + "}"
                + "function atualizarCidade() {"
                + "  var sel = document.getElementById('seletor-cidade');"
                + "  if (sel.value) carregarCidade(sel.value);"
                + "}"
                + "function recarregarSeCidadeSelecionada() {"
                + "  var sel = document.getElementById('seletor-cidade');"
                + "  if (sel.value) carregarCidade(sel.value);"
                + "}"
                + "function atualizarBadgeCache() {"
                + "  fetch('/teste/eventos/cache-info')"
                + "    .then(function(r) { return r.json(); })"
                + "    .then(function(data) {"
                + "      var badge = document.getElementById('cache-badge');"
                + "      if (badge) badge.textContent = data.length;"
                + "    });"
                + "}"
                + "var cacheTooltipTimeout;"
                + "function mostrarInfoCache(event) {"
                + "  var tooltip = document.getElementById('cache-tooltip');"
                + "  clearTimeout(cacheTooltipTimeout);"
                + "  fetch('/teste/eventos/cache-info')"
                + "    .then(function(r) { return r.json(); })"
                + "    .then(function(data) {"
                + "      if (data.length === 0) {"
                + "        tooltip.innerHTML = '<em>Nenhuma cidade cacheada</em>';"
                + "      } else {"
                + "        var html = '<table><tr><th>Cidade</th><th>Linhas</th><th>Ultimo log</th><th>Carregado</th></tr>';"
                + "        data.forEach(function(item) {"
                + "          var dt = new Date(item.carregadoEm);"
                + "          var dtFmt = dt.toLocaleDateString('pt-BR') + ' ' + dt.toLocaleTimeString('pt-BR');"
                + "          html += '<tr><td>' + item.cidade + '</td><td>' + item.linhas + '</td><td>' + item.ultimoTimestamp + '</td><td>' + dtFmt + '</td></tr>';"
                + "        });"
                + "        html += '</table>';"
                + "        tooltip.innerHTML = html;"
                + "      }"
                + "      tooltip.style.display = 'block';"
                + "      tooltip.style.left = event.clientX + 'px';"
                + "      tooltip.style.top = (event.clientY + 16) + 'px';"
                + "    });"
                + "}"
                + "function esconderInfoCache() {"
                + "  cacheTooltipTimeout = setTimeout(function() {"
                + "    document.getElementById('cache-tooltip').style.display = 'none';"
                + "  }, 300);"
                + "}"
                + "atualizarBadgeCache();"
                + "</script>"
                + "</body></html>";
    }

    private String paginaErro(Exception e) {
        return "<html><body><h2 style=\"color:#dc2626\">Erro ao buscar logs via SSH</h2><pre>"
                + escapeHtml(String.valueOf(e.getMessage())) + "</pre></body></html>";
    }
}

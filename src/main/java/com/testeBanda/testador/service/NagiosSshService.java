package com.testeBanda.testador.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NagiosSshService {

    @Value("${nagios.ssh.host}")
    private String host;

    @Value("${nagios.ssh.user}")
    private String user;

    @Value("${nagios.ssh.port:22}")
    private int port;

    @Value("${nagios.script.add-link:/usr/local/bin/add_host.sh}")
    private String script;

    public Resultado cadastrarLinkPrimario(String hostPai,
                                           String nomeLink,
                                           String ip)
            throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "ssh",
                "-p", String.valueOf(port),
                "-o", "BatchMode=yes",
                "-o", "StrictHostKeyChecking=no",
                user + "@" + host,
                "sudo",
                script,
                hostPai,
                nomeLink,
                ip
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        String saida;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            saida = br.lines().collect(Collectors.joining("\n"));
        }

        boolean terminou = process.waitFor(60, TimeUnit.SECONDS);

        if (!terminou) {
            process.destroyForcibly();
            return new Resultado(false, -1, "Timeout aguardando resposta do Nagios.");
        }

        return new Resultado(
                process.exitValue() == 0,
                process.exitValue(),
                saida
        );
    }

    public record Resultado(
            boolean sucesso,
            int exitCode,
            String mensagem
    ) {}
}
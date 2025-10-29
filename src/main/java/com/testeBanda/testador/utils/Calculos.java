package com.testeBanda.testador.utils;

import com.testeBanda.testador.models.Cidades;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculos {

    public static void CorrigirTesteResTesteBanda(Cidades cidade, List<Map<String, String>> resultado)  {
        double rx = 0;
        double tx = 0;
        int count = 0; // Contador de valores válidos

        for (Map<String, String> result : resultado) {
            Pattern dataPattern = Pattern.compile("data=\\{(.*?)\\}");
            Matcher dataMatcher = dataPattern.matcher(result.toString());

            if (dataMatcher.find()) {
                String dataContent = dataMatcher.group(1);
                String[] entries = dataContent.split(", ");
                Map<String, String> dataMap = new HashMap<>();

                for (String entry : entries) {
                    String[] keyValue = entry.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        dataMap.put(key, value);
                    }
                }

                if (dataMap.get("status").equals("running")) {
                    double rxValue = Double.parseDouble(dataMap.get("rx-10-second-average")) / 1_000_000;
                    double txValue = Double.parseDouble(dataMap.get("tx-10-second-average")) / 1_000_000;

                    if (rxValue > 0 && txValue > 0) {
                        rx += rxValue;
                        tx += txValue;
                        count++;
                    }
                } else {
                    System.out.println("Dados de banda não encontrados.");
                }
            } else {
                System.out.println("Formato inesperado no resultado.");
            }
        }
        double rxMean = (rx / count);
        double txMean = (tx / count);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        cidade.ultimoTesteBanda = "RX: " + String.format("%.3f", rxMean)
                + " | TX: " + String.format("%.3f", txMean);
        LocalDateTime now = LocalDateTime.now();
        cidade.dataUltimoTeste = formatter.format(now);
        if ( cidade.getVelocidadeInteger() == 20 ) {
            cidade.checkTesteBanda = (rxMean > 17 && txMean > 17);
            return;
        } else if ( cidade.getVelocidadeInteger() == 50 ) {
            cidade.checkTesteBanda = (rxMean > 43 && txMean > 43);
            return;
        } else if ( cidade.getVelocidadeInteger() == 100 ) {
            cidade.checkTesteBanda = (rxMean > 94 && txMean > 94);
            return;
        } else {
            cidade.checkTesteBanda = false;
        }
        if (count > 0) {
            System.out.println("Dividido por " + rx + " / " + tx + " :: " + count);
            System.out.println("Rx: " + rx / count);
            System.out.println("Tx: " + tx / count);
        } else {
            System.out.println("Nenhum dado válido encontrado.");
        }
    }

    public static double calcularMedia(List<Map<String, String>> resultados, String chave) {
        double soma = 0;
        int contador = 0;
        for (Map<String, String> mapa : resultados) {
            if (mapa.containsKey(chave) && !mapa.get("status").equals("connecting")) {
                try {
                    double valor = Double.parseDouble(mapa.get(chave)) / 1000000;
                    if(valor > 0) {
                        soma += valor;
                        contador++;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Valor inválido para a chave '" + chave + "': " + mapa.get(chave));
                }
            }
        }
        return contador > 0 ? soma / contador : 0;
    }

    private static final Set<String> PREPOSICOES = new HashSet<>(Arrays.asList(
            "de", "do", "da", "dos", "das"
    ));

    public static String formatarNomeSistema(String nome) {
        String[] partes = nome.toLowerCase().split("_");
        StringBuilder resultado = new StringBuilder();

        for (String parte : partes) {
            if (parte.isEmpty()) continue;

            if (PREPOSICOES.contains(parte)) {
                resultado.append(parte);
            } else {
                resultado.append(Character.toUpperCase(parte.charAt(0)))
                        .append(parte.substring(1));
            }
        }

        return resultado.toString();
    }

}



package com.testeBanda.testador.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoriaDTO(
        String key,
        String label,
        String icon,
        String cor,
        String regexNome,
        String regexDescricao,
        String rangeIp,
        String nomeExcluir,
        String descExcluir,
        String ipExcluir,
        String filtroUsuario
) {
}

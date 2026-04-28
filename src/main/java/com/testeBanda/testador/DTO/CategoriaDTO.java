package com.testeBanda.testador.DTO;

public record CategoriaDTO(
        String key,
        String label,
        String icon,
        String cor,
        String regexNome,
        String regexDescricao,
        String rangeIp
) {}
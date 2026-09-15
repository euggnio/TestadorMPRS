package com.testeBanda.testador.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventoDTO(
        @Size(min = 1, max = 30) String nome,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        String descricao
){}

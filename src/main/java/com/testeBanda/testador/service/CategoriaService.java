package com.testeBanda.testador.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testeBanda.testador.DTO.CategoriaDTO;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class CategoriaService {

    private final ObjectMapper objectMapper;
    private final String FILE_PATH = "categorias.json";

    public CategoriaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void salvarCategorias(List<CategoriaDTO> categorias) {
        try {
            File file = new File(FILE_PATH);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, categorias);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo de categorias", e);
        }
    }

    public List<CategoriaDTO> lerCategorias() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(file, new TypeReference<List<CategoriaDTO>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo de categorias", e);
        }
    }
}
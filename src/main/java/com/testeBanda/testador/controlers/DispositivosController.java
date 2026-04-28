package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.CategoriaDTO;
import com.testeBanda.testador.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class DispositivosController {
    private final CategoriaService categoriaService;


    public DispositivosController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
        public ResponseEntity<String> receberESalvar(@RequestBody List<CategoriaDTO> categorias) {
            try {
                categoriaService.salvarCategorias(categorias);
                return ResponseEntity.ok().body("{\"status\": \"sucesso\", \"mensagem\": \"Categorias salvas no arquivo.\"}");
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body("{\"status\": \"erro\", \"mensagem\": \"" + e.getMessage() + "\"}");
            }
        }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> buscarCategorias() {
        try {
            List<CategoriaDTO> categorias = categoriaService.lerCategorias();
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    }





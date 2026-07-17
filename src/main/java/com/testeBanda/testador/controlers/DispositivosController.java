package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.CategoriaDTO;
import com.testeBanda.testador.models.Dispositivos;
import com.testeBanda.testador.repository.DispositivosRepository;
import com.testeBanda.testador.service.CategoriaService;
import com.testeBanda.testador.service.DispositivosService;
import com.testeBanda.testador.service.NagiosSshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class DispositivosController {
    private final CategoriaService categoriaService;
    private final DispositivosRepository dispositivosRepository;
    private final NagiosSshService nagiosSshService;

    @Autowired
    public DispositivosController(CategoriaService categoriaService, DispositivosRepository dispositivosRepository, NagiosSshService nagiosSshService) {
        this.categoriaService = categoriaService;
        this.dispositivosRepository = dispositivosRepository;
        this.nagiosSshService = nagiosSshService;
    }

    @PostMapping
        public ResponseEntity<String> receberESalvar(@RequestBody List<CategoriaDTO> categorias) {
            try {
                categoriaService.salvarCategorias(categorias);
                return ResponseEntity.ok().body("Categorias salvas no arquivo");
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Falha: " + e.getMessage());
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

//    @GetMapping("/dispositivos")
//    public String dispositivos(Model model) {
//        List<Dispositivos> dispositivos = dispositivosRepository.findAll();
//        model.addAttribute("dispositivos", dispositivos);
//        return "dispositivos";
//    }

    @PostMapping("/imprimir-nagios")
    public Dispositivos salvarNagios(@RequestBody Dispositivos dispositivoFront) {
        Dispositivos dispositivo = dispositivosRepository.findById(dispositivoFront.getId())
                .orElse(dispositivoFront);
        System.out.println(dispositivo.toString());
        System.out.println("cidadeNagiosId: " + dispositivo.getCidadeNagiosId());
        try {
            if(dispositivo.getCidadeNagiosId() == null){
                throw new Exception();
            }
            nagiosSshService.cadastrarLinkPrimario(dispositivo.getCidadeNagiosId(), dispositivo.getNome(), dispositivo.getIp());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dispositivo;
    }
    }





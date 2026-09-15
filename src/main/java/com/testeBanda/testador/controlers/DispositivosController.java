package com.testeBanda.testador.controlers;

import com.testeBanda.testador.DTO.CategoriaDTO;
import com.testeBanda.testador.models.Dispositivos;
import com.testeBanda.testador.repository.DispositivosRepository;
import com.testeBanda.testador.service.CategoriaService;
import com.testeBanda.testador.service.NagiosSshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> salvarNagios(@RequestBody Dispositivos dispositivoFront) {
        Dispositivos dispositivo = dispositivosRepository.findById(dispositivoFront.getId())
                .orElse(dispositivoFront);

        if(dispositivo.getIp().endsWith(".1")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Roteadores não podem ser modificados");
        }
            if(dispositivo.getCidadeNagiosId() == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Dispositivo sem cidade cadastrada");
            }
            NagiosSshService.Resultado res = nagiosSshService.cadastrarLinkPrimario(dispositivo.getCidadeNagiosId(), dispositivo.getNome(), dispositivo.getIp());
            if(res.sucesso()){
                //isso aqui é o cumulo, mas funcionou... o OK generico do spring n retorna JSON só plain text.
                throw new ResponseStatusException(HttpStatus.OK, "Dispositivo cadastrado com sucesso");
            }
            else{
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,res.mensagem());
            }
        }
    }






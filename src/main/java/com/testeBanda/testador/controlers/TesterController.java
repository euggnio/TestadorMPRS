package com.testeBanda.testador.controlers;


import com.testeBanda.testador.DTO.TesteBandaDTO;
import com.testeBanda.testador.models.Cidades;
import com.testeBanda.testador.repository.CidadesRepository;
import com.testeBanda.testador.service.CidadeService;
import com.testeBanda.testador.service.TesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TesterController {

    private final TesterService testerService;
    private final CidadeService cidadeService;

    @Autowired
    public TesterController(TesterService testerService, CidadeService cidadeService) {
        this.testerService = testerService;
        this.cidadeService = cidadeService;
    }


    @PostMapping("/testeBanda")
    @ResponseBody
    public String testeBanda(@RequestBody(required = false) String idCidade) {
        if (idCidade == null) {
            idCidade = "";
        }
        testerService.iniciarTeste(idCidade);
        return "{\"status\": \"" + testerService.getTesteStatus() + "\"}"; // Retorna JSON
    }

    @GetMapping("/data")
    public ResponseEntity data(@RequestParam(defaultValue = "") String idCidade) {
        TesteBandaDTO dto = new TesteBandaDTO();
        Cidades c = cidadeService.findById(idCidade.toUpperCase());
        dto.ultimoTesteBanda = c.ultimoTesteBanda;
        dto.dataUltimoTeste = c.dataUltimoTeste;
        dto.check = c.checkTesteBanda;
        return ResponseEntity.ok(dto);
    }


}

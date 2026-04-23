package com.testeBanda.testador.controlers;


import com.testeBanda.testador.api.SnmpWanMonitor;
import com.testeBanda.testador.models.ResultadosSnmp;
import com.testeBanda.testador.service.ScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;


@Controller
public class SnmpController {

    private final SnmpWanMonitor monitor;
    private final ScanService scan;

    @Autowired
    public SnmpController(SnmpWanMonitor monitor, ScanService scan) {
        this.monitor = monitor;
        this.scan = scan;
    }

    @GetMapping("/snmpWan")
    @ResponseBody
    public List<ResultadosSnmp> getSnmpList2(){
        return monitor.resultados;
    }

    @GetMapping("/scan")
    @ResponseBody
    public String getSnmpList32() throws IOException {
        scan.adicionarDispositivos("ALVORADA");
                return "monitor.resultados;";
    }


}

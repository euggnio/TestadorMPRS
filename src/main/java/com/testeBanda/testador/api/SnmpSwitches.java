package com.testeBanda.testador.api;

import com.testeBanda.testador.models.SwitchStatus;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SnmpSwitches {

    private Snmp snmp;

    OID SYS_NAME =      new OID(".1.3.6.1.2.1.1.5.0");
    OID SYS_DESCR =     new OID(".1.3.6.1.2.1.1.1.0");
    OID SYS_LOCATION =  new OID(".1.3.6.1.2.1.1.6.0");
    OID SYS_UPTIME =        new OID(".1.3.6.1.2.1.1.3.0");
    OID IF_NUMBER =     new OID(".1.3.6.1.2.1.2.1.0");

    OID IF_STATUS =     new OID(".1.3.6.1.2.1.2.2.1.8.");
    OID IF_SPEED =     new OID("1.3.6.1.2.1.31.1.1.1.15");
    OID IF_DESCR =     new OID(".1.3.6.1.2.1.2.2.1.2");

    OID IF_IN_OCTETS =     new OID(".1.3.6.1.2.1.2.2.1.10");
    OID IF_IN_ERRORS =     new OID(".1.3.6.1.2.1.2.2.1.14");
    OID IF_IN_DISCARDS =     new OID(".1.3.6.1.2.1.2.2.1.13");

    OID IF_OUT_OCTETS =     new OID(".1.3.6.1.2.1.2.2.1.16");
    OID IF_OUT_ERRORS =     new OID(".1.3.6.1.2.1.2.2.1.20");
    OID IF_OUT_DISCARDS =     new OID(".1.3.6.1.2.1.2.2.1.19");



    private CommunityTarget<Address> createTarget(String ip){
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString("public"));
        target.setAddress(GenericAddress.parse("udp:"+ip+"/161"));
        target.setTimeout(6000);
        target.setRetries(3);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    public void init(){
        TransportMapping<UdpAddress> transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException e) {
            log.error("ERRO ao iniciar o SnmpMonitor : " + e.getMessage());
        }
    }

    public void close() throws IOException {
        snmp.close();
    }

    public SwitchStatus getInfo(String ip){
        SwitchStatus info = new SwitchStatus();

        init();

        try {
            info.setCpu(getCPU(ip)); // pega a CPU antes dos outros SNMP

            info.setName(makeRequest(ip, SYS_NAME).toString());
            info.setDescricao(makeRequest(ip, SYS_DESCR).toString());
            info.setLocation(makeRequest(ip, SYS_LOCATION).toString());
            info.setUptime(makeRequest(ip, SYS_UPTIME).toString());
            info.setIfNumber(makeRequest(ip, IF_NUMBER).toInt());

            List<Variable> portStatusList = makeRequestPorts(ip,IF_STATUS, info.getIfNumber());
            List<Variable> portSpeedList = makeRequestPorts(ip,IF_SPEED, info.getIfNumber());
            List<Variable> portDescrList = makeRequestPorts(ip,IF_DESCR, info.getIfNumber());

            List<Variable> portInOctets = makeRequestPorts(ip,IF_IN_OCTETS, info.getIfNumber());
            List<Variable> portInErrors = makeRequestPorts(ip,IF_IN_ERRORS, info.getIfNumber());
            List<Variable> portInDiscards = makeRequestPorts(ip,IF_IN_DISCARDS, info.getIfNumber());

            List<Variable> portOutOctets = makeRequestPorts(ip,IF_OUT_OCTETS, info.getIfNumber());
            List<Variable> portOutErrors = makeRequestPorts(ip,IF_OUT_ERRORS, info.getIfNumber());
            List<Variable> portOutDiscards = makeRequestPorts(ip,IF_OUT_DISCARDS, info.getIfNumber());

//            System.out.println(portOutOctets);

            for(int i = 0; i < portStatusList.size(); i++){
                int status = portStatusList.get(i) != null ? portStatusList.get(i).toInt() : -1;
                int speed  = portSpeedList.get(i) != null ? portSpeedList.get(i).toInt() : -1;
                String descr = portDescrList.get(i) != null ? portDescrList.get(i).toString() :"[Error]";

                long inOctets    = portInOctets.get(i)   != null ? portInOctets.get(i).toLong()   : -1;
                long inErrors    = portInErrors.get(i)   != null ? portInErrors.get(i).toLong()   : -1;
                long inDiscards  = portInDiscards.get(i) != null ? portInDiscards.get(i).toLong() : -1;

                long outOctets   = portOutOctets.get(i)  != null ? portOutOctets.get(i).toLong()  : -1;
                long outErrors   = portOutErrors.get(i)  != null ? portOutErrors.get(i).toLong()  : -1;
                long outDiscards = portOutDiscards.get(i)!= null ? portOutDiscards.get(i).toLong(): -1;

                SwitchStatus.Port port = new SwitchStatus.Port(i+1, descr, status, speed, inOctets, inErrors, inDiscards, outOctets, outErrors, outDiscards);

                info.interfaces.add(port);
            }

            close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return info;
    }

    private int getCPU(String ip){
        final String cisco_OID = "1.3.6.1.4.1.9.9.109.1.1.1.1.6.1";
        final String dlink_OID = "1.3.6.1.4.1.171.12.1.1.6.1.0";
        final String zyxel_OID = "1.3.6.1.4.1.890.1.15.3.2.4.0";
        final String intelbras_OID = ".1.3.6.1.4.1.26138.2.6.1.1.1.1.6.212";
        final String extremeSummit_OID = "1.3.6.1.4.1.1916.1.1.1.28.0";
        // OID da extreme summit pode estar errado, dispositivos do MP são muito antigos
        // Em testes parece que sempre retorna 0%, mas se mandar muitos SNMP, já vi chegar a 1%
        // Acho que é o melhor que tem

        OID CPU_USAGE = null;
        int result = -1;

        try {
            String descr = makeRequest(ip, SYS_DESCR).toString();

            if(descr.contains("Cisco")){
                CPU_USAGE = new OID(cisco_OID);
            }else if(descr.contains("GS2210-48")){
                CPU_USAGE = new OID(zyxel_OID);
            }else if(descr.contains("DGS") || descr.contains("DES")){
                CPU_USAGE = new OID(dlink_OID);
            }else if(descr.contains("Summit")){
                CPU_USAGE = new OID(extremeSummit_OID);
            }else if(descr.contains("INTELBRAS")){
                CPU_USAGE = new OID(intelbras_OID);
            }

            if(CPU_USAGE != null){
                result = makeRequest(ip, CPU_USAGE).toInt();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private List<Variable> makeRequestPorts(String ip, OID oid_raiz, int ifNumber) throws Exception{
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> events = treeUtils.getSubtree(createTarget(ip), oid_raiz);

        List<Variable> results = new ArrayList<>();

        if (events != null) {
            for (TreeEvent event : events) {
                if (event.isError()) {
                    System.err.println("Error: " + event.getErrorMessage());
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                if (varBindings != null) {
                    for (VariableBinding vb : varBindings) {
                        if(vb.getOid().last() <= ifNumber){
                            results.add(vb.getVariable());
                        }
                        //System.out.println(vb.getOid() + " = " + vb.getVariable() + "  " + vb.getOid().last());
                    }
                }
            }
        }

        return results;
    }

    private Variable makeRequest(String ip, OID oid) throws Exception {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));

        pdu.setType(PDU.GET);
        ResponseEvent<Address> event = snmp.send(pdu,createTarget(ip));

        if(event.getResponse()==null){
            System.out.println("Switch " + ip + " TIMEOUT");

            throw new Exception("error");
        }

        PDU response = event.getResponse();

        //System.out.println(response);
        return response.get(0).getVariable();
    }


}

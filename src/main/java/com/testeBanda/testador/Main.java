package com.testeBanda.testador;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class Main {

    public static void main(String[] args) {
        try {

            String host = "172.17.111.1";
            String community = "moc3pgj"; // ou sua comunidade SNMP
            String oidUptime = "1.3.6.1.2.1.1.3.0"; // system uptime

            // Configuração do alvo (onde fica a RB)
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(new UdpAddress(host + "/161"));
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            transport.listen();
            Snmp snmp = new Snmp(transport);
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oidUptime)));
            pdu.setType(PDU.GET);
            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                VariableBinding vb = response.getResponse().get(0);
                System.out.println(response.getResponse());
            } else {
            }

            snmp.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

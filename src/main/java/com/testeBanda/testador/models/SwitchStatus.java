package com.testeBanda.testador.models;


import lombok.Getter;
import lombok.Setter;
import org.snmp4j.smi.Variable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SwitchStatus {

    public String name;
    public String descricao;
    public String location;
    public String uptime;
    public int cpu;
    //public int mem;

    public int ifNumber;


    public record Port(int index,
                       String descr,
                       int status,
                       int speed,
                       long portInOctets,
                       long portInErrors,
                       long portInDiscards,
                       long portOutOctets,
                       long portOutErrors,
                       long portOutDiscards){}

    public List<Port> interfaces;

    public SwitchStatus() {
        this.interfaces = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "SwitchStatus{" +
                "name='" + name + '\'' +
                ", descricao='" + descricao + '\'' +
                ", location='" + location + '\'' +
                ", uptime='" + uptime + '\'' +
                ", cpu=" + cpu +
                ", ifNumber=" + ifNumber +
                ", interfaces=" + interfaces +
                '}';
    }
}

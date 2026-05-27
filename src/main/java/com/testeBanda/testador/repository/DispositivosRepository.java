package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Dispositivos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispositivosRepository extends JpaRepository<Dispositivos,Long> {


    List<Dispositivos> findByIpIn(List<String> ip);



}

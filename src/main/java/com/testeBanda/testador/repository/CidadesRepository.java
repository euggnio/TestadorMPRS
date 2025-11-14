package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Cidades;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CidadesRepository extends JpaRepository<Cidades,String>  {

    Optional<Cidades> findByNagiosIDEqualsIgnoreCase(String nagiosID);

}

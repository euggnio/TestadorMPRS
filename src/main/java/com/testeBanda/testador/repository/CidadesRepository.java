package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Cidades;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CidadesRepository extends JpaRepository<Cidades,String>  {

    Optional<Cidades> findByNagiosIDEqualsIgnoreCase(String nagiosID);

    @Query("SELECT c FROM Cidades c LEFT JOIN FETCH c.dispositivos WHERE c.nome = :nome")
    Optional<Cidades> findByIdComDispositivos(@Param("nome") String nome);

}

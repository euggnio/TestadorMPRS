package com.testeBanda.testador.repository;

import com.testeBanda.testador.models.Queda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuedaRepository extends JpaRepository<Queda,Long> {

}

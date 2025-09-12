package ru.hh.aiinterviewer.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hh.aiinterviewer.domain.model.RoleSynonym;

public interface RoleSynonymRepository extends JpaRepository<RoleSynonym, UUID> {

  @Query("select s from RoleSynonym s join fetch s.role where s.normalized like concat('%', :q, '%')")
  List<RoleSynonym> searchByNormalizedContains(@Param("q") String q);
}

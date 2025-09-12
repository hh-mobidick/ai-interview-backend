package ru.hh.aiinterviewer.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hh.aiinterviewer.domain.model.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  @Query("select r from Role r where r.normalized like concat('%', :q, '%') order by r.popularity desc")
  List<Role> searchByNormalizedContains(@Param("q") String q);
}

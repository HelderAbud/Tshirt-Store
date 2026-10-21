package com.revenda.repository;

import com.revenda.domain.NfeImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeImportRepository extends JpaRepository<NfeImport, Long> {

  boolean existsByAccessKey(String accessKey);
}

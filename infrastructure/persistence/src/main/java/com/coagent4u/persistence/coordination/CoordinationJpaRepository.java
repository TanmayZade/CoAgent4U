package com.coagent4u.persistence.coordination;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinationJpaRepository extends JpaRepository<CoordinationJpaEntity, UUID> {
}

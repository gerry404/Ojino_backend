package com.schoolcopilot.content.earlyyears.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.content.earlyyears.domain.LearningDomain;

@Repository
public interface LearningDomainRepository extends MongoRepository<LearningDomain, String> {

    List<LearningDomain> findBySystemCodeOrderByDisplayOrderAsc(String systemCode);

    Optional<LearningDomain> findBySystemCodeAndCode(String systemCode, String code);

    long countBySystemCode(String systemCode);
}

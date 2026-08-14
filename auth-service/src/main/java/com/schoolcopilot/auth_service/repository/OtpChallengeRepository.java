package com.schoolcopilot.auth_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.auth_service.domain.OtpChallenge;

@Repository
public interface OtpChallengeRepository extends MongoRepository<OtpChallenge, String> {

    /** Le dernier code demande pour ce numero : sert au delai anti-renvoi. */
    Optional<OtpChallenge> findFirstByPhoneOrderByCreatedAtDesc(String phone);
}

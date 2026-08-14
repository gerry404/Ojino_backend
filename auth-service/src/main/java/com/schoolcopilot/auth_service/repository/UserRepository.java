package com.schoolcopilot.auth_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    /**
     * Retrouve le compte deja rattache a cette identite externe. C'est la requete
     * qui fait qu'un utilisateur qui revient par Google retombe sur son compte au
     * lieu d'en creer un second.
     */
    @Query("{ 'identities': { $elemMatch: { 'provider': ?0, 'subject': ?1 } } }")
    Optional<User> findByIdentity(AuthProvider provider, String subject);
}

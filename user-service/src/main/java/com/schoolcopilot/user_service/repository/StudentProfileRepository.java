package com.schoolcopilot.user_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.user_service.domain.profile.StudentProfile;

@Repository
public interface StudentProfileRepository extends MongoRepository<StudentProfile, String> {

    /**
     * Comptages d'usage, consultes avant toute suppression dans le referentiel :
     * retirer un niveau encore reference laisserait des profils pointant vers un
     * code qui n'existe plus.
     */
    long countBySystemCode(String systemCode);

    long countBySystemCodeAndLevelCode(String systemCode, String levelCode);

    long countBySystemCodeAndTrackCode(String systemCode, String trackCode);

    long countBySystemCodeAndSubjectCodesContaining(String systemCode, String subjectCode);
}

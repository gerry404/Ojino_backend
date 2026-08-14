package com.schoolcopilot.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;

/** Les acces au referentiel scolaire, regroupes pour rester lisibles. */
public final class ReferenceRepositories {

    private ReferenceRepositories() {
    }

    @Repository
    public interface EducationSystems extends MongoRepository<EducationSystem, String> {

        List<EducationSystem> findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Repository
    public interface EducationLevels extends MongoRepository<EducationLevel, String> {

        List<EducationLevel> findBySystemCodeOrderByRankAsc(String systemCode);

        Optional<EducationLevel> findBySystemCodeAndCode(String systemCode, String code);

        long countBySystemCode(String systemCode);
    }

    @Repository
    public interface Tracks extends MongoRepository<Track, String> {

        List<Track> findBySystemCodeOrderByDisplayOrderAsc(String systemCode);

        Optional<Track> findBySystemCodeAndCode(String systemCode, String code);
    }

    @Repository
    public interface Subjects extends MongoRepository<Subject, String> {

        List<Subject> findBySystemCodeOrderByDisplayOrderAsc(String systemCode);
    }
}

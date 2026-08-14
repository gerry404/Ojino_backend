package com.schoolcopilot.content.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.EducationSystem;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;

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

        Optional<Subject> findBySystemCodeAndCode(String systemCode, String code);
    }
}

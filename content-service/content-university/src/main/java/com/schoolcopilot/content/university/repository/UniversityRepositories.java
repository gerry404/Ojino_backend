package com.schoolcopilot.content.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.content.university.domain.CourseUnit;
import com.schoolcopilot.content.university.domain.Program;

/** Les acces au referentiel universitaire, regroupes pour rester lisibles. */
public final class UniversityRepositories {

    private UniversityRepositories() {
    }

    @Repository
    public interface Programs extends MongoRepository<Program, String> {

        List<Program> findBySystemCodeOrderByLabelAsc(String systemCode);

        Optional<Program> findBySystemCodeAndCode(String systemCode, String code);
    }

    @Repository
    public interface CourseUnits extends MongoRepository<CourseUnit, String> {

        List<CourseUnit> findBySystemCodeAndProgramCodeOrderBySemesterAscCodeAsc(
                String systemCode, String programCode);

        Optional<CourseUnit> findBySystemCodeAndCode(String systemCode, String code);
    }
}

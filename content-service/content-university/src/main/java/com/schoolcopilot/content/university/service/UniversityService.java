package com.schoolcopilot.content.university.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.university.domain.CourseUnit;
import com.schoolcopilot.content.university.domain.Program;
import com.schoolcopilot.content.university.repository.UniversityRepositories;

/**
 * Parcours et unites d'enseignement.
 *
 * <p>Meme regle que partout ailleurs dans le referentiel : rien ne se supprime,
 * tout s'archive. Un parcours ferme reste resolvable pour les etudiants qui l'ont
 * suivi.
 */
@Service
public class UniversityService {

    private final UniversityRepositories.Programs programs;
    private final UniversityRepositories.CourseUnits courseUnits;

    public UniversityService(UniversityRepositories.Programs programs,
            UniversityRepositories.CourseUnits courseUnits) {
        this.programs = programs;
        this.courseUnits = courseUnits;
    }

    /** Les parcours ouverts aux inscriptions. */
    public List<Program> programsFor(String systemCode) {
        return programs.findBySystemCodeOrderByLabelAsc(systemCode).stream()
                .filter(program -> !program.archived())
                .toList();
    }

    public Program requireSelectableProgram(String systemCode, String programCode) {
        Program program = programs.findBySystemCodeAndCode(systemCode, programCode)
                .orElseThrow(() -> ApiException.unknownProgram(programCode));
        if (program.archived()) {
            throw ApiException.archived("Le parcours", programCode);
        }
        return program;
    }

    /**
     * Les unites d'enseignement d'un parcours, filtrables par semestre.
     *
     * @param semester facultatif ; refuse s'il sort de la duree du parcours
     */
    public List<CourseUnit> courseUnitsFor(String systemCode, String programCode,
            Integer semester) {
        Program program = requireSelectableProgram(systemCode, programCode);
        if (semester != null && !program.hasSemester(semester)) {
            throw ApiException.unknownSemester(semester, program.semesterCount());
        }

        return courseUnits
                .findBySystemCodeAndProgramCodeOrderBySemesterAscCodeAsc(systemCode, programCode)
                .stream()
                .filter(unit -> !unit.archived())
                .filter(unit -> semester == null || unit.semester() == semester)
                .toList();
    }

    /** Total des credits d'un semestre — trente dans le systeme ECTS. */
    public int creditsOf(String systemCode, String programCode, int semester) {
        return courseUnitsFor(systemCode, programCode, semester).stream()
                .mapToInt(CourseUnit::credits)
                .sum();
    }
}

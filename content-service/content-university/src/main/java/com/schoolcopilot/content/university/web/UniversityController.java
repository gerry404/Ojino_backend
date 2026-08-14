package com.schoolcopilot.content.university.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.university.domain.CourseUnit;
import com.schoolcopilot.content.university.domain.Program;
import com.schoolcopilot.content.university.service.UniversityService;

/**
 * Le referentiel de l'enseignement superieur.
 *
 * <p>Sous son propre prefixe, avec son propre vocabulaire : ni niveau, ni
 * filiere, ni matiere, mais parcours, semestres et unites d'enseignement.
 */
@RestController
@RequestMapping("/api/v1/reference/university")
public class UniversityController {

    private final UniversityService university;

    public UniversityController(UniversityService university) {
        this.university = university;
    }

    public record ProgramView(String code, String label, String degree, String faculty,
            int semesterCount) {

        static ProgramView from(Program program) {
            return new ProgramView(program.code(), program.label(), program.degree(),
                    program.faculty(), program.semesterCount());
        }
    }

    public record CourseUnitView(String code, String label, int semester, int credits,
            boolean mandatory) {

        static CourseUnitView from(CourseUnit unit) {
            return new CourseUnitView(unit.code(), unit.label(), unit.semester(), unit.credits(),
                    unit.mandatory());
        }
    }

    @GetMapping("/systems/{systemCode}/programs")
    public List<ProgramView> programs(@PathVariable String systemCode) {
        return university.programsFor(systemCode).stream().map(ProgramView::from).toList();
    }

    @GetMapping("/systems/{systemCode}/programs/{programCode}")
    public ProgramView program(@PathVariable String systemCode,
            @PathVariable String programCode) {
        return ProgramView.from(university.requireSelectableProgram(systemCode, programCode));
    }

    /** @param semester facultatif ; sans lui, toutes les UE du parcours */
    @GetMapping("/systems/{systemCode}/programs/{programCode}/units")
    public List<CourseUnitView> units(@PathVariable String systemCode,
            @PathVariable String programCode,
            @RequestParam(required = false) Integer semester) {
        return university.courseUnitsFor(systemCode, programCode, semester).stream()
                .map(CourseUnitView::from)
                .toList();
    }
}

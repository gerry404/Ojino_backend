package com.schoolcopilot.content.university.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un parcours universitaire : licence d'informatique, master de droit prive...
 *
 * <p>Remplace le couple niveau/filiere du secondaire, qui n'a pas de sens ici :
 * un etudiant ne dit pas "je suis en Terminale D" mais "je suis en L2 informatique".
 *
 * @param degree LICENCE, MASTER, DOCTORAT
 * @param semesterCount duree totale en semestres — six pour une licence
 */
@Document(collection = "university_programs")
@CompoundIndex(name = "idx_program_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record Program(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        String degree,
        String faculty,
        int semesterCount,
        boolean archived) {

    public boolean hasSemester(int semester) {
        return semester >= 1 && semester <= semesterCount;
    }
}

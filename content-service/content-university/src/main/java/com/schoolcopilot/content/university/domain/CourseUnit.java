package com.schoolcopilot.content.university.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une unite d'enseignement, rattachee a un parcours et a un semestre.
 *
 * <p>{@code credits} porte les credits ECTS. C'est la difference de fond avec une
 * matiere du secondaire : une UE se valide et se capitalise, elle ne se "suit"
 * pas simplement.
 *
 * @param mandatory obligatoire dans le parcours, par opposition a une UE au choix
 */
@Document(collection = "university_course_units")
@CompoundIndex(name = "idx_unit_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record CourseUnit(
        @Id String id,
        @Indexed String systemCode,
        @Indexed String programCode,
        String code,
        String label,
        int semester,
        int credits,
        boolean mandatory,
        boolean archived) {
}

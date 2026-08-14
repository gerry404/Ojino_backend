package com.schoolcopilot.content.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un chapitre du programme.
 *
 * <p>Le decoupage chapitre puis notion est le meme dans tous les cycles ; ce qui
 * change, c'est ce a quoi le chapitre se rattache. {@code anchorCode} designe une
 * matiere au college, au lycee et en prepa, un domaine d'apprentissage en
 * maternelle, une unite d'enseignement a l'universite. Le coeur ne sait pas
 * l'interpreter : il demande au module du cycle si le code existe.
 *
 * @param trackCode restreint le chapitre a une filiere. Null vaut "toutes les
 *        filieres de ce niveau" — le cas courant, car la plupart des chapitres du
 *        tronc commun ne dependent pas de la filiere.
 */
@Document(collection = "chapters")
@CompoundIndex(name = "idx_chapter_system_code", def = "{'systemCode': 1, 'code': 1}",
        unique = true)
@CompoundIndex(name = "idx_chapter_lookup",
        def = "{'systemCode': 1, 'levelCode': 1, 'anchorCode': 1, 'rank': 1}")
public record Chapter(
        @Id String id,
        @Indexed String systemCode,
        EducationCycle cycle,
        String levelCode,
        String anchorCode,
        String trackCode,
        String code,
        String label,
        String summary,
        int rank,
        PublicationStatus status,
        boolean archived) {

    /** Ce que voit une application : publie et non archive. */
    public boolean isVisible() {
        return status == PublicationStatus.PUBLISHED && !archived;
    }

    /** Un chapitre sans filiere vaut pour toutes. */
    public boolean appliesToTrack(String track) {
        return trackCode == null || trackCode.equals(track);
    }
}

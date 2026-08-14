package com.schoolcopilot.content.core.spi;

import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;

/**
 * Base commune aux cycles dont les chapitres se rattachent a une matiere :
 * college, lycee, prepa.
 *
 * <p>Ces trois-la partagent tout leur vocabulaire, porte par le coeur. Ecrire
 * trois fois la meme verification aurait ete de la duplication, pas de
 * l'isolation : le module reste libre de tout redefinir s'il diverge un jour.
 */
public abstract class SubjectAnchoredModule implements CurriculumModule {

    private final ReferenceRepositories.Subjects subjects;

    protected SubjectAnchoredModule(ReferenceRepositories.Subjects subjects) {
        this.subjects = subjects;
    }

    @Override
    public AnchorKind anchorKind() {
        return AnchorKind.SUBJECT;
    }

    /** Une matiere archivee ne peut plus porter de nouveau chapitre. */
    @Override
    public boolean anchorExists(String systemCode, String anchorCode) {
        return subjects.findBySystemCodeAndCode(systemCode, anchorCode)
                .filter(subject -> !subject.archived())
                .isPresent();
    }

    protected java.util.Optional<Subject> subject(String systemCode, String code) {
        return subjects.findBySystemCodeAndCode(systemCode, code);
    }
}

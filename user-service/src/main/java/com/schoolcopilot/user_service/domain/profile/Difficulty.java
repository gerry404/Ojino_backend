package com.schoolcopilot.user_service.domain.profile;

/**
 * Une matiere ou l'eleve se dit en difficulte.
 *
 * @param subjectCode matiere concernee
 * @param severity de 1 (un peu juste) a 3 (vraiment bloque)
 * @param note precision libre, par exemple "je ne comprends pas les limites"
 */
public record Difficulty(String subjectCode, int severity, String note) {

    public static final int MIN_SEVERITY = 1;
    public static final int MAX_SEVERITY = 3;

    public boolean isValid() {
        return subjectCode != null && !subjectCode.isBlank()
                && severity >= MIN_SEVERITY && severity <= MAX_SEVERITY;
    }
}

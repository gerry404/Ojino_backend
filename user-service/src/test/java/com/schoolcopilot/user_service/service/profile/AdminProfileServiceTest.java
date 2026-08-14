package com.schoolcopilot.user_service.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

/**
 * Verifie l'assemblage des criteres de recherche : c'est la seule logique du
 * service, le reste etant delegue a Mongo.
 */
class AdminProfileServiceTest {

    private final Pageable pageable = PageRequest.of(0, 20);

    private MongoTemplate mongoTemplate;
    private AdminProfileService adminProfiles;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        StudentProfileRepository profiles = mock(StudentProfileRepository.class);

        when(mongoTemplate.count(any(Query.class), eq(StudentProfile.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(StudentProfile.class)))
                .thenReturn(List.of());

        adminProfiles = new AdminProfileService(mongoTemplate, profiles);
    }

    @Test
    @DisplayName("sans filtre, la requete ne restreint rien")
    void noFilterMeansNoCriteria() {
        adminProfiles.search(null, null, null, pageable);

        assertThat(capturedQuery().getQueryObject()).isEmpty();
    }

    @Test
    @DisplayName("un filtre vide est ignore comme un filtre absent")
    void blankFiltersAreIgnored() {
        adminProfiles.search("   ", "", "  ", pageable);

        assertThat(capturedQuery().getQueryObject()).isEmpty();
    }

    @Test
    @DisplayName("les filtres fournis se combinent tous")
    void filtersAreCombined() {
        adminProfiles.search("Paul", "CM-FR", "TLE", pageable);

        String query = capturedQuery().getQueryObject().toJson();
        assertThat(query).contains("firstName", "lastName", "systemCode", "levelCode");
    }

    @Test
    @DisplayName("le terme recherche est echappe avant d'entrer dans l'expression reguliere")
    void searchTermIsEscaped() {
        // Sans echappement, ce terme serait une expression reguliere valide et
        // couteuse au lieu d'une chaine cherchee litteralement.
        adminProfiles.search("(a+)+$", null, null, pageable);

        assertThat(capturedQuery().getQueryObject().toJson()).contains("\\\\Q(a+)+$\\\\E");
    }

    @Test
    @DisplayName("le total est compte avant la pagination")
    void totalIsCountedBeforePaging() {
        when(mongoTemplate.count(any(Query.class), eq(StudentProfile.class))).thenReturn(137L);

        Page<StudentProfile> page = adminProfiles.search(null, "CM-FR", null, pageable);

        // Compte apres pagination, le total vaudrait la taille de la page.
        assertThat(page.getTotalElements()).isEqualTo(137L);
        assertThat(page.getSize()).isEqualTo(20);
    }

    private Query capturedQuery() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(query.capture(), eq(StudentProfile.class));
        return query.getValue();
    }
}

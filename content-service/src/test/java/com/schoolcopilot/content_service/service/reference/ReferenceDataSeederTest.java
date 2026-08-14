package com.schoolcopilot.content_service.service.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.schoolcopilot.content_service.config.ReferenceProperties;
import com.schoolcopilot.content_service.domain.reference.EducationLevel;
import com.schoolcopilot.content_service.domain.reference.EducationSystem;
import com.schoolcopilot.content_service.domain.reference.Subject;
import com.schoolcopilot.content_service.domain.reference.Track;
import com.schoolcopilot.content_service.repository.ReferenceRepositories;

import tools.jackson.databind.json.JsonMapper;

/**
 * Verifie le fichier de referentiel livre avec l'application. Une coquille dedans
 * casserait le demarrage en production : autant la trouver ici.
 */
class ReferenceDataSeederTest {

    private ReferenceRepositories.EducationSystems systems;
    private ReferenceRepositories.EducationLevels levels;
    private ReferenceRepositories.Tracks tracks;
    private ReferenceRepositories.Subjects subjects;
    private ReferenceDataSeeder seeder;

    @BeforeEach
    void setUp() {
        systems = mock(ReferenceRepositories.EducationSystems.class);
        levels = mock(ReferenceRepositories.EducationLevels.class);
        tracks = mock(ReferenceRepositories.Tracks.class);
        subjects = mock(ReferenceRepositories.Subjects.class);

        seeder = new ReferenceDataSeeder(systems, levels, tracks, subjects,
                new ReferenceProperties(true, "CM-FR"), JsonMapper.builder().build());
    }

    @Test
    @DisplayName("le fichier livre se lit et charge les deux systemes camerounais")
    @SuppressWarnings("unchecked")
    void seedsBundledSystems() throws IOException {
        when(systems.existsById(anyString())).thenReturn(false);

        seeder.run(null);

        ArgumentCaptor<EducationSystem> saved = ArgumentCaptor.forClass(EducationSystem.class);
        verify(systems, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(EducationSystem::code)
                .containsExactly("CM-FR", "CM-EN");

        ArgumentCaptor<List<EducationLevel>> savedLevels = ArgumentCaptor.forClass(List.class);
        verify(levels, org.mockito.Mockito.times(2)).saveAll(savedLevels.capture());
        assertThat(savedLevels.getAllValues().get(0)).extracting(EducationLevel::code)
                .containsExactly("6E", "5E", "4E", "3E", "2NDE", "1ERE", "TLE");

        ArgumentCaptor<List<Track>> savedTracks = ArgumentCaptor.forClass(List.class);
        verify(tracks, org.mockito.Mockito.times(2)).saveAll(savedTracks.capture());
        assertThat(savedTracks.getAllValues().get(0)).extracting(Track::code)
                .contains("A4", "C", "D", "E", "TI");

        ArgumentCaptor<List<Subject>> savedSubjects = ArgumentCaptor.forClass(List.class);
        verify(subjects, org.mockito.Mockito.times(2)).saveAll(savedSubjects.capture());
        assertThat(savedSubjects.getAllValues().get(0)).extracting(Subject::code)
                .contains("MATH", "FRAN", "PHILO");
    }

    @Test
    @DisplayName("un systeme deja present n'est jamais ecrase")
    void existingSystemsAreLeftAlone() throws IOException {
        when(systems.existsById(anyString())).thenReturn(true);

        seeder.run(null);

        // Les corrections faites en base, ou les pays ajoutes en production, doivent
        // survivre a un redeploiement.
        verify(systems, never()).save(org.mockito.ArgumentMatchers.any());
        verify(levels, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("le chargement peut etre coupe par configuration")
    void seedingCanBeDisabled() throws IOException {
        ReferenceDataSeeder disabled = new ReferenceDataSeeder(systems, levels, tracks, subjects,
                new ReferenceProperties(false, "CM-FR"), JsonMapper.builder().build());

        disabled.run(null);

        verify(systems, never()).existsById(anyString());
    }
}

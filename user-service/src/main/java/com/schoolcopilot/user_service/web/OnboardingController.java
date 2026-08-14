package com.schoolcopilot.user_service.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.service.profile.OnboardingService;
import com.schoolcopilot.user_service.service.profile.ProfileService;
import com.schoolcopilot.user_service.web.dto.AvailabilityRequest;
import com.schoolcopilot.user_service.web.dto.DifficultiesRequest;
import com.schoolcopilot.user_service.web.dto.GoalRequest;
import com.schoolcopilot.user_service.web.dto.IdentityRequest;
import com.schoolcopilot.user_service.web.dto.LevelRequest;
import com.schoolcopilot.user_service.web.dto.OnboardingStateResponse;
import com.schoolcopilot.user_service.web.dto.PhotoRequest;
import com.schoolcopilot.user_service.web.dto.ProfileResponse;
import com.schoolcopilot.user_service.web.dto.SubjectsRequest;
import com.schoolcopilot.user_service.web.dto.TrackRequest;

import jakarta.validation.Valid;

/**
 * Le parcours de creation de profil.
 *
 * <p>Chaque etape s'enregistre seule et renvoie l'etat complet du parcours, avec
 * l'etape suivante. L'application n'a donc aucune sequence a coder : elle suit ce
 * que le serveur lui indique, et le parcours peut evoluer sans mise a jour des
 * applications mobiles.
 *
 * <p>L'identifiant du profil est le {@code sub} de l'access token : personne ne
 * peut modifier le profil d'un autre, il n'y a pas d'identifiant a passer.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final ProfileService profiles;
    private final OnboardingService onboarding;

    public OnboardingController(ProfileService profiles, OnboardingService onboarding) {
        this.profiles = profiles;
        this.onboarding = onboarding;
    }

    /** Ou en est l'eleve. Appele a l'ouverture de l'application pour reprendre au bon endroit. */
    @GetMapping
    public OnboardingStateResponse state(@AuthenticationPrincipal Jwt jwt) {
        return respond(profiles.getOrCreate(jwt.getSubject()));
    }

    @PutMapping("/identity")
    public OnboardingStateResponse identity(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody IdentityRequest request) {
        return respond(profiles.updateIdentity(jwt.getSubject(), request.firstName(),
                request.lastName(), request.birthDate()));
    }

    @PutMapping("/photo")
    public OnboardingStateResponse photo(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhotoRequest request) {
        return respond(profiles.updatePhoto(jwt.getSubject(), request.avatarUrl()));
    }

    /** La photo n'est jamais un obstacle a l'entree dans l'application. */
    @PostMapping("/photo/skip")
    public OnboardingStateResponse skipPhoto(@AuthenticationPrincipal Jwt jwt) {
        return respond(profiles.skipPhoto(jwt.getSubject()));
    }

    @PutMapping("/level")
    public OnboardingStateResponse level(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LevelRequest request) {
        return respond(profiles.updateLevel(jwt.getSubject(), request.systemCode(),
                request.levelCode()));
    }

    @PutMapping("/track")
    public OnboardingStateResponse track(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TrackRequest request) {
        return respond(profiles.updateTrack(jwt.getSubject(), request.trackCode()));
    }

    @PutMapping("/subjects")
    public OnboardingStateResponse subjects(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubjectsRequest request) {
        return respond(profiles.updateSubjects(jwt.getSubject(), request.subjectCodes()));
    }

    @PutMapping("/goal")
    public OnboardingStateResponse goal(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GoalRequest request) {
        return respond(profiles.updateGoal(jwt.getSubject(), request.goal(), request.targetExam(),
                request.note()));
    }

    @PutMapping("/difficulties")
    public OnboardingStateResponse difficulties(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DifficultiesRequest request) {
        return respond(profiles.updateDifficulties(jwt.getSubject(), request.toDomain()));
    }

    @PutMapping("/availability")
    public OnboardingStateResponse availability(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AvailabilityRequest request) {
        return respond(profiles.updateAvailability(jwt.getSubject(), request.toDomain()));
    }

    private OnboardingStateResponse respond(StudentProfile profile) {
        return OnboardingStateResponse.of(onboarding.stateOf(profile),
                ProfileResponse.from(profile));
    }
}

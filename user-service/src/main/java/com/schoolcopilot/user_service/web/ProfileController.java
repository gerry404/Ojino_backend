package com.schoolcopilot.user_service.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.user_service.service.profile.ProfileService;
import com.schoolcopilot.user_service.web.dto.ProfileResponse;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    /**
     * Le profil de l'utilisateur connecte.
     *
     * <p>Renvoie un profil vide plutot qu'une erreur si l'inscription n'a pas
     * encore commence : c'est un etat normal juste apres la creation du compte,
     * pas une anomalie.
     */
    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profiles.getOrCreate(jwt.getSubject()));
    }
}

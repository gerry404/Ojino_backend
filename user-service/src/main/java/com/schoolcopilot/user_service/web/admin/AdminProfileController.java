package com.schoolcopilot.user_service.web.admin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.user_service.service.profile.AdminProfileService;
import com.schoolcopilot.user_service.web.admin.dto.AdminProfileView;
import com.schoolcopilot.user_service.web.admin.dto.PageResponse;

/**
 * Consultation des profils.
 *
 * <p>Volontairement en lecture seule : corriger un niveau ou une matiere se fait
 * depuis l'application, par l'eleve lui-meme.
 */
@RestController
@RequestMapping("/api/v1/admin/profiles")
public class AdminProfileController {

    private final AdminProfileService adminProfiles;

    public AdminProfileController(AdminProfileService adminProfiles) {
        this.adminProfiles = adminProfiles;
    }

    /**
     * @param q terme cherche dans le prenom et le nom
     * @param systemCode filtre par systeme scolaire
     * @param levelCode filtre par classe
     */
    @GetMapping
    public PageResponse<AdminProfileView> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String levelCode,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return PageResponse.of(adminProfiles.search(q, systemCode, levelCode, pageable),
                AdminProfileView::from);
    }

    @GetMapping("/{userId}")
    public AdminProfileView detail(@PathVariable String userId) {
        return AdminProfileView.from(adminProfiles.get(userId));
    }
}

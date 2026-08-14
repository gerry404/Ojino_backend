package com.schoolcopilot.auth_service.web.admin;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.service.AdminUserService;
import com.schoolcopilot.auth_service.web.admin.dto.AdminUserResponse;
import com.schoolcopilot.auth_service.web.admin.dto.PageResponse;
import com.schoolcopilot.auth_service.web.admin.dto.SessionResponse;
import com.schoolcopilot.auth_service.web.admin.dto.UpdateRolesRequest;

import jakarta.validation.Valid;

/**
 * Back-office des comptes.
 *
 * <p>Tout ce qui vit sous {@code /api/v1/admin} exige {@code ROLE_ADMIN}, impose
 * par la chaine de filtres et non par une annotation posee ici : la protection ne
 * depend donc pas du fait qu'on pense a l'ecrire sur chaque nouvelle methode.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUsers;

    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = adminUsers;
    }

    /**
     * @param q terme libre cherche dans l'email, le telephone et le nom affiche
     */
    @GetMapping
    public PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<User> page = adminUsers.search(q, pageable);
        return PageResponse.of(page, AdminUserResponse::from);
    }

    @GetMapping("/{userId}")
    public AdminUserResponse detail(@PathVariable String userId) {
        return AdminUserResponse.from(adminUsers.get(userId));
    }

    /** Desactive le compte et coupe immediatement toutes ses sessions. */
    @PostMapping("/{userId}/disable")
    public AdminUserResponse disable(@PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        return AdminUserResponse.from(adminUsers.disable(userId, jwt.getSubject()));
    }

    @PostMapping("/{userId}/enable")
    public AdminUserResponse enable(@PathVariable String userId) {
        return AdminUserResponse.from(adminUsers.enable(userId));
    }

    @PutMapping("/{userId}/roles")
    public AdminUserResponse updateRoles(@PathVariable String userId,
            @Valid @RequestBody UpdateRolesRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return AdminUserResponse.from(
                adminUsers.updateRoles(userId, request.roles(), jwt.getSubject()));
    }

    /** Les appareils sur lesquels ce compte est connecte. */
    @GetMapping("/{userId}/sessions")
    public List<SessionResponse> sessions(@PathVariable String userId) {
        return adminUsers.activeSessions(userId).stream().map(SessionResponse::from).toList();
    }

    @DeleteMapping("/{userId}/sessions")
    public Map<String, String> revokeSessions(@PathVariable String userId) {
        adminUsers.revokeSessions(userId);
        return Map.of("status", "sessions_revoked");
    }
}

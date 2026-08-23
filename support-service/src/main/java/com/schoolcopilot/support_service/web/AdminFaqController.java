package com.schoolcopilot.support_service.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.support_service.service.FaqService;
import com.schoolcopilot.support_service.web.dto.FaqAdminView;
import com.schoolcopilot.support_service.web.dto.FaqEntryUpsertRequest;

/**
 * Back-office de la FAQ : voit tout, brouillons et archives compris.
 *
 * <p>Le {@code @PreAuthorize} est une ceinture, pas la protection principale.
 * La vraie barriere est dans {@code SecurityConfig}, qui ferme
 * {@code /api/v1/admin/**} en bloc avant toute autre regle. Les deux ensemble :
 * si quelqu'un ajoute demain une route en oubliant l'annotation, le chemin reste
 * ferme.
 */
@RestController
@RequestMapping("/api/v1/admin/faq")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFaqController {

    private final FaqService faqService;

    public AdminFaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping
    public List<FaqAdminView> list() {
        return faqService.listAll().stream()
                .map(FaqAdminView::from)
                .toList();
    }

    @GetMapping("/{code}")
    public FaqAdminView get(@PathVariable String code) {
        return FaqAdminView.from(faqService.getByCode(code));
    }

    @PostMapping
    public FaqAdminView create(@Valid @RequestBody FaqEntryUpsertRequest request) {
        return FaqAdminView.from(faqService.create(request));
    }

    /** Remplace le contenu. Le {@code code} du corps est ignore par le service. */
    @PutMapping("/{code}")
    public FaqAdminView update(@PathVariable String code,
            @Valid @RequestBody FaqEntryUpsertRequest request) {
        return FaqAdminView.from(faqService.update(code, request));
    }

    /**
     * Publier n'est pas modifier l'entree, c'est changer son etat : d'ou un
     * {@code PATCH} sur une sous-ressource explicite plutot qu'un {@code PUT}
     * portant un champ {@code status}, qui obligerait le client a connaitre la
     * machine a etats.
     */
    @PatchMapping("/{code}/publish")
    public FaqAdminView publish(@PathVariable String code) {
        return FaqAdminView.from(faqService.publish(code));
    }

    @PatchMapping("/{code}/unpublish")
    public FaqAdminView unpublish(@PathVariable String code) {
        return FaqAdminView.from(faqService.unpublish(code));
    }

    @PatchMapping("/{code}/archive")
    public FaqAdminView archive(@PathVariable String code) {
        return FaqAdminView.from(faqService.archive(code));
    }

    @PatchMapping("/{code}/restore")
    public FaqAdminView restore(@PathVariable String code) {
        return FaqAdminView.from(faqService.restore(code));
    }
}

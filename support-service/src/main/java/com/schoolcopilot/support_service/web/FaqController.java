package com.schoolcopilot.support_service.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.support_service.service.FaqService;
import com.schoolcopilot.support_service.web.dto.FaqEntryView;

/**
 * Parcours utilisateur : lecture seule, et uniquement ce qui est publie.
 *
 * <p>Aucun {@code if (isAdmin)} ici. Le back-office vit dans une autre classe,
 * derriere un autre prefixe d'URL et une autre regle de securite.
 */
@RestController
@RequestMapping("/api/v1/faq")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    /** La categorie est facultative : son absence signifie « tout ». */
    @GetMapping
    public List<FaqEntryView> list(@RequestParam(required = false) String category) {
        return faqService.listVisible(category).stream()
                .map(FaqEntryView::from)
                .toList();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return faqService.listCategories();
    }
}

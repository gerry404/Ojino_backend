package com.schoolcopilot.user_service.service.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

/**
 * Consultation des profils par l'equipe.
 *
 * <p>En lecture seule : un administrateur n'a pas a modifier le profil scolaire
 * de quelqu'un a sa place. Corriger un niveau ou une matiere se fait depuis
 * l'application, par l'interesse.
 */
@Service
public class AdminProfileService {

    private final MongoTemplate mongoTemplate;
    private final StudentProfileRepository profiles;

    public AdminProfileService(MongoTemplate mongoTemplate, StudentProfileRepository profiles) {
        this.mongoTemplate = mongoTemplate;
        this.profiles = profiles;
    }

    /**
     * Recherche a filtres facultatifs, combines entre eux.
     *
     * <p>Ecrite avec {@code MongoTemplate} plutot qu'avec des methodes derivees :
     * trois filtres optionnels donneraient huit methodes de repository a ecrire et
     * a maintenir, pour un seul assemblage de criteres ici.
     */
    public Page<StudentProfile> search(String term, String systemCode, String levelCode,
            Pageable pageable) {

        List<Criteria> filters = new ArrayList<>();

        if (isPresent(term)) {
            // Le terme part dans une expression reguliere : sans echappement, un
            // point ou une etoile changerait le sens de la recherche, et une
            // expression bien choisie pourrait bloquer le serveur.
            String escaped = Pattern.quote(term.trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("firstName").regex(escaped, "i"),
                    Criteria.where("lastName").regex(escaped, "i")));
        }
        if (isPresent(systemCode)) {
            filters.add(Criteria.where("systemCode").is(systemCode.trim()));
        }
        if (isPresent(levelCode)) {
            filters.add(Criteria.where("levelCode").is(levelCode.trim()));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters));
        }

        // Le total se compte avant la pagination, sinon il vaudrait la taille de la
        // page au lieu du nombre de resultats.
        long total = mongoTemplate.count(query, StudentProfile.class);
        List<StudentProfile> content =
                mongoTemplate.find(query.with(pageable), StudentProfile.class);

        return new PageImpl<>(content, pageable, total);
    }

    public StudentProfile get(String userId) {
        return profiles.findById(userId).orElseThrow(ApiException::profileNotFound);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}

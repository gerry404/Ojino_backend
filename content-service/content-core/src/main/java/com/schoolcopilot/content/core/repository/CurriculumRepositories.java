package com.schoolcopilot.content.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.content.core.domain.Chapter;

/** Les acces au programme, regroupes pour rester lisibles. */
public final class CurriculumRepositories {

    private CurriculumRepositories() {
    }

    @Repository
    public interface Chapters extends MongoRepository<Chapter, String> {

        List<Chapter> findBySystemCodeAndLevelCodeOrderByRankAsc(String systemCode,
                String levelCode);

        List<Chapter> findBySystemCodeAndLevelCodeAndAnchorCodeOrderByRankAsc(String systemCode,
                String levelCode, String anchorCode);

        Optional<Chapter> findBySystemCodeAndCode(String systemCode, String code);

        long countBySystemCodeAndAnchorCode(String systemCode, String anchorCode);
    }
}

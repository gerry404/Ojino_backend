package com.schoolcopilot.content.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.content.core.domain.Chapter;
import com.schoolcopilot.content.core.domain.Difficulty;
import com.schoolcopilot.content.core.domain.Exercise;
import com.schoolcopilot.content.core.domain.LearningResource;
import com.schoolcopilot.content.core.domain.Notion;

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

    @Repository
    public interface Notions extends MongoRepository<Notion, String> {

        List<Notion> findBySystemCodeAndChapterCodeOrderByRankAsc(String systemCode,
                String chapterCode);

        Optional<Notion> findBySystemCodeAndCode(String systemCode, String code);

        /** Tout le systeme : necessaire pour raisonner sur le graphe des prerequis. */
        List<Notion> findBySystemCode(String systemCode);

        /** Les notions que celle-ci debloque. */
        List<Notion> findBySystemCodeAndPrerequisiteCodesContaining(String systemCode,
                String prerequisiteCode);
    }

    @Repository
    public interface LearningResources extends MongoRepository<LearningResource, String> {

        List<LearningResource> findBySystemCodeAndNotionCodeOrderByRankAsc(String systemCode,
                String notionCode);

        Optional<LearningResource> findBySystemCodeAndCode(String systemCode, String code);
    }

    @Repository
    public interface Exercises extends MongoRepository<Exercise, String> {

        List<Exercise> findBySystemCodeAndNotionCodeOrderByDifficultyAscCodeAsc(String systemCode,
                String notionCode);

        List<Exercise> findBySystemCodeAndNotionCodeAndDifficultyOrderByCodeAsc(String systemCode,
                String notionCode, Difficulty difficulty);

        Optional<Exercise> findBySystemCodeAndCode(String systemCode, String code);
    }
}

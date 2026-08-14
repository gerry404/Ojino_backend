package com.schoolcopilot.learning_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.MasteryLevel;
import com.schoolcopilot.learning_service.domain.NotionMastery;

public final class LearningRepositories {

    private LearningRepositories() {
    }

    @Repository
    public interface Events extends MongoRepository<LearningEvent, String> {

        /** Tous les evenements d'un eleve sur une notion : la base du recalcul. */
        List<LearningEvent> findByUserIdAndSystemCodeAndNotionCode(String userId,
                String systemCode, String notionCode);

        List<LearningEvent> findTop50ByUserIdOrderByOccurredAtDesc(String userId);
    }

    @Repository
    public interface Mastery extends MongoRepository<NotionMastery, String> {

        List<NotionMastery> findByUserIdAndSystemCode(String userId, String systemCode);

        Optional<NotionMastery> findByUserIdAndSystemCodeAndNotionCode(String userId,
                String systemCode, String notionCode);

        List<NotionMastery> findByUserIdAndSystemCodeAndLevelIn(String userId, String systemCode,
                List<MasteryLevel> levels);
    }
}

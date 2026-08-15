package com.schoolcopilot.engagement_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.engagement_service.domain.EngagementProfile;
import com.schoolcopilot.engagement_service.domain.MoodCheckIn;
import com.schoolcopilot.engagement_service.domain.Streak;

public final class EngagementRepositories {

    private EngagementRepositories() {
    }

    @Repository
    public interface Streaks extends MongoRepository<Streak, String> {

        /** Les series encore vivantes, pour la relance quotidienne. */
        List<Streak> findByCurrentGreaterThanAndLastActiveOnBefore(int minimum, LocalDate before);
    }

    @Repository
    public interface Profiles extends MongoRepository<EngagementProfile, String> {
    }

    @Repository
    public interface CheckIns extends MongoRepository<MoodCheckIn, String> {

        List<MoodCheckIn> findByUserIdAndDayGreaterThanEqualOrderByDayDesc(String userId,
                LocalDate from);

        List<MoodCheckIn> findByUserIdOrderByDayDesc(String userId);
    }
}

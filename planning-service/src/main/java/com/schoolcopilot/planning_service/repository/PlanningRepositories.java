package com.schoolcopilot.planning_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.SessionStatus;
import com.schoolcopilot.planning_service.domain.StudySession;

public final class PlanningRepositories {

    private PlanningRepositories() {
    }

    @Repository
    public interface Deadlines extends MongoRepository<Deadline, String> {

        List<Deadline> findByUserIdOrderByDueOnAsc(String userId);

        List<Deadline> findByUserIdAndCompletedFalseAndDueOnGreaterThanEqualOrderByDueOnAsc(
                String userId, LocalDate from);
    }

    @Repository
    public interface Sessions extends MongoRepository<StudySession, String> {

        List<StudySession> findByUserIdAndScheduledOnBetweenOrderByScheduledOnAscStartTimeAsc(
                String userId, LocalDate from, LocalDate to);

        List<StudySession> findByUserIdAndStatusAndScheduledOnLessThan(String userId,
                SessionStatus status, LocalDate date);

        List<StudySession> findByUserIdAndScheduledOnGreaterThanEqualAndStatus(String userId,
                LocalDate from, SessionStatus status);
    }
}

package com.schoolcopilot.notification_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.notification_service.domain.Notification;
import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.domain.NotificationStatus;

public final class NotificationRepositories {

    private NotificationRepositories() {
    }

    @Repository
    public interface Notifications extends MongoRepository<Notification, String> {

        /** La file : ce qui est du, du plus ancien au plus recent. */
        List<Notification> findByStatusAndSendAfterLessThanEqualOrderBySendAfterAsc(
                NotificationStatus status, Instant now, Pageable pageable);

        List<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

        List<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                String userId, NotificationChannel channel);

        long countByUserIdAndChannelAndReadAtIsNull(String userId, NotificationChannel channel);

        /** Sert au plafond quotidien : seuls les canaux intrusifs comptent. */
        long countByUserIdAndStatusAndChannelNotAndSentAtAfter(String userId,
                NotificationStatus status, NotificationChannel channel, Instant since);

        boolean existsByDedupeKey(String dedupeKey);
    }

    @Repository
    public interface Preferences extends MongoRepository<NotificationPreferences, String> {
    }
}

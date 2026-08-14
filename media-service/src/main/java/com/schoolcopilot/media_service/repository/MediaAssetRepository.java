package com.schoolcopilot.media_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.media_service.domain.MediaAsset;
import com.schoolcopilot.media_service.domain.MediaPurpose;
import com.schoolcopilot.media_service.domain.MediaStatus;

@Repository
public interface MediaAssetRepository extends MongoRepository<MediaAsset, String> {

    List<MediaAsset> findByOwnerIdAndPurposeAndStatus(String ownerId, MediaPurpose purpose,
            MediaStatus status);

    List<MediaAsset> findByOwnerIdAndStatusOrderByCreatedAtDesc(String ownerId,
            MediaStatus status);

    /** Les envois abandonnes, a nettoyer. */
    List<MediaAsset> findByStatusAndCreatedAtBefore(MediaStatus status, Instant threshold);
}

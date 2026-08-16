package com.schoolcopilot.assistant_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.assistant_service.domain.Conversation;
import com.schoolcopilot.assistant_service.domain.Message;
import com.schoolcopilot.assistant_service.domain.UsageQuota;

public final class AssistantRepositories {

    private AssistantRepositories() {
    }

    @Repository
    public interface Conversations extends MongoRepository<Conversation, String> {

        List<Conversation> findByUserIdAndArchivedAtIsNullOrderByUpdatedAtDesc(String userId);
    }

    @Repository
    public interface Messages extends MongoRepository<Message, String> {

        List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

        /** Les derniers messages, pour la fenetre de contexte. */
        List<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId,
                Pageable pageable);

        void deleteByConversationId(String conversationId);
    }

    @Repository
    public interface Quotas extends MongoRepository<UsageQuota, String> {
    }
}

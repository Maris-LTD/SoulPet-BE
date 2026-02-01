package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find all chat messages by reading ID, ordered by creation time
     * @param readingId reading ID
     * @return List of ChatMessages
     */
    List<ChatMessage> findByReadingIdOrderByCreatedAtAsc(UUID readingId);

    /**
     * Count chat messages by reading ID
     * @param readingId reading ID
     * @return count of messages
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.reading.id = :readingId")
    long countByReadingId(@Param("readingId") UUID readingId);
}

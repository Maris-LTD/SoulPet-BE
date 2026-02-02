package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.Reading;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Reading entity
 */
@Repository
public interface ReadingRepository extends JpaRepository<Reading, UUID> {

    /**
     * Find all readings by user ID with pagination
     * @param userId user ID
     * @param pageable pagination parameters
     * @return Page of Readings
     */
    Page<Reading> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all readings by user ID and status
     * @param userId user ID
     * @param status reading status
     * @return List of Readings
     */
    List<Reading> findByUserIdAndStatus(UUID userId, ReadingStatus status);

    /**
     * Count weekly readings by user ID (from start of current week)
     * Weekly reset logic - tính từ đầu tuần (Monday 00:00)
     * @param userId user ID
     * @param status reading status (typically ACTIVE)
     * @param weekStart start of current week
     * @return count of readings
     */
    @Query("SELECT COUNT(r) FROM Reading r WHERE r.user.id = :userId AND r.status = :status AND r.createdAt >= :weekStart")
    long countWeeklyReadingsByUserId(@Param("userId") UUID userId, 
                                     @Param("status") ReadingStatus status,
                                     @Param("weekStart") LocalDateTime weekStart);

    /**
     * Find reading by ID and user ID (for security)
     * @param id reading ID
     * @param userId user ID
     * @return Optional Reading
     */
    Optional<Reading> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Count readings by user and status
     * @param userId user ID
     * @param status reading status
     * @return count of readings
     */
    long countByUserIdAndStatus(UUID userId, ReadingStatus status);
}

package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.TarotCard;
import com.mystictarot.backend.entity.enums.SuitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TarotCard entity (master data)
 */
@Repository
public interface TarotCardRepository extends JpaRepository<TarotCard, Integer> {

    /**
     * Find tarot card by name
     * @param name card name
     * @return Optional TarotCard
     */
    Optional<TarotCard> findByName(String name);

    /**
     * Find all tarot cards by suit
     * @param suit suit type
     * @return List of TarotCards
     */
    List<TarotCard> findBySuit(SuitType suit);

    /**
     * Find all tarot cards ordered by card number
     * @return List of TarotCards
     */
    List<TarotCard> findAllByOrderByCardNumberAsc();
}

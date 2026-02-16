package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.TarotCard;
import com.mystictarot.backend.entity.enums.SuitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarotCardRepository extends JpaRepository<TarotCard, Integer> {

    List<TarotCard> findBySuit(SuitType suit);

    List<TarotCard> findAllByOrderByCardNumberAsc();
}

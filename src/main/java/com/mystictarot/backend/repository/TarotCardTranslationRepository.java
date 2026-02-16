package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.TarotCardTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarotCardTranslationRepository extends JpaRepository<TarotCardTranslation, Long> {

    Optional<TarotCardTranslation> findByTarotCard_IdAndLocale(Integer cardId, String locale);

    @Query("SELECT t FROM TarotCardTranslation t JOIN FETCH t.tarotCard WHERE t.locale = :locale ORDER BY t.tarotCard.cardNumber ASC")
    List<TarotCardTranslation> findAllByLocaleOrderByTarotCard_CardNumberAsc(@Param("locale") String locale);

    List<TarotCardTranslation> findByTarotCard_IdInAndLocale(List<Integer> cardIds, String locale);
}

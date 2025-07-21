package org.example.userserv.repository;

import org.example.userserv.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    @Query("select card from Card card where card.id in :ids")
    List<Card> findByIds(@Param("ids") List<Long> ids);

    boolean existsByNumber(String number);

    List<Card> findByUserId(UUID id);
}

package org.example.userserv.repository;

import org.example.userserv.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    @Query(value = "select * from cards where id = :id", nativeQuery = true)
    Optional<Card> findById(@Param("id") Long id);

    @Query("select card from Card card where card.id in :ids")
    List<Card> findByIds(@Param("ids") List<Long> ids);

    boolean existsByNumber(String number);

    List<Card> findByUserId(Long id);
}

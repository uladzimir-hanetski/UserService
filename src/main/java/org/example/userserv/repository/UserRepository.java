package org.example.userserv.repository;

import org.example.userserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "select * from users where id = :id", nativeQuery = true)
    Optional<User> findById(@Param("id") Long id);

    @Query(value = "select * from users where email = :email", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select u from User u where u.id in :ids")
    List<User> findByIds(@Param("ids") List<Long> ids);

    boolean existsByEmail(String email);
}

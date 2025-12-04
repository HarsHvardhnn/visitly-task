package com.assignments.rbac.repository;

import com.assignments.rbac.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r WHERE u.email = :email AND u.isDeleted = false AND (r.isDeleted = false OR r.isDeleted IS NULL)")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}


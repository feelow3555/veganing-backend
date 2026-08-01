package com.veganing.domain.auth.repository;

import com.veganing.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository = PostgreSQL 접근

public interface UserRepository extends JpaRepository<User, Long> {
    // optional 인 이유는 없을때 null 처리 하려고
    Optional<User> findByEmail(String email); // -> SELECT * FROM users WHERE email = ?
    boolean existsByEmail(String email); // -> SELECT EXISTS (SELECT * FROM users WHERE email = ?)
}

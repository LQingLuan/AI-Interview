package com.university.smartinterview.repository;

import com.university.smartinterview.entity.SmartInterviewUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmartInterviewUserRepository extends JpaRepository<SmartInterviewUser, Long> {
    Optional<SmartInterviewUser> findByUsername(String username);
    Optional<SmartInterviewUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    
}
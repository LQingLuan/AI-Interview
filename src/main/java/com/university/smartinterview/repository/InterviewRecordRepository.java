package com.university.smartinterview.repository;

import com.university.smartinterview.entity.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {

    Optional<InterviewRecord> findByInterviewId(String interviewId);

    List<InterviewRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    List<InterviewRecord> findByStatus(InterviewRecord.InterviewStatus status);

    List<InterviewRecord> findByCareerDirectionAndDifficultyLevel(String careerDirection, Integer difficultyLevel);

    @Query("SELECT ir FROM InterviewRecord ir WHERE ir.createdAt >= :startDate AND ir.createdAt <= :endDate")
    List<InterviewRecord> findBetweenDates(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(ir) FROM InterviewRecord ir WHERE ir.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    @Query("SELECT AVG(CAST(SUBSTRING_INDEX(ir.overallScore, '/', 1) AS double)) FROM InterviewRecord ir WHERE ir.userId = :userId AND ir.overallScore IS NOT NULL")
    Double calculateAverageScoreByUserId(@Param("userId") String userId);
}
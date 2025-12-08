package com.university.smartinterview.repository;

import com.university.smartinterview.entity.FeedbackDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackDimensionRepository extends JpaRepository<FeedbackDimension, Long> {

    List<FeedbackDimension> findByInterviewId(String interviewId);

    @Modifying
    @Query("DELETE FROM FeedbackDimension fd WHERE fd.interviewId = :interviewId")
    void deleteByInterviewId(@Param("interviewId") String interviewId);

    @Query("SELECT fd.dimensionName, AVG(fd.score) as avgScore FROM FeedbackDimension fd WHERE fd.interviewId IN " +
            "(SELECT ir.interviewId FROM InterviewRecord ir WHERE ir.userId = :userId) " +
            "GROUP BY fd.dimensionName")
    List<Object[]> findAverageScoresByUserId(@Param("userId") String userId);
}
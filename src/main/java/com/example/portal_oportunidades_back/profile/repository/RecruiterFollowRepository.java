package com.example.portal_oportunidades_back.profile.repository;

import com.example.portal_oportunidades_back.profile.entity.RecruiterFollow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruiterFollowRepository extends JpaRepository<RecruiterFollow, UUID> {
    boolean existsByStudentIdAndRecruiterId(Long studentId, Long recruiterId);

    @Modifying
    @Query(value = """
            INSERT INTO perfil.recruiter_follow (id, student_id, recruiter_id, created_at)
            VALUES (:id, :studentId, :recruiterId, CURRENT_TIMESTAMP)
            ON CONFLICT (student_id, recruiter_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("studentId") Long studentId,
                       @Param("recruiterId") Long recruiterId);

    @Modifying
    @Query("delete from RecruiterFollow f where f.student.id = :studentId and f.recruiter.id = :recruiterId")
    int deleteFollow(@Param("studentId") Long studentId, @Param("recruiterId") Long recruiterId);
}

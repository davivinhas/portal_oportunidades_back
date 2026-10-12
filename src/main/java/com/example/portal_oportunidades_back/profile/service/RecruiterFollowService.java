package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.profile.dto.FollowingResponse;
import com.example.portal_oportunidades_back.profile.repository.RecruiterFollowRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import com.example.portal_oportunidades_back.profile.repository.StudentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterFollowService {
    private final StudentRepository students;
    private final RecruiterRepository recruiters;
    private final RecruiterFollowRepository follows;

    public RecruiterFollowService(StudentRepository students, RecruiterRepository recruiters,
                                 RecruiterFollowRepository follows) {
        this.students = students;
        this.recruiters = recruiters;
        this.follows = follows;
    }

    @Transactional
    public void follow(Long studentId, Long recruiterId) {
        validateProfiles(studentId, recruiterId);
        follows.insertIfAbsent(UUID.randomUUID(), studentId, recruiterId);
    }

    @Transactional
    public void unfollow(Long studentId, Long recruiterId) {
        validateProfiles(studentId, recruiterId);
        follows.deleteFollow(studentId, recruiterId);
    }

    @Transactional(readOnly = true)
    public FollowingResponse getFollowing(Long studentId, Long recruiterId) {
        validateProfiles(studentId, recruiterId);
        return new FollowingResponse(follows.existsByStudentIdAndRecruiterId(studentId, recruiterId));
    }

    private void validateProfiles(Long studentId, Long recruiterId) {
        if (!students.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        if (!recruiters.existsById(recruiterId)) {
            throw new ResourceNotFoundException("Recruiter not found: " + recruiterId);
        }
    }
}

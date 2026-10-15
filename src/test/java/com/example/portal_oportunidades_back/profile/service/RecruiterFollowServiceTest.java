package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.profile.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RecruiterFollowServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final RecruiterRepository recruiters = mock(RecruiterRepository.class);
    private final RecruiterFollowRepository follows = mock(RecruiterFollowRepository.class);
    private final RecruiterFollowService service = new RecruiterFollowService(students, recruiters, follows);

    @BeforeEach
    void setUp() {
        when(students.existsById(1L)).thenReturn(true);
        when(recruiters.existsById(2L)).thenReturn(true);
    }

    @Test
    void repeatedFollowUsesConflictSafeInsertion() {
        when(follows.insertIfAbsent(any(), eq(1L), eq(2L))).thenReturn(1, 0);
        service.follow(1L, 2L);
        service.follow(1L, 2L);
        verify(follows, times(2)).insertIfAbsent(any(), eq(1L), eq(2L));
    }

    @Test
    void repeatedUnfollowSucceedsEvenWithoutAnExistingLink() {
        when(follows.deleteFollow(1L, 2L)).thenReturn(1, 0);
        service.unfollow(1L, 2L);
        service.unfollow(1L, 2L);
        verify(follows, times(2)).deleteFollow(1L, 2L);
    }

    @Test
    void reportsBothFollowingStates() {
        when(follows.existsByStudentIdAndRecruiterId(1L, 2L)).thenReturn(false, true);
        assertThat(service.getFollowing(1L, 2L).following()).isFalse();
        assertThat(service.getFollowing(1L, 2L).following()).isTrue();
    }

    @Test
    void rejectsMissingStudentForAllOperations() {
        assertThatThrownBy(() -> service.follow(99L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.unfollow(99L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getFollowing(99L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(follows);
    }

    @Test
    void rejectsMissingRecruiterForAllOperations() {
        assertThatThrownBy(() -> service.follow(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.unfollow(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getFollowing(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(follows);
    }
}

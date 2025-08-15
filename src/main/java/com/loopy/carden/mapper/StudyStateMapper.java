package com.loopy.carden.mapper;

import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import com.loopy.carden.entity.StudyState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for StudyState entity and DTOs
 */
@Component
public class StudyStateMapper {

    /**
     * Convert StudyState entity to response DTO
     */
    public StudyStateResponseDto toResponseDto(StudyState studyState) {
        if (studyState == null) {
            return null;
        }

        StudyStateResponseDto dto = new StudyStateResponseDto();
        dto.setId(studyState.getId());
        dto.setCardId(studyState.getCard().getId());
        dto.setUserId(studyState.getUser().getId());
        dto.setDeckId(studyState.getDeck().getId());
        dto.setRepetitionCount(studyState.getRepetitionCount());
        dto.setEaseFactor(studyState.getEaseFactor());
        dto.setIntervalDays(studyState.getIntervalDays());
        dto.setDueDate(studyState.getDueDate());
        dto.setCardState(studyState.getCardState());
        dto.setLastReviewDate(studyState.getLastReviewDate());
        dto.setLastScore(studyState.getLastScore());
        dto.setTotalReviews(studyState.getTotalReviews());
        dto.setCorrectReviews(studyState.getCorrectReviews());
        dto.setAccuracyRate(studyState.getAccuracyRate());
        dto.setConsecutiveFailures(studyState.getConsecutiveFailures());
        dto.setCurrentLearningStep(studyState.getCurrentLearningStep());
        dto.setIsLeech(studyState.getIsLeech());
        dto.setGraduatedAt(studyState.getGraduatedAt());
        dto.setCreatedAt(studyState.getCreatedAt());
        dto.setUpdatedAt(studyState.getUpdatedAt());

        // Add computed fields
        dto.setIsDue(studyState.isDue());
        dto.setIsNew(studyState.isNew());
        dto.setIsLearning(studyState.isLearning());
        dto.setIsInReview(studyState.isInReview());

        return dto;
    }

    /**
     * Convert list of StudyState entities to response DTOs
     */
    public List<StudyStateResponseDto> toResponseDtoList(List<StudyState> studyStates) {
        if (studyStates == null) {
            return null;
        }

        return studyStates.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert StudyState entity to simplified response DTO (for practice sessions)
     */
    public StudyStateResponseDto.SimplifiedDto toSimplifiedDto(StudyState studyState) {
        if (studyState == null) {
            return null;
        }

        StudyStateResponseDto.SimplifiedDto dto = new StudyStateResponseDto.SimplifiedDto();
        dto.setId(studyState.getId());
        dto.setCardId(studyState.getCard().getId());
        dto.setCardState(studyState.getCardState());
        dto.setDueDate(studyState.getDueDate());
        dto.setIntervalDays(studyState.getIntervalDays());
        dto.setAccuracyRate(studyState.getAccuracyRate());
        dto.setTotalReviews(studyState.getTotalReviews());
        dto.setIsDue(studyState.isDue());
        dto.setIsNew(studyState.isNew());
        dto.setIsLearning(studyState.isLearning());

        return dto;
    }

    /**
     * Convert list of StudyState entities to simplified response DTOs
     */
    public List<StudyStateResponseDto.SimplifiedDto> toSimplifiedDtoList(List<StudyState> studyStates) {
        if (studyStates == null) {
            return null;
        }

        return studyStates.stream()
                .map(this::toSimplifiedDto)
                .collect(Collectors.toList());
    }
}

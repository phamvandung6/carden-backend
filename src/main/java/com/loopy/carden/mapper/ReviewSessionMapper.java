package com.loopy.carden.mapper;

import com.loopy.carden.dto.session.ReviewSessionCreateDto;
import com.loopy.carden.dto.session.ReviewSessionResponseDto;
import com.loopy.carden.entity.ReviewSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ReviewSession entity and DTOs
 */
@Component
public class ReviewSessionMapper {

    /**
     * Convert ReviewSession entity to response DTO
     */
    public ReviewSessionResponseDto toResponseDto(ReviewSession session) {
        if (session == null) {
            return null;
        }

        ReviewSessionResponseDto dto = new ReviewSessionResponseDto();
        dto.setId(session.getId());
        dto.setUserId(session.getUser().getId());
        
        // Include deck info if present
        if (session.getDeck() != null) {
            dto.setDeckId(session.getDeck().getId());
            dto.setDeckTitle(session.getDeck().getTitle());
        }
        
        dto.setSessionDate(session.getSessionDate());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setCardsStudied(session.getCardsStudied());
        dto.setCardsCorrect(session.getCardsCorrect());
        dto.setNewCards(session.getNewCards());
        dto.setReviewCards(session.getReviewCards());
        dto.setRelearningCards(session.getRelearningCards());
        dto.setAccuracyRate(session.getAccuracyRate());
        dto.setStudyMode(session.getStudyMode());
        dto.setSessionStatus(session.getSessionStatus());
        dto.setSessionStats(session.getSessionStats());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());

        // Add computed fields
        dto.setIsCompleted(session.isCompleted());
        dto.setIsInProgress(session.isInProgress());

        return dto;
    }

    /**
     * Convert list of ReviewSession entities to response DTOs
     */
    public List<ReviewSessionResponseDto> toResponseDtoList(List<ReviewSession> sessions) {
        if (sessions == null) {
            return null;
        }

        return sessions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert ReviewSession entity to summary DTO (for statistics)
     */
    public ReviewSessionResponseDto.SummaryDto toSummaryDto(ReviewSession session) {
        if (session == null) {
            return null;
        }

        ReviewSessionResponseDto.SummaryDto dto = new ReviewSessionResponseDto.SummaryDto();
        dto.setId(session.getId());
        dto.setSessionDate(session.getSessionDate());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setCardsStudied(session.getCardsStudied());
        dto.setAccuracyRate(session.getAccuracyRate());
        dto.setStudyMode(session.getStudyMode());
        dto.setSessionStatus(session.getSessionStatus());

        if (session.getDeck() != null) {
            dto.setDeckTitle(session.getDeck().getTitle());
        }

        return dto;
    }

    /**
     * Convert list of ReviewSession entities to summary DTOs
     */
    public List<ReviewSessionResponseDto.SummaryDto> toSummaryDtoList(List<ReviewSession> sessions) {
        if (sessions == null) {
            return null;
        }

        return sessions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Map create DTO fields to entity (partial mapping)
     * Note: Entity creation is handled by service layer, this is for field mapping only
     */
    public void mapCreateDtoToEntity(ReviewSessionCreateDto createDto, ReviewSession session) {
        if (createDto == null || session == null) {
            return;
        }

        session.setStudyMode(createDto.getStudyMode());
        
        // Other fields like user, deck, sessionDate are set by service layer
        // based on authentication context and business logic
    }
}

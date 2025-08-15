package com.loopy.carden.mapper;

import com.loopy.carden.dto.practice.PracticeCardDto;
import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.StudyState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for practice-related DTOs
 */
@Component
@RequiredArgsConstructor
public class PracticeMapper {

    private final StudyStateMapper studyStateMapper;

    /**
     * Convert StudyState to PracticeCardDto
     */
    public PracticeCardDto toPracticeCardDto(StudyState studyState, Long targetDeckId) {
        if (studyState == null) {
            return null;
        }

        PracticeCardDto dto = new PracticeCardDto();
        
        // Card information
        dto.setCardId(studyState.getCard().getId());
        dto.setFrontText(studyState.getCard().getFront());
        dto.setFrontImageUrl(studyState.getCard().getImageUrl());
        dto.setBackDefinition(studyState.getCard().getBack());
        dto.setBackMeaningVi(studyState.getCard().getBack()); // Use back for Vietnamese meaning
        dto.setIpa(studyState.getCard().getIpaPronunciation());
        
        // Study state information
        dto.setStudyStateId(studyState.getId());
        dto.setCardState(studyState.getCardState());
        dto.setDueDate(studyState.getDueDate());
        dto.setIntervalDays(studyState.getIntervalDays());
        dto.setTotalReviews(studyState.getTotalReviews());
        dto.setAccuracyRate(studyState.getAccuracyRate());
        dto.setIsDue(studyState.isDue());
        dto.setIsNew(studyState.isNew());
        dto.setIsLearning(studyState.isLearning());
        
        // Deck information
        dto.setDeckId(studyState.getDeck().getId());
        dto.setDeckTitle(studyState.getDeck().getTitle());
        
        // Practice metadata (would need additional queries for exact counts)
        dto.setShowAnswer(false);
        dto.setRemainingNewCards(0); // TODO: Implement if needed
        dto.setRemainingReviewCards(0); // TODO: Implement if needed
        dto.setRemainingLearningCards(0); // TODO: Implement if needed
        
        return dto;
    }

    /**
     * Copy base properties from Card to any PracticeCardDto subclass
     */
    public void copyBasePracticeCardProperties(Card card, PracticeCardDto dto) {
        dto.setCardId(card.getId());
        dto.setFrontText(card.getFront());
        dto.setFrontImageUrl(card.getImageUrl());
        dto.setBackDefinition(card.getBack());
        dto.setBackMeaningVi(card.getBack());
        dto.setIpa(card.getIpaPronunciation());
        dto.setDeckId(card.getDeck().getId());
        dto.setDeckTitle(card.getDeck().getTitle());
        dto.setShowAnswer(false);
        dto.setRemainingNewCards(0);
        dto.setRemainingReviewCards(0);
        dto.setRemainingLearningCards(0);
    }

    /**
     * Convert StudyState to simplified DTO
     */
    public StudyStateResponseDto.SimplifiedDto toSimplifiedStudyStateDto(StudyState studyState) {
        return studyStateMapper.toSimplifiedDto(studyState);
    }
}

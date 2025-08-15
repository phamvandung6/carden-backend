package com.loopy.carden.service;

import com.loopy.carden.entity.Card;
import com.loopy.carden.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating intelligent distractors for multiple choice questions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistractorGenerationService {

    private final CardRepository cardRepository;
    private final Random random = new Random();

    /**
     * Generate 3 intelligent distractors for a given card
     */
    public List<String> generateDistractors(Card targetCard, int count) {
        if (count <= 0) return Collections.emptyList();
        
        Set<String> distractors = new HashSet<>();
        String correctAnswer = targetCard.getBack().toLowerCase().trim();
        
        // 1. Try synonyms from the card itself
        addSynonymsAsDistractors(targetCard, distractors, correctAnswer);
        
        // 2. Try cards from the same deck
        addDeckBasedDistractors(targetCard, distractors, correctAnswer);
        
        // 3. Try cards with similar difficulty
        addDifficultyBasedDistractors(targetCard, distractors, correctAnswer);
        
        // 4. Try cards with similar tags
        addTagBasedDistractors(targetCard, distractors, correctAnswer);
        
        // 5. Generate morphological variations if needed
        addMorphologicalDistractors(targetCard, distractors, correctAnswer);
        
        // 6. Add generic wrong answers if still not enough
        addGenericDistractors(targetCard, distractors, correctAnswer);
        
        List<String> result = new ArrayList<>(distractors);
        Collections.shuffle(result, random);
        
        return result.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Add synonyms from the card as distractors
     */
    private void addSynonymsAsDistractors(Card card, Set<String> distractors, String correctAnswer) {
        if (card.getSynonyms() != null) {
            for (Object synonym : card.getSynonyms()) {
                String syn = synonym.toString().trim();
                if (!syn.toLowerCase().equals(correctAnswer) && !syn.isEmpty()) {
                    distractors.add(syn);
                }
            }
        }
    }

    /**
     * Add cards from the same deck as distractors
     */
    private void addDeckBasedDistractors(Card card, Set<String> distractors, String correctAnswer) {
        List<Card> deckCards = cardRepository.findRandomCardsByDeckExcluding(
                card.getDeck().getId(), card.getId(), 10);
        
        for (Card deckCard : deckCards) {
            String back = deckCard.getBack().trim();
            if (!back.toLowerCase().equals(correctAnswer) && !back.isEmpty()) {
                distractors.add(back);
                if (distractors.size() >= 6) break; // Don't take too many from deck
            }
        }
    }

    /**
     * Add cards with similar difficulty as distractors
     */
    private void addDifficultyBasedDistractors(Card card, Set<String> distractors, String correctAnswer) {
        List<Card> similarCards = cardRepository.findRandomCardsByDifficultyExcluding(
                card.getDifficulty().name(), card.getId(), 8);
        
        for (Card similarCard : similarCards) {
            String back = similarCard.getBack().trim();
            if (!back.toLowerCase().equals(correctAnswer) && !back.isEmpty()) {
                distractors.add(back);
                if (distractors.size() >= 8) break;
            }
        }
    }

    /**
     * Add cards with similar tags as distractors
     */
    private void addTagBasedDistractors(Card card, Set<String> distractors, String correctAnswer) {
        if (card.getTags() != null && !card.getTags().isEmpty()) {
            // Get a random tag from the card
            List<Object> tags = new ArrayList<>(card.getTags());
            String randomTag = tags.get(random.nextInt(tags.size())).toString();
            
            String tagJson = "[\"" + randomTag + "\"]";
            List<Card> taggedCards = cardRepository.findRandomCardsByTagExcluding(
                    tagJson, card.getId(), 5);
            
            for (Card taggedCard : taggedCards) {
                String back = taggedCard.getBack().trim();
                if (!back.toLowerCase().equals(correctAnswer) && !back.isEmpty()) {
                    distractors.add(back);
                    if (distractors.size() >= 10) break;
                }
            }
        }
    }

    /**
     * Add morphological variations of the correct answer
     */
    private void addMorphologicalDistractors(Card card, Set<String> distractors, String correctAnswer) {
        String back = card.getBack().trim();
        
        // Add variations with different articles (for languages that use them)
        addArticleVariations(back, distractors, correctAnswer);
        
        // Add variations with different verb forms
        addVerbFormVariations(back, distractors, correctAnswer);
        
        // Add variations with different plurals
        addPluralVariations(back, distractors, correctAnswer);
    }

    /**
     * Add variations with different articles
     */
    private void addArticleVariations(String answer, Set<String> distractors, String correctAnswer) {
        String[] articles = {"a ", "an ", "the ", "một ", "các ", "những "};
        
        for (String article : articles) {
            String withArticle = article + answer;
            String withoutArticle = answer.replaceAll("^(a|an|the|một|các|những)\\s+", "");
            
            if (!withArticle.toLowerCase().equals(correctAnswer)) {
                distractors.add(withArticle);
            }
            if (!withoutArticle.toLowerCase().equals(correctAnswer) && !withoutArticle.equals(answer)) {
                distractors.add(withoutArticle);
            }
        }
    }

    /**
     * Add variations with different verb forms
     */
    private void addVerbFormVariations(String answer, Set<String> distractors, String correctAnswer) {
        // Simple English verb form variations
        if (answer.endsWith("ing")) {
            String base = answer.substring(0, answer.length() - 3);
            distractors.add(base);
            distractors.add(base + "ed");
            distractors.add(base + "s");
        } else if (answer.endsWith("ed")) {
            String base = answer.substring(0, answer.length() - 2);
            distractors.add(base);
            distractors.add(base + "ing");
            distractors.add(base + "s");
        }
        
        // Remove any that match the correct answer
        distractors.removeIf(d -> d.toLowerCase().equals(correctAnswer));
    }

    /**
     * Add variations with different plurals
     */
    private void addPluralVariations(String answer, Set<String> distractors, String correctAnswer) {
        if (answer.endsWith("s") && answer.length() > 2) {
            String singular = answer.substring(0, answer.length() - 1);
            if (!singular.toLowerCase().equals(correctAnswer)) {
                distractors.add(singular);
            }
        } else {
            String plural = answer + "s";
            if (!plural.toLowerCase().equals(correctAnswer)) {
                distractors.add(plural);
            }
        }
    }

    /**
     * Add generic wrong answers as last resort
     */
    private void addGenericDistractors(Card card, Set<String> distractors, String correctAnswer) {
        String[] genericAnswers = {
                "Not applicable", "Unknown", "None of the above", "Other",
                "Không áp dụng", "Không rõ", "Khác", "Không có đáp án nào đúng"
        };
        
        for (String generic : genericAnswers) {
            if (!generic.toLowerCase().equals(correctAnswer)) {
                distractors.add(generic);
                if (distractors.size() >= 15) break; // Enough options now
            }
        }
    }
}

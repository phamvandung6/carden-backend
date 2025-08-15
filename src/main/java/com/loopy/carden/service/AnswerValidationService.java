package com.loopy.carden.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 * Service for validating user answers against correct answers using fuzzy matching
 */
@Service
@Slf4j
public class AnswerValidationService {

    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.85;
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /**
     * Validate user answer against correct answer using fuzzy matching
     */
    public AnswerValidationResult validateAnswer(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return AnswerValidationResult.incorrect(0.0, "Invalid input");
        }

        String normalizedUser = normalizeText(userAnswer);
        String normalizedCorrect = normalizeText(correctAnswer);

        // Exact match
        if (normalizedUser.equals(normalizedCorrect)) {
            return AnswerValidationResult.correct(1.0, "Exact match");
        }

        // Check multiple possible answers (split by comma, semicolon, or "or")
        List<String> possibleAnswers = splitPossibleAnswers(correctAnswer);
        
        for (String answer : possibleAnswers) {
            String normalizedAnswer = normalizeText(answer);
            
            // Try exact match with each possible answer
            if (normalizedUser.equals(normalizedAnswer)) {
                return AnswerValidationResult.correct(1.0, "Exact match with alternative");
            }
            
            // Try fuzzy matching
            AnswerValidationResult fuzzyResult = performFuzzyMatching(normalizedUser, normalizedAnswer);
            if (fuzzyResult.isCorrect()) {
                return fuzzyResult;
            }
        }

        // If no match found, calculate best similarity score for feedback
        double bestSimilarity = possibleAnswers.stream()
                .mapToDouble(answer -> calculateSimilarity(normalizedUser, normalizeText(answer)))
                .max()
                .orElse(0.0);

        return AnswerValidationResult.incorrect(bestSimilarity, 
                bestSimilarity > 0.5 ? "Close, but not quite right" : "Incorrect answer");
    }

    /**
     * Normalize text for comparison
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        
        // Remove diacritics and normalize unicode
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        
        // Convert to lowercase, trim, and remove extra spaces
        normalized = normalized.toLowerCase().trim().replaceAll("\\s+", " ");
        
        // Remove common punctuation
        normalized = normalized.replaceAll("[.,!?;:\"'()\\[\\]{}]", "");
        
        return normalized;
    }

    /**
     * Split possible answers from a string containing multiple options
     */
    private List<String> splitPossibleAnswers(String correctAnswer) {
        // Split by common separators
        String[] parts = correctAnswer.split("[,;]|\\b(or|hoặc|hay)\\b");
        
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Perform fuzzy matching between user and correct answer
     */
    private AnswerValidationResult performFuzzyMatching(String userAnswer, String correctAnswer) {
        double similarity = calculateSimilarity(userAnswer, correctAnswer);
        
        if (similarity >= MIN_SIMILARITY_THRESHOLD) {
            return AnswerValidationResult.correct(similarity, "Close match accepted");
        }
        
        // Check for word-level matching (important for multi-word answers)
        double wordSimilarity = calculateWordLevelSimilarity(userAnswer, correctAnswer);
        if (wordSimilarity >= MIN_SIMILARITY_THRESHOLD) {
            return AnswerValidationResult.correct(wordSimilarity, "Word-level match accepted");
        }
        
        return AnswerValidationResult.incorrect(similarity, "Not close enough");
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1.isEmpty() && str2.isEmpty()) return 1.0;
        if (str1.isEmpty() || str2.isEmpty()) return 0.0;
        
        int distance = levenshteinDistance.apply(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Calculate word-level similarity for multi-word answers
     */
    private double calculateWordLevelSimilarity(String str1, String str2) {
        String[] words1 = str1.split("\\s+");
        String[] words2 = str2.split("\\s+");
        
        if (words1.length != words2.length) {
            return 0.0; // Different number of words
        }
        
        double totalSimilarity = 0.0;
        for (int i = 0; i < words1.length; i++) {
            totalSimilarity += calculateSimilarity(words1[i], words2[i]);
        }
        
        return totalSimilarity / words1.length;
    }

    /**
     * Result of answer validation
     */
    public static class AnswerValidationResult {
        private final boolean correct;
        private final double similarity;
        private final String feedback;

        private AnswerValidationResult(boolean correct, double similarity, String feedback) {
            this.correct = correct;
            this.similarity = similarity;
            this.feedback = feedback;
        }

        public static AnswerValidationResult correct(double similarity, String feedback) {
            return new AnswerValidationResult(true, similarity, feedback);
        }

        public static AnswerValidationResult incorrect(double similarity, String feedback) {
            return new AnswerValidationResult(false, similarity, feedback);
        }

        public boolean isCorrect() {
            return correct;
        }

        public double getSimilarity() {
            return similarity;
        }

        public String getFeedback() {
            return feedback;
        }

        public int getGrade() {
            if (!correct) return 0; // Again
            
            if (similarity >= 0.95) return 3; // Easy (exact or very close)
            if (similarity >= 0.90) return 2; // Good (close match)
            return 1; // Hard (fuzzy match accepted but not great)
        }
    }
}


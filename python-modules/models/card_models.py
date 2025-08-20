"""
Pydantic models for card generation matching database schema
"""
from typing import List, Optional
from pydantic import BaseModel, Field
from enum import Enum

class Difficulty(str, Enum):
    EASY = "EASY"
    NORMAL = "NORMAL" 
    HARD = "HARD"

class CEFRLevel(str, Enum):
    A1 = "A1"
    A2 = "A2"
    B1 = "B1"
    B2 = "B2" 
    C1 = "C1"
    C2 = "C2"

class CardGenerationRequest(BaseModel):
    """Request model for bulk card generation"""
    deck_id: int = Field(..., description="ID of the target deck")
    topic: str = Field(..., description="Topic or theme for card generation")
    count: int = Field(default=10, ge=1, le=15, description="Number of cards to generate (max 15)")
    source_language: str = Field(default="en", description="Source language code")
    target_language: str = Field(default="vi", description="Target language code") 
    cefr_level: Optional[CEFRLevel] = Field(default=None, description="CEFR difficulty level")
    keywords: Optional[List[str]] = Field(default=None, description="Specific keywords to include")
    include_examples: bool = Field(default=True, description="Include example sentences")
    include_pronunciation: bool = Field(default=True, description="Include IPA pronunciation")

class GeneratedCard(BaseModel):
    """Generated card data matching database schema"""
    front: str = Field(..., max_length=500, description="Front side content")
    back: str = Field(..., max_length=500, description="Back side content")  
    ipa_pronunciation: Optional[str] = Field(default=None, max_length=200, description="IPA pronunciation")
    examples: Optional[List[str]] = Field(default=None, description="Example sentences")
    synonyms: Optional[List[str]] = Field(default=None, description="Synonyms list")
    antonyms: Optional[List[str]] = Field(default=None, description="Antonyms list")
    tags: Optional[List[str]] = Field(default=None, description="Tags for categorization")
    difficulty: Difficulty = Field(default=Difficulty.NORMAL, description="Card difficulty")
    display_order: int = Field(default=0, description="Display order in deck")

class BulkCardGenerationResponse(BaseModel):
    """Response model for bulk card generation"""
    success: bool
    message: str
    generated_cards: List[GeneratedCard]
    total_count: int
    total_saved: int = Field(default=0, description="Number of cards actually saved (excluding duplicates)")
    duplicates_skipped: int = Field(default=0, description="Number of duplicate cards skipped")
    deck_id: int
    errors: Optional[List[str]] = None

class CardValidationError(BaseModel):
    """Error model for card validation"""
    card_index: int
    field: str
    error: str

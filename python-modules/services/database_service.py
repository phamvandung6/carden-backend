"""
Database service for card operations
"""
import logging
from typing import List, Optional
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import SQLAlchemyError
from config.settings import settings
from models.card_models import GeneratedCard
import json
import hashlib

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class DatabaseService:
    """Service for database operations"""
    
    def __init__(self):
        """Initialize database connection"""
        self.engine = create_engine(settings.DATABASE_URL)
        self.SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)
        logger.info("Đã khởi tạo kết nối database")
    
    def get_session(self) -> Session:
        """Get database session"""
        return self.SessionLocal()
    
    async def validate_deck_exists(self, deck_id: int, user_id: Optional[int] = None) -> bool:
        """
        Validate if deck exists and optionally belongs to user
        
        Args:
            deck_id: ID of the deck
            user_id: Optional user ID for ownership check
            
        Returns:
            bool: True if deck exists and accessible
        """
        try:
            with self.get_session() as session:
                query = text("""
                    SELECT id, user_id, deleted 
                    FROM decks 
                    WHERE id = :deck_id AND deleted = false
                """)
                
                result = session.execute(query, {"deck_id": deck_id}).fetchone()
                
                if not result:
                    logger.warning(f"Deck {deck_id} không tồn tại hoặc đã bị xóa")
                    return False
                
                # Check ownership if user_id provided
                if user_id is not None and result.user_id != user_id:
                    logger.warning(f"User {user_id} không có quyền truy cập deck {deck_id}")
                    return False
                
                return True
                
        except SQLAlchemyError as e:
            logger.error(f"Lỗi database khi validate deck: {str(e)}")
            return False
    
    async def check_deck_card_limit(self, deck_id: int, requested_cards: int, max_cards: int = 30) -> dict:
        """
        Check if adding requested cards would exceed deck limit
        
        Args:
            deck_id: ID of the deck
            requested_cards: Number of cards to add
            max_cards: Maximum cards allowed per deck (default 30)
            
        Returns:
            dict: Validation result with current count and limit info
        """
        try:
            with self.get_session() as session:
                # Get current card count (non-deleted cards only)
                query = text("""
                    SELECT COUNT(*) as current_count 
                    FROM cards 
                    WHERE deck_id = :deck_id AND deleted = false
                """)
                
                result = session.execute(query, {"deck_id": deck_id}).fetchone()
                current_count = result.current_count if result else 0
                
                would_exceed = (current_count + requested_cards) > max_cards
                remaining_slots = max_cards - current_count
                
                return {
                    "valid": not would_exceed,
                    "current_count": current_count,
                    "requested_cards": requested_cards,
                    "max_cards": max_cards,
                    "remaining_slots": max(0, remaining_slots),
                    "would_exceed": would_exceed,
                    "message": f"Deck hiện có {current_count}/{max_cards} thẻ, còn lại {remaining_slots} slot"
                }
                
        except SQLAlchemyError as e:
            logger.error(f"Lỗi database khi check card limit: {str(e)}")
            return {
                "valid": False,
                "current_count": 0,
                "requested_cards": requested_cards,
                "max_cards": max_cards,
                "remaining_slots": 0,
                "would_exceed": True,
                "message": f"Lỗi database: {str(e)}"
            }
    
    async def save_cards_to_database(
        self, 
        deck_id: int, 
        cards: List[GeneratedCard],
        user_id: Optional[int] = None
    ) -> dict:
        """
        Save generated cards to database
        
        Args:
            deck_id: ID of target deck
            cards: List of generated cards
            user_id: Optional user ID for validation
            
        Returns:
            dict: Operation result
        """
        session = self.get_session()
        
        try:
            # Validate deck exists
            if not await self.validate_deck_exists(deck_id, user_id):
                return {
                    "success": False,
                    "message": "Deck không tồn tại hoặc không có quyền truy cập",
                    "saved_count": 0,
                    "errors": ["Deck validation failed"]
                }
            
            # Get current max display_order for the deck
            max_order_query = text("""
                SELECT COALESCE(MAX(display_order), 0) as max_order 
                FROM cards 
                WHERE deck_id = :deck_id AND deleted = false
            """)
            
            max_order_result = session.execute(max_order_query, {"deck_id": deck_id}).fetchone()
            current_max_order = max_order_result.max_order if max_order_result else 0
            
            saved_count = 0
            errors = []
            
            # Insert each card
            for i, card in enumerate(cards):
                try:
                    # Generate unique_key for duplicate detection
                    unique_key = self._generate_unique_key(card.front, card.back)
                    
                    # Check for duplicates
                    duplicate_check = text("""
                        SELECT id FROM cards 
                        WHERE deck_id = :deck_id AND unique_key = :unique_key AND deleted = false
                    """)
                    
                    existing = session.execute(duplicate_check, {
                        "deck_id": deck_id,
                        "unique_key": unique_key
                    }).fetchone()
                    
                    if existing:
                        logger.warning(f"Thẻ duplicate: {card.front} - {card.back}")
                        errors.append(f"Thẻ {i+1} đã tồn tại: {card.front}")
                        continue
                    
                    # Insert card
                    insert_query = text("""
                        INSERT INTO cards (
                            deck_id, front, back, ipa_pronunciation, examples, 
                            synonyms, antonyms, tags, unique_key, difficulty, 
                            display_order, deleted, created_at, updated_at
                        ) VALUES (
                            :deck_id, :front, :back, :ipa_pronunciation, :examples,
                            :synonyms, :antonyms, :tags, :unique_key, :difficulty,
                            :display_order, false, NOW(), NOW()
                        )
                    """)
                    
                    session.execute(insert_query, {
                        "deck_id": deck_id,
                        "front": card.front,
                        "back": card.back,
                        "ipa_pronunciation": card.ipa_pronunciation,
                        "examples": json.dumps(card.examples) if card.examples else None,
                        "synonyms": json.dumps(card.synonyms) if card.synonyms else None,
                        "antonyms": json.dumps(card.antonyms) if card.antonyms else None,
                        "tags": json.dumps(card.tags) if card.tags else None,
                        "unique_key": unique_key,
                        "difficulty": card.difficulty.value,
                        "display_order": current_max_order + i + 1
                    })
                    
                    saved_count += 1
                    
                except Exception as card_error:
                    logger.error(f"Lỗi khi lưu thẻ {i+1}: {str(card_error)}")
                    errors.append(f"Lỗi lưu thẻ {i+1}: {str(card_error)}")
                    continue
            
            # Update deck card_count
            if saved_count > 0:
                update_deck_query = text("""
                    UPDATE decks 
                    SET card_count = card_count + :new_cards, updated_at = NOW()
                    WHERE id = :deck_id
                """)
                
                session.execute(update_deck_query, {
                    "new_cards": saved_count,
                    "deck_id": deck_id
                })
            
            # Commit transaction
            session.commit()
            
            logger.info(f"Đã lưu thành công {saved_count}/{len(cards)} thẻ vào deck {deck_id}")
            
            return {
                "success": True,
                "message": f"Đã lưu thành công {saved_count} thẻ",
                "saved_count": saved_count,
                "total_requested": len(cards),
                "errors": errors if errors else None
            }
            
        except SQLAlchemyError as e:
            session.rollback()
            logger.error(f"Lỗi database khi lưu thẻ: {str(e)}")
            return {
                "success": False,
                "message": f"Lỗi database: {str(e)}",
                "saved_count": 0,
                "errors": [str(e)]
            }
            
        finally:
            session.close()
    
    def _generate_unique_key(self, front: str, back: str) -> str:
        """
        Generate unique key for card (matching Java implementation)
        
        Args:
            front: Front side content
            back: Back side content
            
        Returns:
            str: Unique key for duplicate detection
        """
        # Normalize text for duplicate detection (matching Java logic)
        normalized_front = front.lower().strip()
        normalized_front = ' '.join(normalized_front.split())  # Replace multiple spaces with single space
        
        normalized_back = back.lower().strip()
        normalized_back = ' '.join(normalized_back.split())
        
        return f"{normalized_front}:{normalized_back}"
    
    async def get_deck_info(self, deck_id: int) -> Optional[dict]:
        """
        Get deck information
        
        Args:
            deck_id: ID of the deck
            
        Returns:
            dict: Deck information or None if not found
        """
        try:
            with self.get_session() as session:
                query = text("""
                    SELECT id, title, description, user_id, topic_id, 
                           source_language, target_language, cefr_level,
                           card_count, created_at, updated_at
                    FROM decks 
                    WHERE id = :deck_id AND deleted = false
                """)
                
                result = session.execute(query, {"deck_id": deck_id}).fetchone()
                
                if result:
                    return {
                        "id": result.id,
                        "title": result.title,
                        "description": result.description,
                        "user_id": result.user_id,
                        "topic_id": result.topic_id,
                        "source_language": result.source_language,
                        "target_language": result.target_language,
                        "cefr_level": result.cefr_level,
                        "card_count": result.card_count,
                        "created_at": result.created_at,
                        "updated_at": result.updated_at
                    }
                
                return None
                
        except SQLAlchemyError as e:
            logger.error(f"Lỗi khi lấy thông tin deck: {str(e)}")
            return None

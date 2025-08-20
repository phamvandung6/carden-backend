"""
FastAPI application for bulk card generation
"""
import logging
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from typing import Optional
from sqlalchemy import text

from config.settings import settings
from models.card_models import (
    CardGenerationRequest,
    BulkCardGenerationResponse
)
from services.gemini_card_generator import GeminiCardGenerator
from services.database_service import DatabaseService
from services.content_validator import content_validator

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize services
card_generator = None
database_service = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan management"""
    global card_generator, database_service
    
    # Startup
    logger.info("Khởi động dịch vụ tạo thẻ...")
    try:
        card_generator = GeminiCardGenerator()
        database_service = DatabaseService()
        logger.info("Đã khởi tạo thành công các dịch vụ")
    except Exception as e:
        logger.error(f"Lỗi khởi tạo dịch vụ: {str(e)}")
        raise
    
    yield
    
    # Shutdown
    logger.info("Dừng dịch vụ tạo thẻ...")

# Initialize FastAPI app
app = FastAPI(
    title="Carden Card Generation Service",
    description="Dịch vụ tạo thẻ học hàng loạt sử dụng Google Gemini AI",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "message": "Dịch vụ tạo thẻ Carden đang hoạt động",
        "version": "1.0.0",
        "status": "healthy"
    }

@app.get("/health")
async def health_check():
    """Detailed health check"""
    try:
        # Test database connection without causing warnings
        db_status = "connected"
        if database_service:
            try:
                with database_service.get_session() as session:
                    # Simple connection test without querying specific tables
                    session.execute(text("SELECT 1"))
            except Exception:
                db_status = "error"
        else:
            db_status = "error"
        
        return {
            "status": "healthy",
            "services": {
                "gemini_api": "connected" if card_generator else "error",
                "database": db_status
            },
            "timestamp": "2024-01-01T00:00:00Z"
        }
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        raise HTTPException(status_code=503, detail="Service unhealthy")

@app.post("/generate-cards", response_model=BulkCardGenerationResponse)
async def generate_cards(
    request: CardGenerationRequest,
    background_tasks: BackgroundTasks,
    user_id: Optional[int] = None
):
    """
    Generate cards using Gemini AI and save to database
    
    Args:
        request: Card generation request
        background_tasks: FastAPI background tasks
        user_id: Optional user ID for authorization
        
    Returns:
        BulkCardGenerationResponse: Generation result
    """
    try:
        logger.info(f"Nhận yêu cầu tạo {request.count} thẻ cho deck {request.deck_id}")
        
        # Validate deck exists and user has access
        if not await database_service.validate_deck_exists(request.deck_id, user_id):
            raise HTTPException(
                status_code=404,
                detail="Deck không tồn tại hoặc không có quyền truy cập"
            )
        
        # Check deck card limit (configurable max cards per deck)
        limit_check = await database_service.check_deck_card_limit(
            request.deck_id, 
            request.count, 
            settings.MAX_CARDS_PER_DECK
        )
        if not limit_check["valid"]:
            raise HTTPException(
                status_code=400,
                detail=f"Không thể thêm {request.count} thẻ. {limit_check['message']}. Tối đa {limit_check['max_cards']} thẻ/deck."
            )
        
        logger.info(f"Deck {request.deck_id} limit check passed: {limit_check['message']}")
        
        # Generate cards using Gemini
        generation_response = await card_generator.generate_cards(request)
        
        # Handle content validation failure gracefully
        if not generation_response.success:
            logger.warning(f"Card generation blocked/failed: {generation_response.message}")
            
            # Check if it's a content validation issue (contains suggestions or validation keywords)
            is_validation_issue = (
                "not approved" in generation_response.message.lower() or
                "inappropriate" in generation_response.message.lower() or
                "suggestions:" in generation_response.message.lower() or
                len(generation_response.errors or []) > 0 and any("suggestions:" in str(error).lower() for error in generation_response.errors)
            )
            
            if is_validation_issue:
                # Return structured validation failure instead of HTTP error
                return generation_response
            else:
                # For other errors, still raise HTTP exception
                raise HTTPException(
                    status_code=400,
                    detail=generation_response.message
                )
        
        # Save cards to database
        save_result = await database_service.save_cards_to_database(
            deck_id=request.deck_id,
            cards=generation_response.generated_cards,
            user_id=user_id
        )
        
        # Update response with save results
        generation_response.message = save_result["message"]
        generation_response.success = save_result["success"]
        generation_response.total_saved = save_result["saved_count"]
        
        # Calculate duplicates skipped
        total_generated = len(generation_response.generated_cards)
        duplicates_skipped = total_generated - save_result["saved_count"]
        generation_response.duplicates_skipped = duplicates_skipped
        
        if save_result.get("errors"):
            generation_response.errors = (generation_response.errors or []) + save_result["errors"]
        
        # Update message with duplicate info if any
        if duplicates_skipped > 0:
            generation_response.message = f"Đã lưu {save_result['saved_count']} thẻ mới, bỏ qua {duplicates_skipped} thẻ trùng lặp"
        
        logger.info(f"Hoàn thành tạo thẻ cho deck {request.deck_id}: {save_result['saved_count']} thẻ đã lưu, {duplicates_skipped} thẻ trùng lặp")
        
        return generation_response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Lỗi không mong muốn: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi hệ thống: {str(e)}"
        )

@app.get("/deck/{deck_id}/info")
async def get_deck_info(deck_id: int, user_id: Optional[int] = None):
    """
    Get deck information
    
    Args:
        deck_id: ID of the deck
        user_id: Optional user ID for authorization
        
    Returns:
        dict: Deck information
    """
    try:
        # Validate access
        if not await database_service.validate_deck_exists(deck_id, user_id):
            raise HTTPException(
                status_code=404,
                detail="Deck không tồn tại hoặc không có quyền truy cập"
            )
        
        # Get deck info
        deck_info = await database_service.get_deck_info(deck_id)
        
        if not deck_info:
            raise HTTPException(status_code=404, detail="Không tìm thấy thông tin deck")
        
        return deck_info
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Lỗi khi lấy thông tin deck: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi hệ thống: {str(e)}"
        )

@app.post("/validate-topic")
async def validate_topic(topic_request: dict):
    """
    Validate topic using LangChain content validator
    
    Args:
        topic_request: Dict containing 'topic' field
        
    Returns:
        dict: Validation result
    """
    try:
        topic = topic_request.get("topic", "")
        
        if not topic:
            raise HTTPException(
                status_code=400,
                detail="Topic is required"
            )
        
        logger.info(f"Validating topic: {topic}")
        validation_result = await content_validator.validate_topic(topic)
        
        return {
            "success": validation_result.success,
            "approved": validation_result.approved,
            "original_topic": validation_result.original_topic,
            "final_topic": validation_result.final_topic,
            "message": validation_result.message,
            "suggestions": validation_result.suggestions,
            "warnings": validation_result.warnings
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Lỗi validation: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi hệ thống: {str(e)}"
        )

@app.post("/generate-cards-async")
async def generate_cards_async(
    request: CardGenerationRequest,
    background_tasks: BackgroundTasks,
    user_id: Optional[int] = None
):
    """
    Generate cards asynchronously (for large batches)
    
    Args:
        request: Card generation request
        background_tasks: FastAPI background tasks
        user_id: Optional user ID for authorization
        
    Returns:
        dict: Task submission confirmation
    """
    try:
        # Validate deck exists
        if not await database_service.validate_deck_exists(request.deck_id, user_id):
            raise HTTPException(
                status_code=404,
                detail="Deck không tồn tại hoặc không có quyền truy cập"
            )
        
        # Add background task
        background_tasks.add_task(
            _process_card_generation_background,
            request,
            user_id
        )
        
        return {
            "message": "Yêu cầu tạo thẻ đã được gửi và đang được xử lý",
            "deck_id": request.deck_id,
            "requested_count": request.count,
            "status": "processing"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Lỗi khi gửi yêu cầu async: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi hệ thống: {str(e)}"
        )

async def _process_card_generation_background(
    request: CardGenerationRequest,
    user_id: Optional[int] = None
):
    """Background task for card generation"""
    try:
        logger.info(f"Bắt đầu xử lý background task cho deck {request.deck_id}")
        
        # Generate cards
        generation_response = await card_generator.generate_cards(request)
        
        if generation_response.success:
            # Save to database
            save_result = await database_service.save_cards_to_database(
                deck_id=request.deck_id,
                cards=generation_response.generated_cards,
                user_id=user_id
            )
            
            logger.info(f"Background task hoàn thành: {save_result['saved_count']} thẻ đã lưu")
        else:
            logger.error(f"Background task thất bại: {generation_response.message}")
            
    except Exception as e:
        logger.error(f"Lỗi trong background task: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
        log_level="info"
    )

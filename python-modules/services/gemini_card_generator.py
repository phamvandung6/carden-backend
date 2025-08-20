"""
Gemini AI-powered card generation service
"""
import json
import logging
import os
import httpx
from typing import List, Optional
from vertexai.generative_models import GenerativeModel
import vertexai
from config.settings import settings
from models.card_models import (
    CardGenerationRequest,
    GeneratedCard,
    BulkCardGenerationResponse,
    Difficulty,
    CEFRLevel
)
from services.content_validator import content_validator

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class GeminiCardGenerator:
    """Service for generating cards using Google Gemini API"""
    
    def __init__(self):
        """Initialize Vertex AI client"""
        if not settings.GOOGLE_CLOUD_PROJECT:
            raise ValueError("GOOGLE_CLOUD_PROJECT không được cấu hình")
        
        # Configure proxy if available
        if settings.HTTP_PROXY or settings.HTTPS_PROXY:
            proxy_config = {}
            if settings.HTTP_PROXY:
                proxy_config['http://'] = settings.HTTP_PROXY
            if settings.HTTPS_PROXY:
                proxy_config['https://'] = settings.HTTPS_PROXY
            
            # Set environment variables for requests library
            if settings.HTTP_PROXY:
                os.environ['HTTP_PROXY'] = settings.HTTP_PROXY
            if settings.HTTPS_PROXY:
                os.environ['HTTPS_PROXY'] = settings.HTTPS_PROXY
            
            logger.info(f"Configured proxy: {proxy_config}")
        
        # Configure Vertex AI
        try:
            vertexai.init(
                project=settings.GOOGLE_CLOUD_PROJECT,
                location=settings.GOOGLE_CLOUD_REGION
            )
            self.model = GenerativeModel(settings.GEMINI_MODEL)
            logger.info(f"Đã khởi tạo Vertex AI model: {settings.GEMINI_MODEL}")
        except Exception as e:
            logger.error(f"Failed to initialize Vertex AI client: {str(e)}")
            raise
    
    async def generate_cards(self, request: CardGenerationRequest) -> BulkCardGenerationResponse:
        """
        Generate cards using Gemini API with LangChain content validation
        
        Args:
            request: Card generation request
            
        Returns:
            BulkCardGenerationResponse: Generated cards response
        """
        try:
            # Step 1: LangChain Content Validation
            logger.info(f"🔍 Validating topic: {request.topic}")
            validation_result = await content_validator.validate_topic(request.topic)
            
            if not validation_result.success:
                return BulkCardGenerationResponse(
                    success=False,
                    message="Content validation failed",
                    generated_cards=[],
                    total_count=0,
                    deck_id=request.deck_id,
                    errors=["Content validation error"]
                )
            
            if not validation_result.approved:
                return BulkCardGenerationResponse(
                    success=False,
                    message=validation_result.message,
                    generated_cards=[],
                    total_count=0,
                    deck_id=request.deck_id,
                    errors=[
                        "Content not approved for card generation",
                        f"Suggestions: {', '.join(validation_result.suggestions)}"
                    ]
                )
            
            # Use validated and sanitized topic
            validated_topic = validation_result.final_topic
            logger.info(f"✅ Topic approved: {validated_topic}")
            
            # Step 2: Basic request validation
            if request.count > settings.MAX_CARDS_PER_BATCH:
                return BulkCardGenerationResponse(
                    success=False,
                    message=f"Không thể tạo quá {settings.MAX_CARDS_PER_BATCH} thẻ một lần",
                    generated_cards=[],
                    total_count=0,
                    deck_id=request.deck_id,
                    errors=[f"Số lượng thẻ vượt quá giới hạn: {request.count} > {settings.MAX_CARDS_PER_BATCH}"]
                )
            
            # Step 3: Build prompt with validated topic
            # Update request with validated topic
            validated_request = request.copy(deep=True)
            validated_request.topic = validated_topic
            
            prompt = self._build_generation_prompt(validated_request)
            logger.info(f"Tạo {request.count} thẻ cho deck {request.deck_id} với chủ đề đã được xác thực: {validated_topic}")
            
            # Generate content using Gemini
            response = await self._call_gemini_api(prompt)
            
            # Parse and validate response
            cards_data = self._parse_gemini_response(response)
            
            # Convert to GeneratedCard objects
            generated_cards = self._convert_to_generated_cards(
                cards_data, 
                request.count,
                request.source_language,
                request.target_language
            )
            
            return BulkCardGenerationResponse(
                success=True,
                message=f"Đã tạo thành công {len(generated_cards)} thẻ",
                generated_cards=generated_cards,
                total_count=len(generated_cards),
                deck_id=request.deck_id
            )
            
        except Exception as e:
            logger.error(f"Lỗi khi tạo thẻ: {str(e)}")
            return BulkCardGenerationResponse(
                success=False,
                message=f"Lỗi khi tạo thẻ: {str(e)}",
                generated_cards=[],
                total_count=0,
                deck_id=request.deck_id,
                errors=[str(e)]
            )
    
    def _build_generation_prompt(self, request: CardGenerationRequest) -> str:
        """Build prompt for Gemini API"""
        
        # Language information
        source_lang = settings.SUPPORTED_LANGUAGES.get(request.source_language, request.source_language)
        target_lang = settings.SUPPORTED_LANGUAGES.get(request.target_language, request.target_language)
        
        # CEFR level info
        cefr_info = f"Mức độ CEFR: {request.cefr_level.value}" if request.cefr_level else "Mức độ: Trung bình"
        
        # Keywords info
        keywords_info = f"Từ khóa cụ thể: {', '.join(request.keywords)}" if request.keywords else ""
        
        prompt = f"""
{settings.CARD_GENERATION_SYSTEM_PROMPT}

Tạo {request.count} thẻ học ngôn ngữ với thông tin sau:
- Chủ đề: {request.topic}
- Ngôn ngữ nguồn: {source_lang} ({request.source_language})
- Ngôn ngữ đích: {target_lang} ({request.target_language})
- {cefr_info}
{keywords_info}

Yêu cầu bổ sung:
- Bao gồm ví dụ: {'Có' if request.include_examples else 'Không'}
- Bao gồm phiên âm: {'Có' if request.include_pronunciation else 'Không'}

Hãy tạo {request.count} thẻ đa dạng và phù hợp với chủ đề "{request.topic}".
Đảm bảo mỗi thẻ có nội dung phong phú và hữu ích cho việc học.
"""
        
        return prompt
    
    async def _call_gemini_api(self, prompt: str) -> str:
        """Call Vertex AI with prompt"""
        try:
            from vertexai.generative_models import GenerationConfig
            
            generation_config = GenerationConfig(
                candidate_count=1,
                temperature=0.7,
                max_output_tokens=8192,
                response_mime_type="application/json"
            )
            
            response = self.model.generate_content(
                prompt,
                generation_config=generation_config
            )
            
            if not response.text:
                raise ValueError("Vertex AI trả về phản hồi trống")
            
            return response.text
            
        except Exception as e:
            logger.error(f"Lỗi khi gọi Vertex AI: {str(e)}")
            raise Exception(f"Lỗi Vertex AI: {str(e)}")
    
    def _parse_gemini_response(self, response_text: str) -> List[dict]:
        """Parse JSON response from Gemini"""
        try:
            # Clean response text
            cleaned_text = response_text.strip()
            if cleaned_text.startswith('```json'):
                cleaned_text = cleaned_text[7:]
            if cleaned_text.endswith('```'):
                cleaned_text = cleaned_text[:-3]
            
            # Parse JSON
            response_data = json.loads(cleaned_text)
            
            if "cards" not in response_data:
                raise ValueError("Response không chứa trường 'cards'")
            
            cards = response_data["cards"]
            if not isinstance(cards, list):
                raise ValueError("Trường 'cards' phải là một mảng")
            
            return cards
            
        except json.JSONDecodeError as e:
            logger.error(f"Lỗi parse JSON: {str(e)}")
            logger.error(f"Response text: {response_text}")
            raise Exception(f"Lỗi parse JSON từ Vertex AI: {str(e)}")
    
    def _convert_to_generated_cards(
        self, 
        cards_data: List[dict], 
        requested_count: int,
        source_language: str,
        target_language: str
    ) -> List[GeneratedCard]:
        """Convert raw card data to GeneratedCard objects"""
        
        generated_cards = []
        
        for i, card_data in enumerate(cards_data[:requested_count]):
            try:
                # Validate required fields
                if "front" not in card_data or "back" not in card_data:
                    logger.warning(f"Thẻ {i+1} thiếu trường front hoặc back")
                    continue
                
                # Create GeneratedCard
                card = GeneratedCard(
                    front=str(card_data.get("front", "")).strip()[:500],
                    back=str(card_data.get("back", "")).strip()[:500],
                    ipa_pronunciation=self._safe_get_string(card_data, "ipa_pronunciation", 200),
                    examples=self._safe_get_list(card_data, "examples"),
                    synonyms=self._safe_get_list(card_data, "synonyms"),
                    antonyms=self._safe_get_list(card_data, "antonyms"),
                    tags=self._safe_get_list(card_data, "tags"),
                    difficulty=self._parse_difficulty(card_data.get("difficulty", "NORMAL")),
                    display_order=i
                )
                
                generated_cards.append(card)
                
            except Exception as e:
                logger.warning(f"Lỗi khi chuyển đổi thẻ {i+1}: {str(e)}")
                continue
        
        return generated_cards
    
    def _safe_get_string(self, data: dict, key: str, max_length: int) -> Optional[str]:
        """Safely get string value with length limit"""
        value = data.get(key)
        if value and isinstance(value, str):
            return value.strip()[:max_length] if value.strip() else None
        return None
    
    def _safe_get_list(self, data: dict, key: str) -> Optional[List[str]]:
        """Safely get list value"""
        value = data.get(key)
        if value and isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
        return None
    
    def _parse_difficulty(self, difficulty_str: str) -> Difficulty:
        """Parse difficulty string to enum"""
        try:
            return Difficulty(difficulty_str.upper())
        except ValueError:
            return Difficulty.NORMAL

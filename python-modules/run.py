#!/usr/bin/env python3
"""
Runner script for card generation service
"""
import sys
import json
import asyncio
from pathlib import Path

# Add current directory to Python path
sys.path.append(str(Path(__file__).parent))

from services.gemini_card_generator import GeminiCardGenerator
from services.database_service import DatabaseService
from models.card_models import CardGenerationRequest, CEFRLevel

async def main():
    """Main function for command line usage"""
    if len(sys.argv) < 2:
        print("Usage: python run.py <command> [args...]")
        print("Commands:")
        print("  generate-cards <deck_id> <topic> <count> [source_lang] [target_lang] [cefr_level]")
        print("  test-connection")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "test-connection":
        await test_connection()
    elif command == "generate-cards":
        await generate_cards_cli()
    else:
        print(f"Unknown command: {command}")
        sys.exit(1)

async def test_connection():
    """Test database and API connections"""
    print("Testing connections...")
    
    try:
        # Test database
        db_service = DatabaseService()
        print("✓ Database connection successful")
        
        # Test Gemini API
        card_generator = GeminiCardGenerator()
        print("✓ Gemini API connection successful")
        
        print("All connections are working!")
        
    except Exception as e:
        print(f"✗ Connection failed: {str(e)}")
        sys.exit(1)

async def generate_cards_cli():
    """Generate cards from command line"""
    if len(sys.argv) < 5:
        print("Usage: python run.py generate-cards <deck_id> <topic> <count> [source_lang] [target_lang] [cefr_level]")
        sys.exit(1)
    
    try:
        deck_id = int(sys.argv[2])
        topic = sys.argv[3]
        count = int(sys.argv[4])
        source_lang = sys.argv[5] if len(sys.argv) > 5 else "en"
        target_lang = sys.argv[6] if len(sys.argv) > 6 else "vi"
        cefr_level = sys.argv[7] if len(sys.argv) > 7 else None
        
        # Parse CEFR level
        cefr = None
        if cefr_level:
            try:
                cefr = CEFRLevel(cefr_level.upper())
            except ValueError:
                print(f"Invalid CEFR level: {cefr_level}")
                sys.exit(1)
        
        # Create request
        request = CardGenerationRequest(
            deck_id=deck_id,
            topic=topic,
            count=count,
            source_language=source_lang,
            target_language=target_lang,
            cefr_level=cefr
        )
        
        print(f"Generating {count} cards for deck {deck_id} with topic: {topic}")
        
        # Initialize services
        card_generator = GeminiCardGenerator()
        database_service = DatabaseService()
        
        # Generate cards
        response = await card_generator.generate_cards(request)
        
        if not response.success:
            print(f"Generation failed: {response.message}")
            if response.errors:
                for error in response.errors:
                    print(f"Error: {error}")
            sys.exit(1)
        
        print(f"Generated {len(response.generated_cards)} cards")
        
        # Save to database
        save_result = await database_service.save_cards_to_database(
            deck_id=deck_id,
            cards=response.generated_cards
        )
        
        print(f"Save result: {save_result['message']}")
        print(f"Saved {save_result['saved_count']} cards")
        
        if save_result.get('errors'):
            print("Errors:")
            for error in save_result['errors']:
                print(f"  - {error}")
        
    except ValueError as e:
        print(f"Invalid input: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())

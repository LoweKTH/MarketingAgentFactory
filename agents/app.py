"""
Marketing Agent Factory - Flask Application Entry Point

Simple Flask app for content generation using Google Gemini.
Provides RESTful API for Spring Boot backend integration.
"""
import os
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import our content agent
from content_agent import ContentAgent

# Configure logging
logging.basicConfig(
    level=getattr(logging, os.environ.get('LOG_LEVEL', 'INFO')),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Create Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for frontend communication

# Initialize content agent
content_agent = ContentAgent()

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint for service monitoring.

    Returns:
        JSON response with service status
    """
    return jsonify({
        "status": "healthy",
        "service": "marketing-agent-factory-python",
        "llm_provider": "gemini",
        "version": "1.0.0"
    })

@app.route('/generate', methods=['POST'])
def generate_content():
    """
    Generate marketing content using the content agent.

    Expected JSON body:
    {
        "content_type": "social_post",
        "brand_voice": "professional",
        "topic": "productivity tips",
        "platform": "linkedin",
        "target_audience": "business professionals",
        "key_messages": ["time management", "efficiency"]
    }

    Returns:
        JSON response with generated content and metadata
    """
    try:
        # Get JSON data from request
        data = request.get_json()

        # Validate required fields
        required_fields = ['content_type', 'brand_voice', 'topic']
        missing_fields = [field for field in required_fields if field not in data]

        if missing_fields:
            return jsonify({
                "error": "Missing required fields",
                "missing_fields": missing_fields
            }), 400

        # Log the generation request
        logger.info(f"Content generation request: {data.get('content_type')} - {data.get('topic')}")

        # Generate content using the agent
        result = content_agent.generate_content(data)

        # Return successful response
        return jsonify({
            "status": "success",
            "result": result
        })

    except ValueError as e:
        # Handle validation errors
        logger.error(f"Validation error: {str(e)}")
        return jsonify({
            "status": "error",
            "error": "Invalid input",
            "message": str(e)
        }), 400

    except Exception as e:
        # Handle all other errors
        logger.error(f"Content generation failed: {str(e)}")
        return jsonify({
            "status": "error",
            "error": "Generation failed",
            "message": str(e)
        }), 500

@app.route('/generate/stream/<task_id>', methods=['GET'])
def stream_progress(task_id):
    """
    Stream progress updates for a content generation task.

    This is a placeholder for future streaming implementation.
    For now, returns the current status.

    Args:
        task_id: Task identifier

    Returns:
        JSON with task progress
    """
    # Placeholder - in a real implementation, this would stream SSE
    return jsonify({
        "task_id": task_id,
        "status": "completed",
        "progress": 100,
        "message": "Content generation completed"
    })

@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors with JSON response."""
    return jsonify({
        "status": "error",
        "error": "Not found",
        "message": "The requested endpoint does not exist"
    }), 404

@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors with JSON response."""
    return jsonify({
        "status": "error",
        "error": "Internal server error",
        "message": "An unexpected error occurred"
    }), 500

if __name__ == '__main__':
    # Get configuration from environment
    host = os.environ.get('FLASK_HOST', '0.0.0.0')
    port = int(os.environ.get('FLASK_PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', 'True').lower() == 'true'

    # Start the Flask application
    logger.info(f"Starting Marketing Agent Factory on {host}:{port}")
    app.run(host=host, port=port, debug=debug)
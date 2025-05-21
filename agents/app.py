"""
Marketing Agent Factory - Flask Application Entry Point

Updated Flask app for content generation using Google Gemini.
Provides RESTful API for Spring Boot backend integration with
evaluator-optimizer workflow support.
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
try:
    content_agent = ContentAgent()
    logger.info("Content agent initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize content agent: {str(e)}")
    content_agent = None

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint for service monitoring.
    Matches the format expected by Spring Boot's PythonServiceClient.

    Returns:
        JSON response with service status
    """
    agent_status = "healthy" if content_agent is not None else "unhealthy"

    return jsonify({
        "status": "healthy" if agent_status == "healthy" else "unhealthy",
        "service": "marketing-agent-factory-python",
        "agent_status": agent_status,
        "llm_provider": "gemini",
        "model": os.environ.get('GEMINI_MODEL', 'gemini-2.0-flash'),
        "version": "1.0.0"
    })

@app.route('/generate', methods=['POST'])
def generate_content():
    """
    Generate marketing content using the content agent with evaluator-optimizer workflow.

    Expects the exact format from Spring Boot's PythonGenerationRequest:
    {
        "contentType": "social_post",
        "brandVoice": "professional",
        "topic": "productivity tips",
        "platform": "linkedin",
        "targetAudience": "business professionals",
        "keyMessages": ["time management", "efficiency"],
        "brandGuidelines": "Professional yet approachable...",
        "additionalContext": "Focus on remote work...",
        "lengthPreference": "medium",
        "includeHashtags": true,
        "callToAction": "Learn more"
    }

    Returns:
        JSON response matching PythonGenerationResponse format
    """
    try:
        # Check if content agent is available
        if content_agent is None:
            return jsonify({
                "error": "Content agent not initialized",
                "message": "Service is not ready. Check API key configuration."
            }), 503

        # Get JSON data from request
        data = request.get_json()

        if not data:
            return jsonify({
                "error": "No JSON data provided",
                "message": "Request body must contain valid JSON"
            }), 400

        # Validate required fields (matching Spring Boot expectations)
        required_fields = ['contentType', 'brandVoice', 'topic']
        missing_fields = [field for field in required_fields if field not in data]

        if missing_fields:
            return jsonify({
                "error": "Missing required fields",
                "missing_fields": missing_fields,
                "message": f"Request must include: {', '.join(required_fields)}"
            }), 400

        # Log the generation request
        logger.info(f"Content generation request: {data.get('contentType')} - {data.get('topic')}")
        logger.debug(f"Request details: {data}")

        # Generate content using the agent with evaluator-optimizer workflow
        result = content_agent.generate_content(data)

        # Log successful completion
        logger.info(f"Content generation completed: {result.get('generationTimeSeconds', 0):.2f}s")

        # Return the result directly (already in the correct format)
        return jsonify(result)

    except ValueError as e:
        # Handle validation errors
        logger.error(f"Validation error: {str(e)}")
        return jsonify({
            "error": "Invalid input",
            "message": str(e)
        }), 400

    except Exception as e:
        # Handle all other errors
        logger.error(f"Content generation failed: {str(e)}", exc_info=True)
        return jsonify({
            "error": "Generation failed",
            "message": str(e)
        }), 500

@app.route('/stream/<task_id>', methods=['GET'])
def stream_progress(task_id):
    """
    Stream progress updates for a content generation task.

    This endpoint will be enhanced later with Server-Sent Events (SSE)
    for real-time progress streaming. For now, returns task status.

    Args:
        task_id: Task identifier

    Returns:
        JSON with task progress information
    """
    logger.debug(f"Progress stream request for task: {task_id}")

    # Placeholder response - will be enhanced with real streaming
    return jsonify({
        "task_id": task_id,
        "status": "completed",
        "progress": 100,
        "message": "Content generation completed",
        "steps": [
            {"step": "initial_generation", "status": "completed", "timestamp": "2024-01-01T12:00:00Z"},
            {"step": "evaluation", "status": "completed", "timestamp": "2024-01-01T12:00:30Z"},
            {"step": "optimization", "status": "completed", "timestamp": "2024-01-01T12:01:00Z"}
        ]
    })

@app.route('/evaluate', methods=['POST'])
def evaluate_content():
    """
    Standalone content evaluation endpoint.

    Allows evaluation of existing content without regeneration.
    Useful for testing the evaluator component independently.

    Expected JSON:
    {
        "content": "Content to evaluate...",
        "contentType": "social_post",
        "brandVoice": "professional",
        "platform": "linkedin"
    }
    """
    try:
        if content_agent is None:
            return jsonify({
                "error": "Content agent not initialized"
            }), 503

        data = request.get_json()

        if not data or 'content' not in data:
            return jsonify({
                "error": "Missing required field 'content'"
            }), 400

        # Perform evaluation
        content = data['content']
        content_type = data.get('contentType', 'general')
        brand_voice = data.get('brandVoice', 'professional')
        platform = data.get('platform', 'general')
        key_messages = data.get('keyMessages', [])
        brand_guidelines = data.get('brandGuidelines', '')

        evaluation = content_agent._evaluate_content(
            content, content_type, brand_voice, platform, key_messages, brand_guidelines
        )

        logger.info(f"Content evaluation completed: score {evaluation.get('score', 'N/A')}")

        return jsonify({
            "evaluation": evaluation,
            "content": content,
            "contentType": content_type
        })

    except Exception as e:
        logger.error(f"Content evaluation failed: {str(e)}")
        return jsonify({
            "error": "Evaluation failed",
            "message": str(e)
        }), 500

@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors with JSON response."""
    return jsonify({
        "error": "Not found",
        "message": "The requested endpoint does not exist",
        "available_endpoints": ["/health", "/generate", "/stream/<task_id>", "/evaluate"]
    }), 404

@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors with JSON response."""
    logger.error(f"Internal server error: {str(error)}")
    return jsonify({
        "error": "Internal server error",
        "message": "An unexpected error occurred"
    }), 500

@app.before_request
def log_request_info():
    """Log incoming requests for debugging."""
    if request.method == 'POST':
        logger.debug(f"Incoming {request.method} request to {request.path}")
        if request.is_json:
            logger.debug(f"Request JSON keys: {list(request.get_json().keys()) if request.get_json() else 'None'}")

if __name__ == '__main__':
    # Get configuration from environment
    host = os.environ.get('FLASK_HOST', '0.0.0.0')
    port = int(os.environ.get('FLASK_PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', 'True').lower() == 'true'

    # Validate configuration
    if not os.environ.get('GOOGLE_API_KEY'):
        logger.warning("GOOGLE_API_KEY not found in environment variables")

    # Start the Flask application
    logger.info(f"Starting Marketing Agent Factory on {host}:{port}")
    logger.info(f"Debug mode: {debug}")
    logger.info(f"Content agent status: {'Ready' if content_agent else 'Not available'}")

    app.run(host=host, port=port, debug=debug)
"""
Marketing Agent Factory - Smart Threshold Flask Application

Flask app for content generation with smart threshold evaluator-optimizer workflow.
Provides RESTful API for Spring Boot backend integration.
"""
import os
import time
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import our smart threshold content agent
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
    Provides status about the AI service.
    """
    agent_status = "healthy" if content_agent is not None else "unhealthy"

    return jsonify({
        "status": agent_status,
        "service": "marketing-agent-factory-python",
        "llm_provider": "gemini",
        "model": os.environ.get('GEMINI_MODEL', 'gemini-2.0-flash'),
        "version": "1.0.0",
        "features": {
            "smart_threshold_optimization": True,
            "evaluation": True
        }
    })

@app.route('/generate', methods=['POST'])
def generate_content():
    """
    Generate marketing content with smart threshold evaluator-optimizer workflow.

    Uses the threshold-based approach to decide optimization strategy:
    - Below 7.0: Full optimization
    - 7.0-8.5 with specific weaknesses: Targeted optimization
    - Above 8.5: No optimization

    Returns both versions when optimization occurs.
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

        # Validate required fields
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

        # Start timer for performance measurement
        start_time = time.time()

        # Generate content using the agent with smart threshold workflow
        result = content_agent.generate_content(data)

        # Log the response structure
        app.logger.info(f"Response structure: {result['workflowInfo']}")
        # Make sure evaluationPerformed is not being modified
        if 'workflowInfo' in result and 'evaluationPerformed' in result['workflowInfo']:
            app.logger.info(f"evaluationPerformed: {result['workflowInfo']['evaluationPerformed']}")

        # Calculate total processing time
        total_time = time.time() - start_time

        # Log successful completion
        optimization_type = result.get('workflowInfo', {}).get('optimizationType', 'none')
        logger.info(f"Content generation completed in {total_time:.2f}s with "
                  f"optimization type: {optimization_type}")

        # Return the result - includes both versions when optimization performed
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

@app.route('/evaluate', methods=['POST'])
def evaluate_content():
    """
    Standalone content evaluation endpoint.

    Allows evaluation of existing content without regeneration.
    Useful for testing the evaluator component independently.
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

@app.route('/config', methods=['GET'])
def get_config():
    """
    Get current configuration settings.

    Returns the current threshold settings for the smart threshold workflow.
    """
    if content_agent is None:
        return jsonify({
            "error": "Content agent not initialized"
        }), 503

    config = {
        "lowQualityThreshold": content_agent.low_quality_threshold,
        "highQualityThreshold": content_agent.high_quality_threshold,
        "modelName": content_agent.model_name
    }

    return jsonify(config)

@app.route('/config', methods=['POST'])
def update_config():
    """
    Update configuration settings.

    Allows runtime adjustment of threshold parameters.
    """
    if content_agent is None:
        return jsonify({
            "error": "Content agent not initialized"
        }), 503

    data = request.get_json()

    if not data:
        return jsonify({
            "error": "No JSON data provided"
        }), 400

    # Update configuration settings
    if 'lowQualityThreshold' in data:
        content_agent.low_quality_threshold = float(data['lowQualityThreshold'])

    if 'highQualityThreshold' in data:
        content_agent.high_quality_threshold = float(data['highQualityThreshold'])

    # Return updated configuration
    config = {
        "lowQualityThreshold": content_agent.low_quality_threshold,
        "highQualityThreshold": content_agent.high_quality_threshold,
        "modelName": content_agent.model_name
    }

    logger.info(f"Configuration updated: {config}")

    return jsonify(config)

if __name__ == '__main__':
    # Get configuration from environment
    host = os.environ.get('FLASK_HOST', '0.0.0.0')
    port = int(os.environ.get('FLASK_PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', 'True').lower() == 'true'

    # Validate configuration
    if not os.environ.get('GOOGLE_API_KEY'):
        logger.warning("GOOGLE_API_KEY not found in environment variables")

    # Start the Flask application
    logger.info(f"Starting Smart Threshold Marketing Agent Factory on {host}:{port}")
    logger.info(f"Debug mode: {debug}")
    logger.info(f"Content agent status: {'Ready' if content_agent else 'Not available'}")

    app.run(host=host, port=port, debug=debug)
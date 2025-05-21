"""
Enhanced Content Agent with Evaluator-Optimizer Workflow

This module implements a content generation agent that uses Google Gemini
to create marketing content through an iterative improvement process.
Updated to match Spring Boot backend expectations.
"""
import os
import time
import logging
from typing import Dict, Any, Optional, List
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold


class ContentAgent:
    """
    Content generation agent using Google Gemini with evaluator-optimizer workflow.

    The agent follows a three-step process:
    1. Generate initial content
    2. Evaluate the content quality
    3. Optimize if improvements are needed
    """

    def __init__(self):
        """Initialize the content agent with Gemini configuration."""
        self.logger = logging.getLogger(__name__)

        # Configure Gemini API
        api_key = os.environ.get('GOOGLE_API_KEY')
        if not api_key:
            raise ValueError("GOOGLE_API_KEY environment variable is required")

        genai.configure(api_key=api_key)

        # Set up the model
        self.model_name = os.environ.get('GEMINI_MODEL', 'gemini-1.5-flash')
        self.max_output_tokens = int(os.environ.get('GEMINI_MAX_OUTPUT_TOKENS', 2048))

        # Initialize the model with safety settings
        self.model = genai.GenerativeModel(
            model_name=self.model_name,
            safety_settings={
                HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
            }
        )

        # Generation configuration
        self.generation_config = genai.types.GenerationConfig(
            max_output_tokens=self.max_output_tokens,
            temperature=0.7,
            top_p=0.8,
            top_k=40
        )

        self.logger.info(f"ContentAgent initialized with model: {self.model_name}")

    def generate_content(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate content using the evaluator-optimizer workflow.

        Args:
            request_data: Dictionary with content specifications matching Spring Boot expectations

        Returns:
            Dictionary with generated content and metadata matching PythonGenerationResponse
        """
        start_time = time.time()

        # Extract parameters (matching Spring Boot request format)
        content_type = request_data.get('contentType')  # Note: camelCase
        brand_voice = request_data.get('brandVoice')
        topic = request_data.get('topic')
        platform = request_data.get('platform', 'general')
        target_audience = request_data.get('targetAudience', 'general audience')
        key_messages = request_data.get('keyMessages', [])
        brand_guidelines = request_data.get('brandGuidelines', '')
        additional_context = request_data.get('additionalContext', '')
        length_preference = request_data.get('lengthPreference', 'medium')
        include_hashtags = request_data.get('includeHashtags', False)
        call_to_action = request_data.get('callToAction', '')

        self.logger.info(f"Starting content generation: {content_type} about {topic}")

        try:
            # Step 1: Generate initial content
            self.logger.info("Step 1: Generating initial content...")
            initial_content = self._generate_initial_content(
                content_type, brand_voice, topic, platform, target_audience,
                key_messages, brand_guidelines, additional_context, length_preference,
                include_hashtags, call_to_action
            )

            # Step 2: Evaluate the content
            self.logger.info("Step 2: Evaluating content quality...")
            evaluation = self._evaluate_content(
                initial_content, content_type, brand_voice, platform, key_messages, brand_guidelines
            )

            # Step 3: Optimize if needed
            final_content = initial_content
            optimization_performed = False

            if evaluation.get('needsImprovement', False):
                self.logger.info("Step 3: Optimizing content based on evaluation...")
                final_content = self._optimize_content(
                    initial_content, evaluation, content_type, brand_voice, platform, brand_guidelines
                )
                optimization_performed = True
            else:
                self.logger.info("Step 3: Content quality acceptable, no optimization needed")

            # Calculate generation time
            generation_time = time.time() - start_time

            # Prepare result matching Spring Boot PythonGenerationResponse format
            result = {
                "content": final_content,
                "contentType": content_type,
                "platform": platform,
                "brandVoice": brand_voice,
                "targetAudience": target_audience,
                "generationTimeSeconds": round(generation_time, 2),
                "workflowSteps": {
                    "initialGeneration": True,
                    "evaluationPerformed": True,
                    "optimizationPerformed": optimization_performed
                },
                "evaluation": evaluation,
                "optimizationPerformed": optimization_performed,
                "suggestions": self._generate_suggestions(evaluation, optimization_performed),
                "estimatedMetrics": self._estimate_content_metrics(final_content, platform),
                "modelUsed": self.model_name,
                "metadata": {
                    "targetAudience": target_audience,
                    "keyMessagesIncluded": len(key_messages) > 0,
                    "brandGuidelinesApplied": bool(brand_guidelines),
                    "lengthPreference": length_preference,
                    "includeHashtags": include_hashtags
                }
            }

            self.logger.info(f"Content generation completed in {generation_time:.2f}s")
            return result

        except Exception as e:
            self.logger.error(f"Content generation failed: {str(e)}")
            raise

    def _generate_initial_content(self, content_type: str, brand_voice: str,
                                  topic: str, platform: str, target_audience: str,
                                  key_messages: List[str], brand_guidelines: str,
                                  additional_context: str, length_preference: str,
                                  include_hashtags: bool, call_to_action: str) -> str:
        """Generate initial content using Gemini with enhanced context."""

        # Build the generation prompt with all context
        prompt = self._build_enhanced_generation_prompt(
            content_type, brand_voice, topic, platform, target_audience,
            key_messages, brand_guidelines, additional_context, length_preference,
            include_hashtags, call_to_action
        )

        try:
            # Generate content using Gemini
            response = self.model.generate_content(
                prompt,
                generation_config=self.generation_config
            )

            # Extract text from response
            if response.candidates and len(response.candidates) > 0:
                content = response.candidates[0].content.parts[0].text
                return content.strip()
            else:
                raise ValueError("No content generated by the model")

        except Exception as e:
            self.logger.error(f"Initial content generation failed: {str(e)}")
            raise

    def _evaluate_content(self, content: str, content_type: str, brand_voice: str,
                          platform: str, key_messages: List[str], brand_guidelines: str) -> Dict[str, Any]:
        """Evaluate the generated content quality using the Anthropic evaluator pattern."""

        # Build comprehensive evaluation prompt
        evaluation_prompt = f"""
        You are an expert marketing content evaluator. Please thoroughly evaluate this {content_type} content:

        CONTENT TO EVALUATE:
        "{content}"

        EVALUATION CRITERIA:
        1. Brand voice alignment: Should match "{brand_voice}" tone and style
        2. Platform optimization: Appropriate for {platform} (format, length, style)
        3. Audience engagement: Will it resonate with the target audience?
        4. Clarity and readability: Is the message clear and easy to understand?
        5. Call-to-action effectiveness: Does it drive the desired action?
        6. Key message inclusion: {f"Should include: {', '.join(key_messages)}" if key_messages else "No specific requirements"}
        7. Brand guidelines compliance: {brand_guidelines if brand_guidelines else "No specific guidelines provided"}

        EVALUATION REQUIREMENTS:
        - Provide a quality score from 1-10 (10 = excellent, 7+ = good, <7 = needs improvement)
        - List 2-3 specific strengths
        - List 2-3 specific areas for improvement (even if score is high)
        - Recommend whether optimization is needed (score <7 = definitely needs optimization)

        RESPONSE FORMAT:
        SCORE: [number 1-10]
        STRENGTHS: [strength 1]; [strength 2]; [strength 3]
        IMPROVEMENTS: [improvement 1]; [improvement 2]; [improvement 3]
        NEEDS_OPTIMIZATION: [YES/NO]
        REASONING: [brief explanation of the score and recommendations]
        """

        try:
            response = self.model.generate_content(evaluation_prompt)
            evaluation_text = response.candidates[0].content.parts[0].text

            # Parse the evaluation with improved logic
            evaluation = self._parse_evaluation_response(evaluation_text)
            return evaluation

        except Exception as e:
            self.logger.error(f"Content evaluation failed: {str(e)}")
            # Return conservative evaluation if parsing fails
            return {
                "score": 6.5,
                "strengths": ["Content generated successfully"],
                "improvements": ["Consider refining the message", "Review platform optimization"],
                "needsImprovement": True,
                "rawEvaluation": f"Evaluation failed: {str(e)}"
            }

    def _optimize_content(self, original_content: str, evaluation: Dict[str, Any],
                          content_type: str, brand_voice: str, platform: str, brand_guidelines: str) -> str:
        """Optimize content based on evaluation feedback using the Anthropic optimizer pattern."""

        improvements = evaluation.get('improvements', [])
        if not improvements:
            return original_content

        optimization_prompt = f"""
        You are an expert marketing content optimizer. Please improve this {content_type} content based on the evaluation feedback.

        ORIGINAL CONTENT:
        "{original_content}"

        EVALUATION FEEDBACK:
        - Current Score: {evaluation.get('score', 'N/A')}/10
        - Strengths: {'; '.join(evaluation.get('strengths', []))}
        - Areas for Improvement: {'; '.join(improvements)}

        OPTIMIZATION REQUIREMENTS:
        - Maintain the {brand_voice} brand voice and tone
        - Optimize specifically for {platform} platform
        - Address each improvement point while preserving the strengths
        - Keep the core message and intent intact
        - Ensure the optimized version is significantly better than the original
        {f"- Follow brand guidelines: {brand_guidelines}" if brand_guidelines else ""}

        INSTRUCTIONS:
        Please provide ONLY the optimized content (no explanations or meta-commentary).
        The improved version should address the feedback while maintaining quality.
        """

        try:
            response = self.model.generate_content(optimization_prompt)
            optimized_content = response.candidates[0].content.parts[0].text

            # Clean up the response to ensure we only get the content
            optimized_content = optimized_content.strip()

            # Remove any quotation marks that might wrap the content
            if optimized_content.startswith('"') and optimized_content.endswith('"'):
                optimized_content = optimized_content[1:-1]

            return optimized_content

        except Exception as e:
            self.logger.error(f"Content optimization failed: {str(e)}")
            # Return original content if optimization fails
            return original_content

    def _build_enhanced_generation_prompt(self, content_type: str, brand_voice: str,
                                         topic: str, platform: str, target_audience: str,
                                         key_messages: List[str], brand_guidelines: str,
                                         additional_context: str, length_preference: str,
                                         include_hashtags: bool, call_to_action: str) -> str:
        """Build an enhanced content generation prompt with all available context."""

        # Content type specific instructions
        content_instructions = {
            "social_post": f"Create an engaging social media post for {platform}",
            "blog_post": "Write a comprehensive and engaging blog post",
            "ad_copy": f"Create compelling advertisement copy for {platform}",
            "email": "Write a professional email marketing message",
            "website_copy": "Create persuasive website copy that converts visitors",
            "product_description": "Write an compelling product description",
            "press_release": "Create a professional press release"
        }

        instruction = content_instructions.get(content_type, f"Create high-quality {content_type} content")

        prompt = f"""
        {instruction} about "{topic}" with the following specifications:

        BRAND VOICE: {brand_voice}
        TARGET AUDIENCE: {target_audience}
        PLATFORM: {platform}
        LENGTH PREFERENCE: {length_preference}
        """

        if key_messages:
            prompt += f"\nKEY MESSAGES TO INCLUDE: {', '.join(key_messages)}"

        if brand_guidelines:
            prompt += f"\nBRAND GUIDELINES: {brand_guidelines}"

        if additional_context:
            prompt += f"\nADDITIONAL CONTEXT: {additional_context}"

        if call_to_action:
            prompt += f"\nCALL TO ACTION: {call_to_action}"

        # Platform-specific guidelines
        platform_guidelines = {
            "twitter": "- Keep under 280 characters\n- Make it shareable and engaging\n- Use conversational tone",
            "linkedin": "- Professional yet engaging tone\n- Include thought leadership angle\n- Encourage professional discussion",
            "instagram": "- Visual-first approach\n- Lifestyle-focused language\n- Encourage engagement through questions",
            "facebook": "- Conversational and community-focused\n- Encourage shares and comments\n- Tell a story",
            "tiktok": "- Trendy and energetic tone\n- Appeal to younger demographics\n- Use current slang appropriately",
            "youtube": "- Hook viewers immediately\n- Maintain interest throughout\n- Include clear value proposition"
        }

        if platform.lower() in platform_guidelines:
            prompt += f"\nPLATFORM GUIDELINES for {platform}:\n{platform_guidelines[platform.lower()]}"

        if include_hashtags and platform.lower() in ["twitter", "instagram", "linkedin", "tiktok"]:
            prompt += "\n- Include 3-5 relevant hashtags at the end"

        # Length-specific instructions
        length_instructions = {
            "short": "Keep it concise and to the point (1-2 paragraphs max)",
            "medium": "Provide good detail while staying focused (2-4 paragraphs)",
            "long": "Create comprehensive content with thorough coverage (4+ paragraphs)"
        }

        if length_preference in length_instructions:
            prompt += f"\nLENGTH REQUIREMENT: {length_instructions[length_preference]}"

        prompt += f"\n\nCreate the {content_type} now:"

        return prompt

    def _parse_evaluation_response(self, evaluation_text: str) -> Dict[str, Any]:
        """Parse the evaluation response into structured data with improved logic."""

        evaluation = {
            "score": 7.0,  # Default score as float
            "strengths": [],
            "improvements": [],
            "needsImprovement": False,
            "rawEvaluation": evaluation_text
        }

        try:
            lines = evaluation_text.split('\n')

            for line in lines:
                line = line.strip()

                if line.startswith('SCORE:'):
                    # Extract score with better parsing
                    score_text = line.replace('SCORE:', '').strip()
                    try:
                        # Handle various formats: "8", "8/10", "8.5", etc.
                        score_str = score_text.split('/')[0].split(' ')[0]
                        score = float(score_str)
                        evaluation["score"] = min(max(score, 1), 10)  # Clamp between 1-10
                    except:
                        pass

                elif line.startswith('STRENGTHS:'):
                    # Extract strengths
                    strengths_text = line.replace('STRENGTHS:', '').strip()
                    if strengths_text:
                        evaluation["strengths"] = [s.strip() for s in strengths_text.split(';') if s.strip()]

                elif line.startswith('IMPROVEMENTS:'):
                    # Extract improvements
                    improvements_text = line.replace('IMPROVEMENTS:', '').strip()
                    if improvements_text and improvements_text.lower() not in ['none', 'n/a', 'no improvements needed']:
                        evaluation["improvements"] = [i.strip() for i in improvements_text.split(';') if i.strip()]

                elif line.startswith('NEEDS_OPTIMIZATION:'):
                    # Extract optimization flag
                    needs_opt = line.replace('NEEDS_OPTIMIZATION:', '').strip().upper()
                    evaluation["needsImprovement"] = needs_opt == 'YES'

            # Determine needs_improvement based on score if not explicitly set
            if evaluation["score"] < 7:
                evaluation["needsImprovement"] = True

            # Ensure we have some default improvements if score is low but none were provided
            if evaluation["needsImprovement"] and not evaluation["improvements"]:
                evaluation["improvements"] = ["Enhance clarity and engagement", "Strengthen the call to action"]

        except Exception as e:
            self.logger.error(f"Failed to parse evaluation: {str(e)}")

        return evaluation

    def _generate_suggestions(self, evaluation: Dict[str, Any], optimization_performed: bool) -> List[str]:
        """Generate actionable suggestions based on evaluation results."""

        suggestions = []

        if optimization_performed:
            suggestions.append("Content has been optimized based on evaluation feedback")

        if evaluation.get("score", 7) >= 8:
            suggestions.append("Consider A/B testing this high-quality content")
            suggestions.append("This content is ready for immediate use")
        elif evaluation.get("score", 7) >= 7:
            suggestions.append("Content quality is good - consider minor refinements")
            suggestions.append("Test with a small audience before full rollout")
        else:
            suggestions.append("Consider further refinement before publishing")
            suggestions.append("Focus on improving engagement elements")

        # Add specific suggestions based on improvements
        improvements = evaluation.get("improvements", [])
        if "engagement" in str(improvements).lower():
            suggestions.append("Add interactive elements like questions or polls")
        if "clarity" in str(improvements).lower():
            suggestions.append("Simplify language and structure for better readability")
        if "call to action" in str(improvements).lower():
            suggestions.append("Strengthen the call-to-action with urgency or incentives")

        return suggestions[:3]  # Limit to 3 suggestions

    def _estimate_content_metrics(self, content: str, platform: str) -> Dict[str, Any]:
        """Estimate content performance metrics based on best practices."""

        metrics = {}

        try:
            word_count = len(content.split())
            char_count = len(content)

            # Platform-specific metrics estimation
            if platform.lower() == "twitter":
                metrics["character_utilization"] = f"{char_count}/280"
                metrics["estimated_engagement"] = "Medium" if char_count < 100 else "High"
            elif platform.lower() == "linkedin":
                metrics["reading_time"] = f"{max(1, word_count // 200)} min"
                metrics["estimated_engagement"] = "High" if 50 <= word_count <= 300 else "Medium"
            elif platform.lower() == "instagram":
                hashtag_count = content.count('#')
                metrics["hashtag_count"] = hashtag_count
                metrics["estimated_reach"] = "High" if hashtag_count >= 3 else "Medium"

            # General metrics
            metrics["readability"] = "High" if word_count < 100 else "Medium" if word_count < 300 else "Low"
            metrics["content_length"] = "Short" if word_count < 50 else "Medium" if word_count < 200 else "Long"

        except Exception as e:
            self.logger.error(f"Failed to estimate metrics: {str(e)}")
            metrics["estimation_error"] = str(e)

        return metrics
"""
Content Agent with Evaluator-Optimizer Workflow

This module implements a content generation agent that uses Google Gemini
to create marketing content through an iterative improvement process.
"""
import os
import time
import logging
from typing import Dict, Any, Optional
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
            request_data: Dictionary with content specifications

        Returns:
            Dictionary with generated content and metadata
        """
        start_time = time.time()

        # Extract parameters
        content_type = request_data.get('content_type')
        brand_voice = request_data.get('brand_voice')
        topic = request_data.get('topic')
        platform = request_data.get('platform', 'general')
        target_audience = request_data.get('target_audience', 'general audience')
        key_messages = request_data.get('key_messages', [])

        self.logger.info(f"Starting content generation: {content_type} about {topic}")

        try:
            # Step 1: Generate initial content
            self.logger.info("Step 1: Generating initial content...")
            initial_content = self._generate_initial_content(
                content_type, brand_voice, topic, platform, target_audience, key_messages
            )

            # Step 2: Evaluate the content
            self.logger.info("Step 2: Evaluating content quality...")
            evaluation = self._evaluate_content(
                initial_content, content_type, brand_voice, platform, key_messages
            )

            # Step 3: Optimize if needed
            final_content = initial_content
            optimization_performed = False

            if evaluation.get('needs_improvement', False):
                self.logger.info("Step 3: Optimizing content based on evaluation...")
                final_content = self._optimize_content(
                    initial_content, evaluation, content_type, brand_voice, platform
                )
                optimization_performed = True
            else:
                self.logger.info("Step 3: Content quality acceptable, no optimization needed")

            # Calculate generation time
            generation_time = time.time() - start_time

            # Prepare result
            result = {
                "content": final_content,
                "content_type": content_type,
                "platform": platform,
                "brand_voice": brand_voice,
                "generation_time_seconds": round(generation_time, 2),
                "workflow_steps": {
                    "initial_generation": True,
                    "evaluation_performed": True,
                    "optimization_performed": optimization_performed
                },
                "evaluation": evaluation,
                "metadata": {
                    "model_used": self.model_name,
                    "target_audience": target_audience,
                    "key_messages_included": len(key_messages) > 0
                }
            }

            self.logger.info(f"Content generation completed in {generation_time:.2f}s")
            return result

        except Exception as e:
            self.logger.error(f"Content generation failed: {str(e)}")
            raise

    def _generate_initial_content(self, content_type: str, brand_voice: str,
                                  topic: str, platform: str, target_audience: str,
                                  key_messages: list) -> str:
        """Generate initial content using Gemini."""

        # Build the generation prompt
        prompt = self._build_generation_prompt(
            content_type, brand_voice, topic, platform, target_audience, key_messages
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
                          platform: str, key_messages: list) -> Dict[str, Any]:
        """Evaluate the generated content quality."""

        # Build evaluation prompt
        evaluation_prompt = f"""
        Please evaluate the following {content_type} content for quality and effectiveness:

        CONTENT TO EVALUATE:
        {content}

        EVALUATION CRITERIA:
        1. Brand voice alignment (should be {brand_voice})
        2. Platform appropriateness (for {platform})
        3. Clarity and engagement
        4. Call-to-action effectiveness
        5. Key message inclusion: {key_messages if key_messages else 'None specified'}

        Please provide:
        1. Overall quality score (1-10)
        2. Specific strengths
        3. Areas for improvement
        4. Whether optimization is needed (yes/no)

        Format your response as:
        SCORE: [1-10]
        STRENGTHS: [list strengths]
        IMPROVEMENTS: [list specific improvements needed]
        NEEDS_OPTIMIZATION: [YES/NO]
        """

        try:
            response = self.model.generate_content(evaluation_prompt)
            evaluation_text = response.candidates[0].content.parts[0].text

            # Parse the evaluation (simplified parsing)
            evaluation = self._parse_evaluation_response(evaluation_text)
            return evaluation

        except Exception as e:
            self.logger.error(f"Content evaluation failed: {str(e)}")
            # Return default evaluation if parsing fails
            return {
                "score": 7,
                "strengths": ["Content generated successfully"],
                "improvements": [],
                "needs_improvement": False,
                "raw_evaluation": str(e)
            }

    def _optimize_content(self, original_content: str, evaluation: Dict[str, Any],
                          content_type: str, brand_voice: str, platform: str) -> str:
        """Optimize content based on evaluation feedback."""

        improvements = evaluation.get('improvements', [])
        if not improvements:
            return original_content

        optimization_prompt = f"""
        Please improve the following {content_type} content based on the feedback provided:

        ORIGINAL CONTENT:
        {original_content}

        IMPROVEMENTS NEEDED:
        {'; '.join(improvements)}

        REQUIREMENTS:
        - Maintain {brand_voice} brand voice
        - Optimize for {platform} platform
        - Keep the core message intact
        - Apply the suggested improvements

        Please provide the improved version:
        """

        try:
            response = self.model.generate_content(optimization_prompt)
            optimized_content = response.candidates[0].content.parts[0].text
            return optimized_content.strip()

        except Exception as e:
            self.logger.error(f"Content optimization failed: {str(e)}")
            # Return original content if optimization fails
            return original_content

    def _build_generation_prompt(self, content_type: str, brand_voice: str,
                                 topic: str, platform: str, target_audience: str,
                                 key_messages: list) -> str:
        """Build the content generation prompt."""

        # Content type specific instructions
        content_instructions = {
            "social_post": f"Create an engaging social media post for {platform}",
            "blog_post": "Write a comprehensive blog post with clear structure",
            "ad_copy": f"Create compelling advertisement copy for {platform}",
            "email": "Write a professional email marketing message",
            "website_copy": "Create website copy that converts visitors"
        }

        instruction = content_instructions.get(content_type, f"Create {content_type} content")

        prompt = f"""
        {instruction} about "{topic}" with the following specifications:

        BRAND VOICE: {brand_voice}
        TARGET AUDIENCE: {target_audience}
        PLATFORM: {platform}
        """

        if key_messages:
            prompt += f"\nKEY MESSAGES TO INCLUDE: {', '.join(key_messages)}"

        # Platform-specific guidelines
        if platform == "twitter":
            prompt += "\n- Keep under 280 characters\n- Include relevant hashtags"
        elif platform == "linkedin":
            prompt += "\n- Professional tone\n- Include a call-to-action\n- Ask an engaging question"
        elif platform == "instagram":
            prompt += "\n- Visual-focused copy\n- Include relevant hashtags\n- Encourage engagement"
        elif platform == "facebook":
            prompt += "\n- Conversational tone\n- Encourage comments and shares"

        prompt += f"\n\nGenerate high-quality {content_type} content now:"

        return prompt

    def _parse_evaluation_response(self, evaluation_text: str) -> Dict[str, Any]:
        """Parse the evaluation response into structured data."""

        evaluation = {
            "score": 7,  # Default score
            "strengths": [],
            "improvements": [],
            "needs_improvement": False,
            "raw_evaluation": evaluation_text
        }

        try:
            lines = evaluation_text.split('\n')

            for line in lines:
                line = line.strip()

                if line.startswith('SCORE:'):
                    # Extract score
                    score_text = line.replace('SCORE:', '').strip()
                    try:
                        score = int(score_text.split('/')[0])  # Handle "8/10" format
                        evaluation["score"] = min(max(score, 1), 10)  # Clamp between 1-10
                    except:
                        pass

                elif line.startswith('STRENGTHS:'):
                    # Extract strengths
                    strengths_text = line.replace('STRENGTHS:', '').strip()
                    if strengths_text:
                        evaluation["strengths"] = [s.strip() for s in strengths_text.split(';')]

                elif line.startswith('IMPROVEMENTS:'):
                    # Extract improvements
                    improvements_text = line.replace('IMPROVEMENTS:', '').strip()
                    if improvements_text and improvements_text.lower() not in ['none', 'n/a']:
                        evaluation["improvements"] = [i.strip() for i in improvements_text.split(';')]

                elif line.startswith('NEEDS_OPTIMIZATION:'):
                    # Extract optimization flag
                    needs_opt = line.replace('NEEDS_OPTIMIZATION:', '').strip().upper()
                    evaluation["needs_improvement"] = needs_opt == 'YES'

            # Set needs_improvement based on score if not explicitly set
            if evaluation["score"] < 7:
                evaluation["needs_improvement"] = True

        except Exception as e:
            self.logger.error(f"Failed to parse evaluation: {str(e)}")

        return evaluation
"""
Smart Threshold Content Agent with Evaluator-Optimizer Workflow

This module implements a content generation agent that uses Google Gemini
to create marketing content with intelligent evaluation and targeted optimization.
"""
import os
import time
import logging
from typing import Dict, Any, List, Optional, Tuple
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold


class ContentAgent:
    """
    Content generation agent using Google Gemini with smart threshold evaluator-optimizer workflow.

    The agent follows these steps:
    1. Generate initial content
    2. Evaluate the content quality
    3. Decide optimization strategy based on score thresholds:
       - Below 7.0: Full optimization
       - 7.0-8.5 with specific weaknesses: Targeted optimization
       - Above 8.5: No optimization needed
    4. Return both versions when optimization occurs
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
        self.model_name = os.environ.get('GEMINI_MODEL', 'gemini-2.0-flash')
        self.max_output_tokens = int(os.environ.get('GEMINI_MAX_OUTPUT_TOKENS', 2048))

        # Optimization thresholds
        self.low_quality_threshold = float(os.environ.get('LOW_QUALITY_THRESHOLD', 7.0))
        self.high_quality_threshold = float(os.environ.get('HIGH_QUALITY_THRESHOLD', 8.5))

        # Initialize the model with safety settings
        safety_settings = [
            {
                "category": HarmCategory.HARM_CATEGORY_HATE_SPEECH,
                "threshold": HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            },
            {
                "category": HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
                "threshold": HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            },
            {
                "category": HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
                "threshold": HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            },
            {
                "category": HarmCategory.HARM_CATEGORY_HARASSMENT,
                "threshold": HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            }
        ]

        self.model = genai.GenerativeModel(
            model_name=self.model_name,
            safety_settings=safety_settings
        )

        # Generation configuration
        self.generation_config = genai.types.GenerationConfig(
            max_output_tokens=self.max_output_tokens,
            temperature=0.7,
            top_p=0.8,
            top_k=40
        )

        self.logger.info(f"ContentAgent initialized with model: {self.model_name}")
        self.logger.info(f"Smart threshold settings: low={self.low_quality_threshold}, high={self.high_quality_threshold}")

    def generate_content(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate content using the smart threshold evaluator-optimizer workflow.

        Args:
            request_data: Dictionary with content specifications

        Returns:
            Dictionary with generated content, metadata, and optimization data when performed
        """
        start_time = time.time()

        # Extract parameters (matching Spring Boot request format)
        content_type = request_data.get('contentType')
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

            initial_generation_time = time.time() - start_time

            # Step 2: Evaluate initial content
            self.logger.info("Step 2: Evaluating content quality...")
            evaluation = self._evaluate_content(
                initial_content, content_type, brand_voice, platform, key_messages, brand_guidelines
            )

            # Set this variable to track that evaluation was performed
            evaluation_performed = True

            score = evaluation.get('score', 0.0)
            self.logger.info(f"Content evaluation score: {score}/10")

            # Step 3: Decide optimization strategy based on score
            optimization_performed = False
            optimization_type = "none"
            optimized_content = initial_content
            optimization_details = None

            # Case 1: Low score - needs thorough optimization
            if score < self.low_quality_threshold:
                self.logger.info(f"Score below {self.low_quality_threshold} - performing full optimization")
                optimization_start_time = time.time()
                optimized_content, optimization_details = self._optimize_content(
                    initial_content, evaluation, content_type, brand_voice, platform, brand_guidelines,
                    optimization_type="full"
                )
                optimization_time = time.time() - optimization_start_time
                optimization_performed = True
                optimization_type = "full"

            # Case 2: Medium score with specific weaknesses - targeted optimization
            elif score < self.high_quality_threshold and evaluation.get('improvements', []):
                self.logger.info(f"Score between {self.low_quality_threshold}-{self.high_quality_threshold} " 
                               f"with specific weaknesses - performing targeted optimization")
                optimization_start_time = time.time()
                optimized_content, optimization_details = self._optimize_content(
                    initial_content, evaluation, content_type, brand_voice, platform, brand_guidelines,
                    optimization_type="targeted"
                )
                optimization_time = time.time() - optimization_start_time
                optimization_performed = True
                optimization_type = "targeted"

            # Case 3: High score - no optimization needed
            else:
                self.logger.info(f"Score above {self.high_quality_threshold} - no optimization needed")
                optimization_time = 0

            # Step 4: If optimization occurred, re-evaluate the optimized content
            if optimization_performed:
                self.logger.info("Re-evaluating optimized content...")
                optimized_evaluation = self._evaluate_content(
                    optimized_content, content_type, brand_voice, platform, key_messages, brand_guidelines
                )

                # Store both evaluations
                evaluation_comparison = {
                    "initialScore": score,
                    "optimizedScore": optimized_evaluation.get('score', 0.0),
                    "scoreDifference": optimized_evaluation.get('score', 0.0) - score,
                    "initialEvaluation": evaluation,
                    "optimizedEvaluation": optimized_evaluation
                }

                self.logger.info(f"Optimization improved score from {score} to {optimized_evaluation.get('score', 0.0)}")
            else:
                evaluation_comparison = None
                optimized_evaluation = None

                # Calculate total generation time
            generation_time = time.time() - start_time

            # Prepare result
            # Near the end of the generate_content method, where the result is created
            result = {
                "content": optimized_content if optimization_performed else initial_content,
                "contentType": content_type,
                "platform": platform,
                "brandVoice": brand_voice,
                "targetAudience": target_audience,
                "generationTimeSeconds": round(generation_time, 2),
                "workflowInfo": {
                    "initialGenerationCompleted": True,
                    "evaluationPerformed": True,  # Force this to True always
                    "evaluationScore": score,
                    "optimizationPerformed": optimization_performed,
                    "optimizationType": optimization_type,
                    "optimizationIterations": 1 if optimization_performed else 0,
                    "modelUsed": self.model_name
                },
                "evaluationDetails": {
                    "criteriaScores": evaluation.get('criteriaScores', {}),
                    "strengths": evaluation.get('strengths', []),
                    "improvements": evaluation.get('improvements', [])
                },
                "suggestions": self._generate_suggestions(evaluation, optimization_performed),
                "estimatedMetrics": self._estimate_content_metrics(
                    optimized_content if optimization_performed else initial_content, platform),
                "modelUsed": self.model_name,
            }

            # Add debug logging
            self.logger.info("Final result structure to be returned:")
            self.logger.info(f"evaluationPerformed: {result['workflowInfo']['evaluationPerformed']}")
            self.logger.info(f"evaluationScore: {result['workflowInfo']['evaluationScore']}")
            self.logger.info(f"optimizationPerformed: {result['workflowInfo']['optimizationPerformed']}")

            # After optimization and re-evaluation, update the result with optimized score
            if optimization_performed and optimized_evaluation:
                result["workflowInfo"]["evaluationScore"] = optimized_evaluation.get('score', 0.0)

            # Add optimization data if performed
            if optimization_performed:
                result["optimizationDetails"] = {
                    "initialContent": initial_content,
                    "optimizedContent": optimized_content,
                    "optimizationType": optimization_type,
                    "optimizationTime": round(optimization_time, 2),
                    "initialGenerationTime": round(initial_generation_time, 2),
                    "improvementReason": optimization_details,
                    "evaluationComparison": evaluation_comparison
                }

            self.logger.info(f"Content generation completed in {generation_time:.2f}s")
            self.logger.info("Final result structure to be returned:")
            self.logger.info(f"evaluationPerformed: {result['workflowInfo']['evaluationPerformed']}")
            self.logger.info(f"evaluationScore: {result['workflowInfo']['evaluationScore']}")
            self.logger.info(f"optimizationPerformed: {result['workflowInfo']['optimizationPerformed']}")
            return result

        except Exception as e:
            self.logger.error(f"Content generation failed: {str(e)}", exc_info=True)
            raise

    def _generate_initial_content(self, content_type: str, brand_voice: str,
                                  topic: str, platform: str, target_audience: str,
                                  key_messages: List[str], brand_guidelines: str,
                                  additional_context: str, length_preference: str,
                                  include_hashtags: bool, call_to_action: str) -> str:
        """Generate initial content using Gemini."""

        # Build the generation prompt with all context
        prompt = self._build_generation_prompt(
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
        """Evaluate the generated content quality using comprehensive criteria."""

        # Build detailed evaluation prompt
        evaluation_prompt = f"""
        You are an expert marketing content evaluator with years of experience in digital marketing. 
        Please thoroughly evaluate this {content_type} content based on multiple critical criteria:

        CONTENT TO EVALUATE:
        ```
        {content}
        ```

        EVALUATION CRITERIA (score each from 1-10):
        1. Brand voice alignment: How well does it match the "{brand_voice}" tone and style?
        2. Platform optimization: How appropriate is it for {platform} in format, length, and style?
        3. Key message inclusion: How effectively does it incorporate these key messages: {', '.join(key_messages) if key_messages else 'No specific messages required'}
        4. Audience engagement: How likely is it to resonate with {platform} users in the target audience?
        5. Clarity and readability: How clear, concise and easy to understand is the content?
        6. Call-to-action effectiveness: How compelling is the call to action?
        7. Overall content quality: Considering all factors, how strong is this content?
        {f"8. Brand guidelines compliance: How well does it follow these guidelines: {brand_guidelines}" if brand_guidelines else ""}

        EVALUATION REQUIREMENTS:
        - Provide a detailed assessment for each criterion with specific examples
        - Calculate an overall quality score from 1-10 (weighted average of all criteria)
        - List 2-3 specific strengths with examples from the content
        - List 2-3 specific areas for improvement with concrete suggestions (even for high-scoring content)
        - Recommend whether optimization is needed (typically if score < 8.5 or if there are critical weaknesses)

        RESPONSE FORMAT:
        CRITERIA_SCORES:
        - Brand Voice: [score/10] - [brief assessment]
        - Platform Fit: [score/10] - [brief assessment]
        - Key Messages: [score/10] - [brief assessment]
        - Audience Engagement: [score/10] - [brief assessment]
        - Clarity: [score/10] - [brief assessment]
        - Call to Action: [score/10] - [brief assessment]
        - Overall Quality: [score/10] - [brief assessment]
        {f"- Brand Guidelines: [score/10] - [brief assessment]" if brand_guidelines else ""}

        OVERALL_SCORE: [number 1-10, calculated as weighted average]

        STRENGTHS: 
        1. [strength with specific example]
        2. [strength with specific example]
        3. [optional additional strength]

        IMPROVEMENTS:
        1. [improvement needed with specific suggestion]
        2. [improvement needed with specific suggestion]
        3. [optional additional improvement]

        NEEDS_OPTIMIZATION: [YES/NO]

        OPTIMIZATION_GUIDANCE: [specific guidance for improving the content]
        """

        try:
            response = self.model.generate_content(evaluation_prompt)
            evaluation_text = response.candidates[0].content.parts[0].text

            # Parse the evaluation with improved logic
            evaluation = self._parse_evaluation_response(evaluation_text)

            # Add the raw evaluation text for debugging
            evaluation['rawEvaluation'] = evaluation_text

            return evaluation

        except Exception as e:
            self.logger.error(f"Content evaluation failed: {str(e)}")
            # Return default evaluation if parsing fails
            return {
                "score": 6.5,
                "criteriaScores": {
                    "brandVoice": 6.5,
                    "platformFit": 6.5,
                    "keyMessages": 6.5,
                    "audienceEngagement": 6.5,
                    "clarity": 6.5,
                    "callToAction": 6.5,
                    "overallQuality": 6.5
                },
                "strengths": ["Content generated successfully"],
                "improvements": ["Refine messaging to better match brand voice", "Improve clarity for target audience"],
                "needsImprovement": True,
                "optimizationGuidance": "Revise to better capture the brand voice and clarify messaging",
                "rawEvaluation": f"Evaluation failed: {str(e)}"
            }

    def _optimize_content(self, original_content: str, evaluation: Dict[str, Any],
                          content_type: str, brand_voice: str, platform: str,
                          brand_guidelines: str, optimization_type: str = "full") -> Tuple[str, str]:
        """Optimize content based on evaluation feedback with specific improvements."""

        improvements = evaluation.get('improvements', [])
        optimization_guidance = evaluation.get('optimizationGuidance', '')

        if not improvements and not optimization_guidance:
            return original_content, "No improvements needed"

        # Different prompt based on optimization type
        if optimization_type == "targeted":
            # Targeted optimization focuses only on specific weak areas
            optimization_prompt = f"""
            You are an expert content optimizer for {platform}. Your task is to perform a TARGETED optimization 
            of this {content_type} content, focusing ONLY on specific areas while preserving everything else.
            
            ORIGINAL CONTENT:
            ```
            {original_content}
            ```

            FOCUSED IMPROVEMENT AREAS:
            {'; '.join(improvements)}
            
            TARGETED OPTIMIZATION INSTRUCTIONS:
            - Make MINIMAL changes, focusing ONLY on the specified improvement areas
            - Preserve ALL strengths of the original: {'; '.join(evaluation.get('strengths', []))}
            - Maintain the exact same structure, length, and overall approach
            - Do NOT change any parts that are already working well
            - Optimize specifically for {platform} and its audience
            - Maintain the {brand_voice} brand voice throughout
            {f"- Follow brand guidelines: {brand_guidelines}" if brand_guidelines else ""}

            IMPORTANT:
            - Provide ONLY the optimized content (no explanations or commentary)
            - Make strategic, targeted improvements rather than wholesale changes
            
            First, briefly describe your targeted optimization strategy.
            Then, provide the improved content.
            """
        else:
            # Full optimization can rework the entire content
            optimization_prompt = f"""
            You are an expert content optimizer for {platform}. Please perform a COMPREHENSIVE optimization 
            of this {content_type} content based on the evaluation feedback.
            
            ORIGINAL CONTENT:
            ```
            {original_content}
            ```

            EVALUATION FEEDBACK:
            - Current Score: {evaluation.get('score', 'N/A')}/10
            - Strengths: {'; '.join(evaluation.get('strengths', []))}
            - Areas for Improvement: {'; '.join(improvements)}
            
            DETAILED OPTIMIZATION GUIDANCE:
            {optimization_guidance}

            OPTIMIZATION REQUIREMENTS:
            - Create significantly improved content that addresses ALL improvement areas
            - Maintain the {brand_voice} brand voice throughout
            - Optimize specifically for {platform} and its audience
            - Address each improvement point while preserving the existing strengths
            - Make the content more engaging and impactful
            - Ensure the optimized version is significantly better than the original
            {f"- Follow brand guidelines: {brand_guidelines}" if brand_guidelines else ""}

            IMPORTANT:
            - Provide ONLY the optimized content (no explanations or commentary)
            - Maintain approximately the same length as the original
            
            First, describe your optimization strategy.
            Then, provide the improved content.
            ```
            OPTIMIZED CONTENT:
            [your optimized content here]
            ```
            """

        try:
            response = self.model.generate_content(optimization_prompt)
            full_response = response.candidates[0].content.parts[0].text

            # Extract the optimization reasoning and the actual content
            parts = full_response.split("```")

            # If the model properly wrapped the content in ```
            if len(parts) >= 3:
                reasoning = parts[0].strip()
                optimized_content = parts[1].strip()
            else:
                # If the model didn't follow the format, try to find the strategy vs. content
                lines = full_response.split("\n")
                plan_end_idx = -1

                # Look for likely boundaries between strategy and content
                for i, line in enumerate(lines):
                    if any(marker in line.upper() for marker in [
                        "OPTIMIZED CONTENT:", "IMPROVED CONTENT:",
                        "ENHANCED CONTENT:", "FINAL CONTENT:"
                    ]):
                        plan_end_idx = i
                        break

                if plan_end_idx > 0:
                    reasoning = "\n".join(lines[:plan_end_idx]).strip()
                    optimized_content = "\n".join(lines[plan_end_idx+1:]).strip()
                else:
                    # Default to using the whole response as content with generic reasoning
                    reasoning = f"{optimization_type.capitalize()} optimization performed"
                    optimized_content = full_response.strip()

            return optimized_content, reasoning

        except Exception as e:
            self.logger.error(f"Content optimization failed: {str(e)}")
            # Return original content with error explanation if optimization fails
            return original_content, f"Optimization failed: {str(e)}"

    def _build_generation_prompt(self, content_type: str, brand_voice: str,
                                 topic: str, platform: str, target_audience: str,
                                 key_messages: List[str], brand_guidelines: str,
                                 additional_context: str, length_preference: str,
                                 include_hashtags: bool, call_to_action: str) -> str:
        """Build a detailed content generation prompt with comprehensive instructions."""

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
        You are an expert marketing content creator specializing in creating high-performing content for various platforms.
        
        TASK:
        {instruction} about "{topic}" with the following specifications:

        KEY SPECIFICATIONS:
        - BRAND VOICE: {brand_voice}
        - TARGET AUDIENCE: {target_audience}
        - PLATFORM: {platform}
        - LENGTH PREFERENCE: {length_preference}
        """

        if key_messages:
            prompt += f"""
        KEY MESSAGES TO INCLUDE (MUST cover ALL of these):
        {", ".join(f"- {message}" for message in key_messages)}
        """

        if brand_guidelines:
            prompt += f"""
        BRAND GUIDELINES TO FOLLOW:
        {brand_guidelines}
        """

        if additional_context:
            prompt += f"""
        ADDITIONAL CONTEXT:
        {additional_context}
        """

        if call_to_action:
            prompt += f"""
        CALL TO ACTION:
        {call_to_action}
        """

        # Platform-specific guidelines
        platform_guidelines = {
            "twitter": "- Keep under 280 characters\n- Make it shareable and engaging\n- Use conversational tone",
            "linkedin": "- Professional yet engaging tone\n- Include thought leadership angle\n- Encourage professional discussion\n- Format with clear headline and body structure",
            "instagram": "- Visual-first approach\n- Lifestyle-focused language\n- Encourage engagement through questions",
            "facebook": "- Conversational and community-focused\n- Encourage shares and comments\n- Tell a story",
            "tiktok": "- Trendy and energetic tone\n- Appeal to younger demographics\n- Use current slang appropriately",
            "youtube": "- Hook viewers immediately\n- Maintain interest throughout\n- Include clear value proposition"
        }

        if platform.lower() in platform_guidelines:
            prompt += f"""
        PLATFORM-SPECIFIC GUIDELINES for {platform}:
        {platform_guidelines[platform.lower()]}
        """

        if include_hashtags and platform.lower() in ["twitter", "instagram", "linkedin", "tiktok"]:
            prompt += "\n- Include 3-5 relevant hashtags at the end"

        # Length-specific instructions
        length_instructions = {
            "short": "Keep it concise and to the point (1-2 paragraphs max)",
            "medium": "Provide good detail while staying focused (2-4 paragraphs)",
            "long": "Create comprehensive content with thorough coverage (4+ paragraphs)"
        }

        if length_preference in length_instructions:
            prompt += f"""
        LENGTH REQUIREMENT:
        {length_instructions[length_preference]}
        """

        prompt += """
        QUALITY EXPECTATIONS:
        - Compelling and attention-grabbing
        - Clear, concise, and easy to understand
        - Persuasive and action-oriented
        - Authentic to the brand voice
        - Optimized for the platform's best practices
        - Addresses all key messages naturally
        
        Now, create the content that perfectly meets all these requirements:
        """

        return prompt

    def _parse_evaluation_response(self, evaluation_text: str) -> Dict[str, Any]:
        """Parse the evaluation response into structured data with comprehensive criteria."""

        evaluation = {
            "score": 7.0,
            "criteriaScores": {},
            "strengths": [],
            "improvements": [],
            "needsImprovement": False,
            "optimizationGuidance": ""
        }

        try:
            lines = evaluation_text.split('\n')
            section = ""

            criteria_scores = {}

            for line in lines:
                line = line.strip()

                # Detect section changes
                if line.startswith("CRITERIA_SCORES:"):
                    section = "criteria"
                    continue
                elif line.startswith("OVERALL_SCORE:"):
                    section = "score"
                    score_text = line.replace("OVERALL_SCORE:", "").strip()
                    try:
                        evaluation["score"] = float(score_text.split()[0])
                    except:
                        pass
                    continue
                elif line.startswith("STRENGTHS:"):
                    section = "strengths"
                    continue
                elif line.startswith("IMPROVEMENTS:"):
                    section = "improvements"
                    continue
                elif line.startswith("NEEDS_OPTIMIZATION:"):
                    section = "needs_optimization"
                    needs_opt = line.replace("NEEDS_OPTIMIZATION:", "").strip().upper()
                    evaluation["needsImprovement"] = needs_opt == 'YES'
                    continue
                elif line.startswith("OPTIMIZATION_GUIDANCE:"):
                    section = "optimization_guidance"
                    evaluation["optimizationGuidance"] = line.replace("OPTIMIZATION_GUIDANCE:", "").strip()
                    continue

                # Parse content based on current section
                if section == "criteria" and line.startswith("-"):
                    # Parse criteria scores
                    parts = line.strip("- ").split(":", 1)
                    if len(parts) == 2:
                        criterion_name = parts[0].strip()
                        score_parts = parts[1].strip().split("-", 1)
                        if len(score_parts) >= 1:
                            try:
                                score = float(score_parts[0].replace("/10", "").strip())
                                # Convert to camelCase for Java compatibility
                                key = ''.join([
                                    criterion_name.split()[0].lower(),
                                    ''.join(word.capitalize() for word in criterion_name.split()[1:])
                                ])
                                criteria_scores[key] = score
                            except:
                                pass

                elif section == "strengths" and line.startswith(("1.", "2.", "3.")):
                    # Extract strengths
                    strength = line.split(".", 1)
                    if len(strength) > 1 and strength[1].strip():
                        evaluation["strengths"].append(strength[1].strip())

                elif section == "improvements" and line.startswith(("1.", "2.", "3.")):
                    # Extract improvements
                    improvement = line.split(".", 1)
                    if len(improvement) > 1 and improvement[1].strip():
                        evaluation["improvements"].append(improvement[1].strip())

                elif section == "optimization_guidance" and line:
                    # Append to optimization guidance
                    if evaluation["optimizationGuidance"]:
                        evaluation["optimizationGuidance"] += " " + line
                    else:
                        evaluation["optimizationGuidance"] = line

            # Add criteria scores to evaluation
            evaluation["criteriaScores"] = criteria_scores

            # Set needs improvement based on score if not explicitly set
            if evaluation["score"] < 7:
                evaluation["needsImprovement"] = True

        except Exception as e:
            self.logger.error(f"Failed to parse evaluation: {str(e)}")

        return evaluation

    def _generate_suggestions(self, evaluation: Dict[str, Any], optimization_performed: bool) -> List[str]:
        """Generate actionable suggestions based on evaluation results."""

        suggestions = []

        score = evaluation.get("score", 7)

        if optimization_performed:
            suggestions.append("Content has been optimized based on expert evaluation")

        if score >= 8.5:
            suggestions.append("This high-quality content is ready for immediate use")
        elif score >= 7:
            suggestions.append("Consider minor refinements before publishing")
        else:
            suggestions.append("Review the optimized content before publishing")

        # Add platform-specific suggestions
        improvements = evaluation.get("improvements", [])

        if any("engagement" in str(imp).lower() for imp in improvements):
            suggestions.append("Add interactive elements like questions or polls")
        if any("call to action" in str(imp).lower() for imp in improvements):
            suggestions.append("Strengthen the call-to-action with urgency or incentives")
        if any("clarity" in str(imp).lower() for imp in improvements):
            suggestions.append("Simplify language for better readability")

        # Add A/B testing suggestion for high-quality content
        if score >= 8:
            suggestions.append("Consider A/B testing this high-quality content")

        return suggestions[:3]  # Limit to 3 suggestions

    def _estimate_content_metrics(self, content: str, platform: str) -> Dict[str, Any]:
        """Estimate content performance metrics based on platform best practices."""

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
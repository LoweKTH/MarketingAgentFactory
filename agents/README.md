# Marketing Agent Factory - Python Content Agent

A simplified content generation service using Google Gemini for text-based marketing content with an evaluator-optimizer workflow.

## LLM Integration

This service uses **Google Gemini** as the primary LLM:
- **Free tier**: 15 requests per minute, 1 million tokens per day
- **Model**: Gemini 1.5 Flash (fast and efficient)
- **API**: Google AI Studio REST API

### Getting Your API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Add it to your `.env` file as `GOOGLE_API_KEY`

### Gemini vs Other LLMs

**Why Gemini for MVP:**
- ✅ Generous free tier
- ✅ Fast response times
- ✅ Good content generation quality
- ✅ Easy to integrate

**Future Options:**
- Anthropic Claude (paid, excellent quality)
- OpenAI GPT (paid, widely used)
- Local models (Ollama, etc.)
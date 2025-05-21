// src/components/SocialPostGenerator.js
import React, { useState } from 'react';
import './SocialPostGenerator.css'; // For overall layout of this component

import PromptInput from '../components/PromptInput';
import PostFormatSelector from '../components/PostFormatSelector';
import GenerateButton from '../components/GenerateButton';  // no braces, default import
import { generateSocialPost } from '../api/ContentGenerator-api';


const SocialPostGenerator = () => {

    const [prompt, setPrompt] = useState('');
    // State for API call status
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [generatedContent, setGeneratedContent] = useState(null);

  const handleGeneratePost = async () => {
    // Clear previous states
        setGeneratedContent(null);
        setError(null);

        // Basic validation: ensure prompt is not empty
        if (!prompt.trim()) {
            setError("Please enter a prompt before generating.");
            return;
        }

        setIsLoading(true); // Set loading state to true

        // Hardcode the payload as requested, using 'prompt' for the topic
        const apiPayload = {
            contentType: "social_post",
            brandVoice: "professional",
            topic: prompt, // This is where the prompt input goes
            platform: "linkedin", // Hardcoded
            targetAudience: "business professionals", // Hardcoded
            keyMessages: ["time management", "work-life balance"] // Hardcoded
        };

        try {
            const result = await generateSocialPost(apiPayload);
            setGeneratedContent(result); // Store the API response
            console.log("API Response:", result);
        } catch (err) {
            console.error("Failed to generate post:", err);
            setError(err.message || "An unexpected error occurred during API call.");
        } finally {
            setIsLoading(false); // Reset loading state
        }
  };

  return (
        <div className="social-post-generator-container">
            <h2 className="main-title">Create Your Social Media Post</h2>

            {/* Pass prompt value and setter to PromptInput */}
            <PromptInput value={prompt} onChange={setPrompt} />

            {/* PostFormatSelector is currently not connected to state, but would be in a full app */}
            <PostFormatSelector />

            {/* You'll likely want to add the Trending Topics section here too */}
            {/* <div className="trending-topics-container">...</div> */}

            <div className="footer-controls">
                {/* Pass isLoading prop to disable button while loading */}
                <GenerateButton onClick={handleGeneratePost} isLoading={isLoading} />
            </div>

            {/* Display loading, error, or generated content feedback */}
            {isLoading && (
                <div style={{ textAlign: 'center', marginTop: '20px', color: '#1976d2' }}>
                    <i className="fas fa-spinner fa-spin"></i> Generating post...
                </div>
            )}
            {error && (
                <div style={{ textAlign: 'center', marginTop: '20px', color: 'red' }}>
                    Error: {error}
                </div>
            )}
            {generatedContent && (
                <div style={{ marginTop: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '8px', backgroundColor: '#e9f5ff' }}>
                    <h3>Generated Content:</h3>
                    {/* Assuming the API returns a 'post' field or similar; adjust as per your actual API response structure */}
                    <p>{generatedContent.post || JSON.stringify(generatedContent, null, 2)}</p>
                </div>
            )}
        </div>
    );
};

export default SocialPostGenerator;
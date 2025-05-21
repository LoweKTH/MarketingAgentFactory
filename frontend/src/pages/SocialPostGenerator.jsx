// src/components/SocialPostGenerator.js
import React, { useState } from 'react';
import './SocialPostGenerator.css';

import PromptInput from '../components/PromptInput';
import PostFormatSelector from '../components/PostFormatSelector';
import PublishButton from '../components/PublishButton';
import PublishModal from '../components/PublishModal';

import { generateSocialPost } from '../api/ContentGenerator-api'; // Make sure the path is correct

import GenerateButton from '../components/GenerateButton';  // no braces, default import
import ContentEvaluation from '../components/ContentEvaluation'; // Import our new component
import { generateSocialPost } from '../api/ContentGenerator-api';

const SocialPostGenerator = () => {
    const [prompt, setPrompt] = useState('');
    const [selectedPlatform, setSelectedPlatform] = useState('linkedin'); // New state for selected platform, default to 'linkedin'
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [generatedContent, setGeneratedContent] = useState(null);
    const [showPublishModal, setShowPublishModal] = useState(false);

    const handleGeneratePost = async () => {
        // Clear previous states
        setGeneratedContent(null);
        setError(null);

        if (!prompt.trim()) {
            setError("Please enter a prompt before generating.");
            return;
        }

        setIsLoading(true);

        const apiPayload = {
            contentType: "social_post",
            brandVoice: "professional",
            topic: prompt,
            platform: selectedPlatform, // <-- Now uses the state variable
            targetAudience: "business professionals",
            keyMessages: ["time management", "work-life balance"]
        };

        try {
            const result = await generateSocialPost(apiPayload);
            setGeneratedContent(result.data || result); // Store the API response, handle different response structures
            console.log("API Response:", result);
        } catch (err) {
            console.error("Failed to generate post:", err);
            setError(err.message || "An unexpected error occurred during API call.");
        } finally {
            setIsLoading(false);
        }
    };

    const handlePublishClick = () => {
        if (generatedContent && generatedContent.data.content) {
            setShowPublishModal(true); // Show the modal
        } else {
            alert("Please generate content first before publishing.");
        }
    };

    const handlePublishConfirm = (platforms, content, recurringOptions) => {
        console.log("--- PUBLISH CONFIRMATION (Conceptual) ---");
        console.log("Content to publish:", content.post);
        console.log("Platforms selected:", platforms);

        if (recurringOptions) {
            console.log("This will be scheduled as a recurring post!");
            console.log("Frequency:", recurringOptions.frequencyHours, "hours");
            console.log("Action: Conceptually send this data to a backend '/api/scheduled-posts' endpoint.");
        } else {
            console.log("This will be published immediately!");
            console.log("Action: Conceptually send this data to a backend '/api/publish/immediate' endpoint for each platform.");
        }
        console.log("---------------------------------------");

        alert(`Publishing simulated for: ${platforms.join(', ')}.\n${recurringOptions ? `(Recurring every ${recurringOptions.frequencyHours} hours)` : '(Immediate Publish)'}\nCheck console for details.`);
        setShowPublishModal(false); // Close the modal
    };


    return (
        <div className="social-post-generator-container">
            <h2 className="main-title">Create Your Social Media Post</h2>

            <PromptInput value={prompt} onChange={setPrompt} />

            {/* Pass selectedPlatform and the setter function down to PostFormatSelector */}
            <PostFormatSelector
                selectedFormat={selectedPlatform}
                onSelectFormat={setSelectedPlatform} // This function will update the selectedPlatform state
            />



            <div className="footer-controls">
                <div className="footer-note" style={{ fontSize: '0.9em', color: '#666', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <i className="fas fa-info-circle" style={{ color: '#7e57c2', fontSize: '1em' }}></i>
                    <span>Be specific about your target audience and tone</span>
                </div>
                <GenerateButton onClick={handleGeneratePost} isLoading={isLoading} />
            </div>

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
                <div className="generated-content-wrapper">
                    {/*
           Consolidate the content display.
           Assuming generatedContent.post is the primary text field,
           but fallback to generatedContent.content if 'post' isn't there,
           and then to JSON.stringify for debugging if neither exists.
        */}
                    <div className="content-display">
                        {/* Display platform if available, otherwise default to "Post" */}
                        <h3>Content for {generatedContent.platform || "Your Post"}</h3>
                        <div className="content-text">
                            {generatedContent.post || generatedContent.content || JSON.stringify(generatedContent, null, 2)}
                        </div>
                    </div>

                    {/* Add the ContentEvaluation component - assumes it takes the whole object */}
                    <ContentEvaluation evaluationData={generatedContent} />

                    {/* The Publish Button wrapper */}
                    <div className="publish-button-wrapper">
                        <PublishButton onClick={handlePublishClick} disabled={isLoading} />
                    </div>
                </div>
            )}
            {showPublishModal && (
                <PublishModal
                    onClose={() => setShowPublishModal(false)} // Function to close the modal
                    onPublish={handlePublishConfirm} // Function to call when publishing is confirmed
                    generatedPostContent={generatedContent} // Pass the generated content
                />
            )}
        </div>
    );
};

export default SocialPostGenerator;
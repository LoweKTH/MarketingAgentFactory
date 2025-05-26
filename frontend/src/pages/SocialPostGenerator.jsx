// Updated SocialPostGenerator.js
import React, { useState } from 'react';
import './SocialPostGenerator.css';

import PromptInput from '../components/PromptInput';
import PostFormatSelector from '../components/PostFormatSelector';
import PublishButton from '../components/PublishButton';
import PublishModal from '../components/PublishModal';
import GenerateButton from '../components/GenerateButton';
import ContentEvaluation from '../components/ContentEvaluation';
// Removed SaveButton import
import { generateSocialPost, saveGeneratedContent } from '../api/ContentGenerator-api';

const SocialPostGenerator = () => {
    const [prompt, setPrompt] = useState('');
    const [selectedPlatform, setSelectedPlatform] = useState('linkedin');
    const [isLoading, setIsLoading] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false); // Renamed from isSaving to isPublishing
    const [error, setError] = useState(null);
    const [generatedContent, setGeneratedContent] = useState(null);
    const [showPublishModal, setShowPublishModal] = useState(false);
    const [savedTaskId, setSavedTaskId] = useState(null); // Track if content is saved

    const handleGeneratePost = async () => {
        // Clear previous states
        setGeneratedContent(null);
        setError(null);
        setSavedTaskId(null); // Reset saved status

        if (!prompt.trim()) {
            setError("Please enter a prompt before generating.");
            return;
        }

        setIsLoading(true);

        const apiPayload = {
            contentType: "social_post",
            brandVoice: "professional",
            topic: prompt,
            platform: selectedPlatform,
            targetAudience: "business professionals",
            keyMessages: ["time management", "work-life balance"]
        };

        try {
            const result = await generateSocialPost(apiPayload);
            setGeneratedContent(result.data || result);
            console.log("API Response:", result);
        } catch (err) {
            console.error("Failed to generate post:", err);
            setError(err.message || "An unexpected error occurred during API call.");
        } finally {
            setIsLoading(false);
        }
    };

    // Handle saving content (called after publish confirmation)
    const handleSaveContent = async () => {
        if (!generatedContent) {    
            setError("No content to save.");
            return false; // Return false to indicate failure
        }

        const savePayload = {
            content: generatedContent.content || generatedContent.post,
            contentType: "social_post",
            brandVoice: "professional", 
            topic: prompt,
            platform: selectedPlatform,
            targetAudience: "business professionals",
            keyMessages: ["time management", "work-life balance"],
            generationTimeSeconds: generatedContent.generationTimeSeconds,
            modelUsed: generatedContent.workflowInfo?.modelUsed,
            workflowInfo: generatedContent.workflowInfo,
            suggestions: generatedContent.suggestions,
            estimatedMetrics: generatedContent.estimatedMetrics
        };

        try {
            const result = await saveGeneratedContent(savePayload);
            setSavedTaskId(result.data || result); // Store the returned task ID
            console.log("Content saved with task ID:", result);
            return true; // Return true to indicate success
        } catch (err) {
            console.error("Failed to save content:", err);
            setError(err.message || "Failed to save content.");
            return false; // Return false to indicate failure
        }
    };

    // Publish button shows modal first
    const handlePublishClick = () => {
        if (generatedContent && generatedContent.content) {
            setShowPublishModal(true);
        } else {
            alert("Please generate content first before publishing.");
        }
    };

    const handlePublishConfirm = async (platforms, content, recurringOptions) => {
        console.log("--- PUBLISH CONFIRMATION ---");
        console.log("Content to publish:", content.post);
        console.log("Platforms selected:", platforms);

        // Set loading state
        setIsPublishing(true);
        setError(null);

        // Save the content to database
        const saveSuccess = await handleSaveContent();
        
        if (saveSuccess) {
            // Here you would also publish to social media platforms
            // For now, just simulate the publishing
            if (recurringOptions) {
                console.log("This will be scheduled as a recurring post!");
                console.log("Frequency:", recurringOptions.frequencyHours, "hours");
            } else {
                console.log("This will be published immediately!");
            }
            console.log("Task ID:", savedTaskId);
            console.log("---------------------------------------");

            alert(`Content saved and publishing simulated for: ${platforms.join(', ')}.\n${recurringOptions ? `(Recurring every ${recurringOptions.frequencyHours} hours)` : '(Immediate Publish)'}\nTask ID: ${savedTaskId}\nCheck console for details.`);
        }
        
        setIsPublishing(false);
        setShowPublishModal(false);
    };

    return (
        <div className="social-post-generator-container">
            <h2 className="main-title">Create Your Social Media Post</h2>

            <PromptInput value={prompt} onChange={setPrompt} />

            <PostFormatSelector
                selectedFormat={selectedPlatform}
                onSelectFormat={setSelectedPlatform}
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

            {isPublishing && (
                <div style={{ textAlign: 'center', marginTop: '20px', color: '#1976d2' }}>
                    <i className="fas fa-spinner fa-spin"></i> Saving content...
                </div>
            )}

            {error && (
                <div style={{ textAlign: 'center', marginTop: '20px', color: 'red' }}>
                    Error: {error}
                </div>
            )}

            {generatedContent && (
                <div className="generated-content-wrapper">
                    <div className="content-display">
                        <h3>Content for {generatedContent.platform || selectedPlatform || "Your Post"}</h3>
                        <div className="content-text">
                            {generatedContent.post || generatedContent.content || JSON.stringify(generatedContent, null, 2)}
                        </div>
                    </div>

                    <ContentEvaluation evaluationData={generatedContent} />

                    {/* Modified: Only publish button now */}
                    <div className="action-buttons-wrapper" style={{ 
                        display: 'flex', 
                        gap: '10px', 
                        marginTop: '20px',
                        justifyContent: 'center',
                        flexWrap: 'wrap'
                    }}>
                        {/* Publish Button now handles saving too */}
                        <PublishButton 
                            onClick={handlePublishClick} 
                            disabled={isLoading || isPublishing} 
                        />
                    </div>

                    {/* Show save success message */}
                    {savedTaskId && (
                        <div style={{ 
                            textAlign: 'center', 
                            marginTop: '10px', 
                            color: '#4caf50',
                            fontSize: '0.9em'
                        }}>
                            <i className="fas fa-check-circle"></i> Content saved! Task ID: {savedTaskId}
                        </div>
                    )}
                </div>
            )}

            {showPublishModal && (
                <PublishModal
                    onClose={() => setShowPublishModal(false)}
                    onPublish={handlePublishConfirm}
                    generatedPostContent={generatedContent}
                />
            )}
        </div>
    );
};

export default SocialPostGenerator;
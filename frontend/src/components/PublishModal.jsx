// src/components/PublishModal.jsx
import React, { useState } from 'react';
import './PublishModal.css'; // Create this CSS file

const PublishModal = ({ onClose, onPublish, generatedPostContent }) => {
    // State to keep track of selected platforms
    const [selectedPlatforms, setSelectedPlatforms] = useState({
        twitter: false,
        linkedin: false,
        facebook: false,
        instagram: false,
    });

    const [isRecurring, setIsRecurring] = useState(false);
    const [frequencyHours, setFrequencyHours] = useState(24);

    const handlePlatformChange = (platform) => {
        setSelectedPlatforms(prev => ({
            ...prev,
            [platform]: !prev[platform] // Toggle selection
        }));
    };

    const handlePublishConfirm = () => {
        const platformsToPublish = Object.keys(selectedPlatforms).filter(
            (platform) => selectedPlatforms[platform]
        );

        if (platformsToPublish.length === 0) {
            alert("Please select at least one platform to publish.");
            return;
        }

        // Call the parent's onPublish function with selected platforms and content
        onPublish(platformsToPublish, generatedPostContent);
        onClose(); // Close the modal after confirming publish
    };

   return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close-button" onClick={onClose}>&times;</button>
                <h2>Choose Platforms to Publish To</h2>

                <p className="modal-disclaimer">
                    <i className="fas fa-info-circle"></i>
                    Please note: The generated text may not be perfectly optimized or catered to every platform's specific character limits, tone, or formatting requirements. Review before publishing!
                </p>

                <div className="platform-selection-grid">
                    {/* ... (Existing platform checkboxes) ... */}
                    <label className="platform-checkbox">
                        <input type="checkbox" checked={selectedPlatforms.twitter} onChange={() => handlePlatformChange('twitter')} />
                        <i className="fab fa-twitter"></i> Twitter/X
                    </label>
                    <label className="platform-checkbox">
                        <input type="checkbox" checked={selectedPlatforms.linkedin} onChange={() => handlePlatformChange('linkedin')} />
                        <i className="fab fa-linkedin"></i> LinkedIn
                    </label>
                    <label className="platform-checkbox">
                        <input type="checkbox" checked={selectedPlatforms.facebook} onChange={() => handlePlatformChange('facebook')} />
                        <i className="fab fa-facebook-f"></i> Facebook
                    </label>
                    <label className="platform-checkbox">
                        <input type="checkbox" checked={selectedPlatforms.instagram} onChange={() => handlePlatformChange('instagram')} />
                        <i className="fab fa-instagram"></i> Instagram
                    </label>
                </div>

                {/* NEW: Recurring Post Options */}
                <div className="recurring-options">
                    <label className="recurring-checkbox">
                        <input
                            type="checkbox"
                            checked={isRecurring}
                            onChange={(e) => setIsRecurring(e.target.checked)}
                        />
                        Schedule as Recurring Post
                    </label>

                    {isRecurring && (
                        <div className="frequency-input">
                            <label htmlFor="frequency">Repeat every:</label>
                            <input
                                type="number"
                                id="frequency"
                                value={frequencyHours}
                                onChange={(e) => setFrequencyHours(Math.max(1, parseInt(e.target.value) || 1))} // Min 1 hour
                                min="1"
                                step="1"
                            />
                            <span>hours</span>
                        </div>
                    )}
                </div>

                <div className="modal-actions">
                    <button className="modal-cancel-btn" onClick={onClose}>Cancel</button>
                    <button className="modal-confirm-btn" onClick={handlePublishConfirm}>
                        Confirm Publish
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PublishModal;
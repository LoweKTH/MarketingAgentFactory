// src/components/PublishButton.jsx
import React from 'react';
import './PublishButton.css'; // Import its own CSS file

const PublishButton = ({ onClick, disabled }) => {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className="publish-btn" // Use a specific class for this button
        >
            Publish
        </button>
    );
};

export default PublishButton;
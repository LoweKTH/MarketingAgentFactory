// src/components/PromptInput.js
import React from 'react';
import './PromptInput.css';

// This component now accepts 'value' and 'onChange' as props
const PromptInput = ({ value, onChange }) => { // Destructure props
    return (
        <div className="prompt-input-container">
            <label htmlFor="prompt" className="prompt-label">Your Prompt</label>
            <textarea
                id="prompt"
                className="prompt-textarea"
                value={value} // The value is now controlled by the parent component's state
                onChange={(e) => onChange(e.target.value)} // When the input changes, call the onChange function passed from the parent
                placeholder="Example: Create a LinkedIn post about the benefits of AI in content creation"
            />
        </div>
    );
};

export default PromptInput;
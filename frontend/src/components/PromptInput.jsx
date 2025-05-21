// src/components/PromptInput.js
import React, { useState } from 'react';
import './PromptInput.css';

const PromptInput = () => {
  const [prompt, setPrompt] = useState('');

  return (
    <div className="prompt-input-container">
      <label htmlFor="prompt" className="prompt-label">Your Prompt</label>
      <textarea
        id="prompt"
        className="prompt-textarea"
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder="Example: Create a LinkedIn post about the benefits of AI in content creation"
      />
    </div>
  );
};

export default PromptInput;
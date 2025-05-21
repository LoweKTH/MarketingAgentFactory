// src/components/SocialPostGenerator.js
import React from 'react';
import './SocialPostGenerator.css'; // For overall layout of this component

import PromptInput from '../components/PromptInput';
import PostFormatSelector from '../components/PostFormatSelector';
import GenerateButton from '../components/GenerateButton';  // no braces, default import


const SocialPostGenerator = () => {
  const handleGeneratePost = () => {
    // Logic to generate post will go here
    console.log("Generate Post clicked!");
    // You'll gather state from child components or lift state up here
  };

  return (
    <div className="social-post-generator-container">
      <h2 className="main-title">Create Your Social Media Post</h2>
      <PromptInput />
      <PostFormatSelector />
      <div className="footer-controls">
        <GenerateButton onClick={handleGeneratePost} />
      </div>
    </div>
  );
};

export default SocialPostGenerator;
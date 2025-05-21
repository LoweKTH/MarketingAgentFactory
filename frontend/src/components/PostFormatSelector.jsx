// src/components/PostFormatSelector.js
import React, { useState } from 'react';
import './PostFormatSelector.css';
// You'll need icons, e.g., from react-icons
import { FaLinkedin, FaTwitter, FaInstagram, FaFacebookF } from 'react-icons/fa';

const PostFormatSelector = () => {
  const [selectedFormat, setSelectedFormat] = useState('LinkedIn'); // Default selection

  const formats = [
    { name: 'LinkedIn', icon: <FaLinkedin /> },
    { name: 'Twitter', icon: <FaTwitter /> },
    { name: 'Instagram', icon: <FaInstagram /> },
    { name: 'Facebook', icon: <FaFacebookF /> },
  ];

  return (
    <div className="post-format-container">
      <label className="post-format-label">Post Format</label>
      <div className="format-buttons">
        {formats.map((format) => (
          <button
            key={format.name}
            className={`format-button ${selectedFormat === format.name ? 'active' : ''}`}
            onClick={() => setSelectedFormat(format.name)}
          >
            {format.icon} {format.name}
          </button>
        ))}
      </div>
    </div>
  );
};

export default PostFormatSelector;
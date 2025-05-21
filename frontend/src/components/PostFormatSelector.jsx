// src/components/PostFormatSelector.js
import React from 'react';
import './PostFormatSelector.css';
// You'll need icons, e.g., from react-icons
import { FaLinkedin, FaTwitter, FaInstagram, FaFacebookF } from 'react-icons/fa';

// Accept 'selectedFormat' and 'onSelectFormat' as props
const PostFormatSelector = ({ selectedFormat, onSelectFormat }) => {
  const formats = [
    { name: 'linkedin', label: 'LinkedIn', icon: <FaLinkedin /> }, // Use lowercase for platform name to match API expectation
    { name: 'twitter', label: 'Twitter', icon: <FaTwitter /> },
    { name: 'instagram', label: 'Instagram', icon: <FaInstagram /> },
    { name: 'facebook', label: 'Facebook', icon: <FaFacebookF /> },
  ];

  return (
    <div className="post-format-container">
      <label className="post-format-label">Post Format</label>
      <div className="format-buttons">
        {formats.map((format) => (
          <button
            key={format.name}
            className={`format-button ${selectedFormat === format.name ? 'active' : ''}`}
            // Call the onSelectFormat prop with the selected format's name
            onClick={() => onSelectFormat(format.name)}
          >
            {format.icon} {format.label} {/* Display the human-readable label */}
          </button>
        ))}
      </div>
    </div>
  );
};

export default PostFormatSelector;
import React from 'react';
import './GenerateButton.css';
import { FaPaperPlane } from 'react-icons/fa';

const GenerateButton = ({ onClick }) => {
  return (
    <button className="generate-button" onClick={onClick}>
      Generate Post
      <FaPaperPlane className="send-icon" />
    </button>
  );
};

export default GenerateButton;

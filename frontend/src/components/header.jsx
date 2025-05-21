// src/components/Header.js
import React from 'react';
import './header.css'; // Create Header.css for styles

const Header = () => {
  return (
    <header className="header-container">
      <h1 className="header-title">Social Post Generator</h1>
      <span className="header-badge">Powered by</span>
    </header>
  );
};

export default Header;
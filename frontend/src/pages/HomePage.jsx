// src/components/HomePage.js
import React from 'react';
import { Link } from 'react-router-dom'; // Used for navigation
import './HomePage.css'; // Create this CSS file next

const HomePage = () => {
    return (
        <div className="homepage-container">
            <div className="homepage-content">
                <h2 className="homepage-main-heading">Generate Engaging Social Media Content Instantly</h2>
                <p className="homepage-description">
                    Leverage AI to craft compelling posts for LinkedIn, Twitter, Instagram, and Facebook.
                    Save time, maintain brand voice, and reach your audience effectively.
                </p>
                <Link to="/generate" className="homepage-cta-button">
                    Start Generating Posts <i className="fas fa-arrow-right"></i>
                </Link>
                {/* Optional: Add some feature highlights or testimonials */}
                <div className="homepage-features">
                    <h3>Key Features:</h3>
                    <ul>
                        <li><i className="fas fa-check-circle"></i> AI-powered content generation</li>
                        <li><i className="fas fa-check-circle"></i> Platform-specific optimization</li>
                        <li><i className="fas fa-check-circle"></i> Customizable brand voice</li>
                        <li><i className="fas fa-check-circle"></i> Trending topic integration</li>
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default HomePage;
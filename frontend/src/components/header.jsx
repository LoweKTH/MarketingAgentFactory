// src/components/Header.js
import React from 'react';
import { Link, useLocation } from 'react-router-dom'; // Import Link and useLocation
import './Header.css'; // Link to its styles

const Header = () => {
    const location = useLocation(); // Hook to get current path for active links

    return (
        <header className="header-container">
            <div className="header-left">
                <Link to="/" className="header-title-link"> {/* Link the title to home */}
                    <h1 className="header-title">Social Post Generator</h1>
                </Link>

            </div>

            <nav className="header-nav">
                {/* Link for the Home Page */}
                <Link
                    to="/"
                    className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
                >
                    Home
                </Link>
                {/* Link for the Post Generator Page */}
                <Link
                    to="/generate"
                    className={`nav-link ${location.pathname === '/generate' ? 'active' : ''}`}
                >
                    Generate Post
                </Link>
            </nav>

            <div className="header-right">
                {/* Profile Button */}
                <Link
                    to="/profile"
                    className={`profile-button ${location.pathname === '/profile' ? 'active' : ''}`}
                >
                    <i className="fas fa-user-circle"></i> Profile
                </Link>
            </div>
        </header>
    );
};

export default Header;
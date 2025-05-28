// src/components/Header.js
import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; // Import useAuth hook
import './Header.css';

const Header = () => {
    const location = useLocation();
    const { isAuthenticated, logout } = useAuth(); // Get isAuthenticated and logout from context

    return (
        <header className="header-container">
            <div className="header-left">
                <Link to="/" className="header-title-link">
                    <h1 className="header-title">Social Post Generator</h1>
                </Link>
            </div>

            <nav className="header-nav">
                <Link
                    to="/"
                    className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
                >
                    Home
                </Link>
                {/* Only show Generate Post if authenticated */}
                {isAuthenticated && (
                    <Link
                        to="/generate"
                        className={`nav-link ${location.pathname === '/generate' ? 'active' : ''}`}
                    >
                        Generate Post
                    </Link>
                )}
            </nav>

            <div className="header-right">
                {isAuthenticated ? (
                    <>
                        <Link
                            to="/profile"
                            className={`profile-button ${location.pathname === '/profile' ? 'active' : ''}`}
                        >
                            <i className="fas fa-user-circle"></i> Profile
                        </Link>
                        <button onClick={logout} className="login-logout-button">
                            Logout
                        </button>
                    </>
                ) : (
                    <Link
                        to="/login"
                        className={`login-logout-button ${location.pathname === '/login' ? 'active' : ''}`}
                    >
                        Login
                    </Link>
                )}
            </div>
        </header>
    );
};

export default Header;
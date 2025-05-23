import React, { useState, useEffect } from 'react';
import ConfirmationModal from '../components/ConfirmationModal'; // NEW: Import ConfirmationModal
import './ProfilePage.css';

// Helper function to capitalize the first letter
const capitalize = (s) => {
    if (typeof s !== 'string') return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
};

const ProfilePage = ({  }) => {
    // Define the social media platforms and their connection status
    const [connectedAccounts, setConnectedAccounts] = useState({
        twitter: true,
        linkedin: true,
        facebook: false,
        instagram: false,
        // Add more platforms as needed
    });

    // State for the active tab
    const [activeTab, setActiveTab] = useState('all'); // 'all' for all posts, or platform name (e.g., 'twitter')

    const [scheduledPosts, setScheduledPosts] = useState([
        {
            id: 'mock-1',
            content: 'Exciting news from our AI Marketing Agent! #AI #Marketing',
            platforms: ['twitter', 'linkedin'], // Ensure these match the keys in connectedAccounts
            frequencyHours: 24,
            isActive: true,
            lastRun: '2025-05-20 14:00',
            nextRun: '2025-05-21 14:00'
        },
        {
            id: 'mock-2',
            content: 'Unlock your business potential with cutting-edge tech. #Innovation',
            platforms: ['facebook'], // Ensure these match the keys in connectedAccounts
            frequencyHours: 72,
            isActive: false,
            lastRun: '2025-05-18 09:00',
            nextRun: '2025-05-21 09:00'
        },
        {
            id: 'mock-3',
            content: 'A new post just for Twitter! #TweetDeck',
            platforms: ['twitter'],
            frequencyHours: 48,
            isActive: true,
            lastRun: '2025-05-19 10:00',
            nextRun: '2025-05-21 10:00'
        },
        {
            id: 'mock-4',
            content: 'Engage your professional network with this LinkedIn exclusive. #CareerGrowth',
            platforms: ['linkedin'],
            frequencyHours: 24,
            isActive: true,
            lastRun: '2025-05-20 16:00',
            nextRun: '2025-05-21 16:00'
        }
    ]);

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDeleteId, setPostToDeleteId] = useState(null);

    const handleToggleActive = (postId) => {
        setScheduledPosts(prevPosts =>
            prevPosts.map(post =>
                post.id === postId ? { ...post, isActive: !post.isActive } : post
            )
        );
    };

    const handleDeleteClick = (postId) => {
        setPostToDeleteId(postId);
        setShowDeleteConfirm(true);
    };

    const handleConfirmDelete = () => {
        setScheduledPosts(prevPosts =>
            prevPosts.filter(post => post.id !== postToDeleteId)
        );
        setShowDeleteConfirm(false);
        setPostToDeleteId(null);
    };

    const handleCancelDelete = () => {
        setShowDeleteConfirm(false);
        setPostToDeleteId(null);
    };

    const handleConnectNewAccount = () => {
        // Example: If you connect Facebook, update the state:
        // setConnectedAccounts(prev => ({ ...prev, facebook: true }));
    };

    // Filter posts based on the active tab
    const filteredPosts = scheduledPosts.filter(post => {
        if (activeTab === 'all') {
            return true;
        }
        // Check if the post's platforms array includes the active tab's platform
        return post.platforms.includes(activeTab);
    });

    const getPlatformIcon = (platform) => {
        switch (platform) {
            case 'twitter': return <i className="fab fa-twitter"></i>;
            case 'linkedin': return <i className="fab fa-linkedin"></i>;
            case 'facebook': return <i className="fab fa-facebook-f"></i>;
            case 'instagram': return <i className="fab fa-instagram"></i>;
            default: return null;
        }
    };

    return (
        <div className="profile-page-container">
            <h1>Your Scheduled Recurring Posts</h1>

            <div className="connected-accounts-section">
                <h2>Connected Social Accounts</h2>
                <div className="account-status">
                    {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                        <span key={platform} className={`platform-icon ${isConnected ? 'connected' : 'not-connected'}`}>
                            {getPlatformIcon(platform)} {capitalize(platform)}: {isConnected ? 'Connected' : 'Not Connected'}
                        </span>
                    ))}
                </div>
                <button className="connect-new-btn" onClick={handleConnectNewAccount}>
                    Connect New Account
                </button>
            </div>

            {/* Tabs for filtering posts */}
            <div className="tabs-container">
                <button
                    className={`tab-btn ${activeTab === 'all' ? 'active' : ''}`}
                    onClick={() => setActiveTab('all')}
                >
                    All Posts
                </button>
                {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                    isConnected && ( // Only show tabs for connected accounts
                        <button
                            key={platform}
                            className={`tab-btn ${activeTab === platform ? 'active' : ''}`}
                            onClick={() => setActiveTab(platform)}
                        >
                            {getPlatformIcon(platform)} {capitalize(platform)}
                        </button>
                    )
                ))}
            </div>

            {/* Display filtered posts */}
            {filteredPosts.length === 0 ? (
                <p className="no-posts-message">
                    No recurring posts scheduled for {activeTab === 'all' ? 'all platforms' : capitalize(activeTab)} yet.
                    Generate and schedule one from the main page!
                </p>
            ) : (
                <div className="scheduled-posts-list">
                    {filteredPosts.map(post => (
                        <div key={post.id} className="scheduled-post-item">
                            <div className="post-header">
                                <h3>Post ID: {post.id}</h3>
                                <div className="post-actions">
                                    <label className="toggle-switch">
                                        <input
                                            type="checkbox"
                                            checked={post.isActive}
                                            onChange={() => handleToggleActive(post.id)}
                                        />
                                        <span className="slider round"></span>
                                    </label>
                                    <button onClick={() => handleDeleteClick(post.id)} className="delete-btn">
                                        <i className="fas fa-trash"></i> Delete
                                    </button>
                                </div>
                            </div>
                            <p className="post-content">{post.content}</p>
                            <div className="post-details">
                                <span><i className="fas fa-share-alt"></i> Platforms: {post.platforms.map(p => capitalize(p)).join(', ')}</span>
                                <span><i className="fas fa-redo-alt"></i> Every {post.frequencyHours} hours</span>
                                <span><i className="fas fa-clock"></i> Last Run: {post.lastRun}</span>
                                {post.isActive && <span><i className="fas fa-calendar-alt"></i> Next Run: {post.nextRun}</span>}
                                {!post.isActive && <span style={{ color: '#dc3545' }}><i className="fas fa-pause-circle"></i> Paused</span>}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {showDeleteConfirm && (
                <ConfirmationModal
                    message="Are you sure you want to delete this scheduled post?"
                    onConfirm={handleConfirmDelete}
                    onCancel={handleCancelDelete}
                />
            )}
        </div>
    );
};

export default ProfilePage;
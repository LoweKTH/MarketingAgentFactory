// src/pages/ProfilePage.jsx
import React, { useState, useEffect } from 'react';
import './ProfilePage.css'; // Link to its conceptual CSS

const ProfilePage = () => {
    // Conceptual state for recurring posts
    const [scheduledPosts, setScheduledPosts] = useState([
        {
            id: 'mock-1',
            content: 'Exciting news from our AI Marketing Agent! #AI #Marketing',
            platforms: ['twitter', 'linkedin'],
            frequencyHours: 24,
            isActive: true,
            lastRun: '2025-05-20 14:00',
            nextRun: '2025-05-21 14:00'
        },
        {
            id: 'mock-2',
            content: 'Unlock your business potential with cutting-edge tech. #Innovation',
            platforms: ['facebook'],
            frequencyHours: 72,
            isActive: false,
            lastRun: '2025-05-18 09:00',
            nextRun: '2025-05-21 09:00'
        }
    ]);

    const handleToggleActive = (postId) => {
        setScheduledPosts(prevPosts =>
            prevPosts.map(post =>
                post.id === postId ? { ...post, isActive: !post.isActive } : post
            )
        );
        console.log(`Toggled post ${postId}`);
    };

    const handleDeletePost = (postId) => {
        if (window.confirm("Are you sure you want to delete this scheduled post?")) {
            setScheduledPosts(prevPosts =>
                prevPosts.filter(post => post.id !== postId)
            );
            console.log(`Deleted post ${postId}`);
        }
    };

    const handleConnectNewAccount = () => {
        console.log("Connect New Account button clicked (Conceptual)!");
        alert("Simulating connecting a new social media account. In a real app, this would redirect to an OAuth flow (e.g., Twitter/X, LinkedIn, Facebook API).");
        
    };

    return (
        <div className="profile-page-container">
            <h1>Your Scheduled Recurring Posts</h1>

            <div className="connected-accounts-section">
                <h2>Connected Social Accounts</h2>
                <div className="account-status">
                    <span className="platform-icon"><i className="fab fa-twitter"></i> Twitter/X: Connected</span>
                    <span className="platform-icon"><i className="fab fa-linkedin"></i> LinkedIn: Connected</span>
                    <span className="platform-icon"><i className="fab fa-facebook-f"></i> Facebook: Not Connected</span>
                    <span className="platform-icon"><i className="fab fa-instagram"></i> Instagram: Not Connected</span>
                    {/* In a real app, these would reflect actual backend connection status */}
                </div>
                <button className="connect-new-btn" onClick={handleConnectNewAccount}>
                    Connect New Account
                </button>
            </div>

            {scheduledPosts.length === 0 ? (
                <p>No recurring posts scheduled yet. Generate and schedule one from the main page!</p>
            ) : (
                <div className="scheduled-posts-list">
                    {scheduledPosts.map(post => (
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
                                    <button onClick={() => handleDeletePost(post.id)} className="delete-btn">
                                        <i className="fas fa-trash"></i> Delete
                                    </button>
                                </div>
                            </div>
                            <p className="post-content">{post.content}</p>
                            <div className="post-details">
                                <span><i className="fas fa-share-alt"></i> Platforms: {post.platforms.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(', ')}</span>
                                <span><i className="fas fa-redo-alt"></i> Every {post.frequencyHours} hours</span>
                                <span><i className="fas fa-clock"></i> Last Run: {post.lastRun}</span>
                                {post.isActive && <span><i className="fas fa-calendar-alt"></i> Next Run: {post.nextRun}</span>}
                                {!post.isActive && <span style={{color: '#dc3545'}}><i className="fas fa-pause-circle"></i> Paused</span>}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ProfilePage;
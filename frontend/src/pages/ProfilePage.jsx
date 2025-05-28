// src/pages/ProfilePage.js

import React, { useState, useEffect } from 'react';
import ConfirmationModal from '../components/ConfirmationModal';
import { fetchTasks as fetchTasksApi } from '../api/Task-api';
import { deleteTask } from '../api/Task-api';
import './ProfilePage.css';
import { initiateOAuth } from '../api/Auth-api';
import { fetchConnectedPlatforms } from '../api/Connection-api'; // Import the new API helper

// Helper function to capitalize the first letter of each word
const capitalizeWords = (s) => {
    if (typeof s !== 'string') return '';
    return s.split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
};

const ProfilePage = () => {
    // Initialize with default states, but fetch actual status
    const [connectedAccounts, setConnectedAccounts] = useState({
        twitter: false,
        linkedin: false, // Set to false initially, will be updated by API
        facebook: false,
        instagram: false,
    });
    const [activeTab, setActiveTab] = useState('all');
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDeleteId, setPostToDeleteId] = useState(null);
    const [showConnectOptions, setShowConnectOptions] = useState(false);
    const [connectionMessage, setConnectionMessage] = useState(null);
    const [connectionError, setConnectionError] = useState(null);

    // Function to fetch tasks from the backend and process them for display
    const fetchAndProcessTasks = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchTasksApi();
            const formattedTasks = data.map(task => {
                const randomHours = Math.floor(Math.random() * (72 - 24 + 1)) + 24;
                const baseDate = task.createdAt ? new Date(task.createdAt) : new Date();

                const lastRunPlaceholder = new Date(baseDate.getTime() - (randomHours * 3600 * 1000)).toLocaleString();
                const nextRunPlaceholder = new Date(baseDate.getTime() + (randomHours * 3600 * 1000)).toLocaleString();

                return {
                    ...task,
                    id: task.taskId || task.id,
                    platform: task.platform ? task.platform.toLowerCase() : 'N/A',
                    isActive: task.status === 'COMPLETED' || task.status === 'PROCESSING' || task.status === 'PENDING',
                    topic: task.contentType ? capitalizeWords(task.contentType.replace(/_/g, ' ')) : 'Generated Content',
                    frequencyHours: randomHours,
                    lastRun: lastRunPlaceholder,
                    nextRun: nextRunPlaceholder,
                };
            });
            setTasks(formattedTasks);
        } catch (err) {
            console.error("Failed to load tasks:", err);
            setError("Failed to load tasks. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    // New useEffect to fetch connection status
    useEffect(() => {
        const getConnections = async () => {
            try {
                const connections = await fetchConnectedPlatforms();
                setConnectedAccounts(connections); // Update state with actual connection status
            } catch (err) {
                console.error("Failed to fetch connected platforms:", err);
                // Optionally set a connection error state here
            }
        };

        fetchAndProcessTasks(); // Keep existing task fetch
        getConnections(); // Fetch connection status on component mount
    }, []);

    const handleToggleActive = (taskId) => {
        setTasks(prevTasks =>
            prevTasks.map(task =>
                task.id === taskId ? { ...task, isActive: !task.isActive } : task
            )
        );
        console.log(`Toggle active status for task: ${taskId}`);
        // TODO: In a real app, implement an API call to update the task's status in the backend
    };
    const handleDeleteClick = (taskId) => {
        setPostToDeleteId(taskId);
        setShowDeleteConfirm(true);
    };
    const handleConfirmDelete = async () => {
        try {
            await deleteTask(postToDeleteId);
            setTasks(prevTasks =>
                prevTasks.filter(task => task.id !== postToDeleteId)
            );
            console.log(`Successfully deleted task: ${postToDeleteId} from backend and frontend.`);
        } catch (err) {
            console.error(`Failed to delete task ${postToDeleteId}:`, err);
            setError(`Failed to delete task: ${err.message}`);
        } finally {
            setShowDeleteConfirm(false);
            setPostToDeleteId(null);
        }
    };

    const handleCancelDelete = () => {
        setShowDeleteConfirm(false);
        setPostToDeleteId(null);
    };

    const handleConnectPlatform = async (platform) => {
        setShowConnectOptions(false); // Close the options dropdown immediately

        try {
            setConnectionMessage(`Initiating ${capitalizeWords(platform)} connection...`);
            setConnectionError(null); // Clear previous errors

            await initiateOAuth(platform);
            // The API function will handle the window.location.href redirection,
            // so no need for further local state updates here unless the redirection fails
        } catch (err) {
            console.error(`Error connecting to ${capitalizeWords(platform)}:`, err);
            setConnectionError(`Error connecting to ${capitalizeWords(platform)}: ${err.message}`);
            setConnectionMessage(null);
        }
    };

    // Monitor URL for OAuth callback parameters after redirect
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const success = params.get('success');
        const platform = params.get('platform');
        const errorParam = params.get('error');
        const errorDescription = params.get('error_description');

        if (success === 'true' && platform) {
            setConnectionMessage(`${capitalizeWords(platform)} connected successfully!`);
            // Re-fetch connections to update the UI
            fetchConnectedPlatforms().then(connections => {
                setConnectedAccounts(connections);
            }).catch(err => {
                console.error("Failed to re-fetch connections after successful OAuth:", err);
            });
            // Clean URL
            window.history.replaceState({}, document.title, window.location.pathname);
        } else if (errorParam) {
            setConnectionError(`Connection failed: ${errorDescription || errorParam}`);
            // Clean URL
            window.history.replaceState({}, document.title, window.location.pathname);
        }
    }, []);


    const filteredTasks = tasks.filter(task => {
        if (activeTab === 'all') {
            return true;
        }
        return task.platform && task.platform === activeTab;
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

    if (loading) {
        return <div className="profile-page-container"><p>Loading tasks...</p></div>;
    }

    if (error) {
        return <div className="profile-page-container"><p className="error-message">{error}</p></div>;
    }

    return (
        <div className="profile-page-container">
            <h1>Your Saved Tasks</h1>

            <div className="connected-accounts-section">
                <h2>Connected Social Accounts</h2>
                {connectionMessage && <p className="success-message">{connectionMessage}</p>}
                {connectionError && <p className="error-message">{connectionError}</p>}
                <div className="account-status">
                    {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                        <span key={platform} className={`platform-icon ${isConnected ? 'connected' : 'not-connected'}`}>
                            {getPlatformIcon(platform)} {capitalizeWords(platform)}: {isConnected ? 'Connected' : 'Not Connected'}
                        </span>
                    ))}
                </div>
                <button className="connect-new-btn" onClick={() => setShowConnectOptions(true)}>
                    Connect New Account
                </button>

                {/* NEW: Connect New Account Options */}
                {showConnectOptions && (
                    <div className="connect-options-dropdown">
                        <h3>Select a platform to connect:</h3>
                        {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                            !isConnected && (
                                <button
                                    key={`connect-${platform}`}
                                    className="connect-option-btn"
                                    onClick={() => handleConnectPlatform(platform)}
                                >
                                    {getPlatformIcon(platform)} Connect {capitalizeWords(platform)}
                                </button>
                            )
                        ))}
                         <button className="cancel-connect-btn" onClick={() => setShowConnectOptions(false)}>
                            Cancel
                        </button>
                    </div>
                )}
            </div>

            <div className="tabs-container">
                <button
                    className={`tab-btn ${activeTab === 'all' ? 'active' : ''}`}
                    onClick={() => setActiveTab('all')}
                >
                    All Tasks
                </button>
                {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                    isConnected && (
                        <button
                            key={platform}
                            className={`tab-btn ${activeTab === platform ? 'active' : ''}`}
                            onClick={() => setActiveTab(platform)}
                        >
                            {getPlatformIcon(platform)} {capitalizeWords(platform)}
                        </button>
                    )
                ))}
            </div>

            {filteredTasks.length === 0 ? (
                <p className="no-posts-message">
                    No saved tasks for {activeTab === 'all' ? 'all platforms' : capitalizeWords(activeTab)} yet.
                    Generate a task from the main page!
                </p>
            ) : (
                <div className="scheduled-posts-list">
                    {filteredTasks.map(task => (
                        <div key={task.id} className="scheduled-post-item">
                            <div className="post-header">
                                {task.taskId && <h3>Task ID: {task.taskId}</h3>}
                                <div className="post-actions">
                                    <label className="toggle-switch">
                                        <input
                                            type="checkbox"
                                            checked={task.isActive}
                                            onChange={() => handleToggleActive(task.id)}
                                        />
                                        <span className="slider round"></span>
                                    </label>
                                    <button onClick={() => handleDeleteClick(task.id)} className="delete-btn">
                                        <i className="fas fa-trash"></i> Delete
                                    </button>
                                </div>
                            </div>
                            <p className="post-content">
                                <strong>Topic:</strong> {task.topic || 'N/A'}
                            </p>
                            <div className="post-details">
                                <textarea
                                    readOnly
                                    value={task.generatedContent || ''}
                                    style={{
                                        width: '100%',
                                        minHeight: '100px',
                                        marginTop: '5px',
                                        padding: '10px',
                                        border: '1px solid #dcdcdc',
                                        borderRadius: '4px',
                                        resize: 'vertical',
                                        boxSizing: 'border-box',
                                        fontSize: '0.9em',
                                        lineHeight: '1.5'
                                    }}
                                />
                            </div>
                            <div className="post-details">
                                <span><i className="fas fa-share-alt"></i> Platform: {task.platform ? capitalizeWords(task.platform) : 'N/A'}</span>
                                <span><i className="fas fa-redo-alt"></i> Every {task.frequencyHours} hours</span>
                                <span><i className="fas fa-clock"></i> Last Run: {task.lastRun}</span>
                                {task.isActive && <span><i className="fas fa-calendar-alt"></i> Next Run: {task.nextRun}</span>}
                                {!task.isActive && <span style={{ color: '#dc3545' }}><i className="fas fa-pause-circle"></i> Paused</span>}
                                {task.status && <span><i className="fas fa-info-circle"></i> Status: {capitalizeWords(task.status)}</span>}
                                {task.createdAt && <span><i className="fas fa-calendar-alt"></i> Created At: {new Date(task.createdAt).toLocaleString()}</span>}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {showDeleteConfirm && (
                <ConfirmationModal
                    message="Are you sure you want to delete this saved task?"
                    onConfirm={handleConfirmDelete}
                    onCancel={handleCancelDelete}
                />
            )}
        </div>
    );
};

export default ProfilePage;
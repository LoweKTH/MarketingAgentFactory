import React, { useState, useEffect } from 'react';
import ConfirmationModal from '../components/ConfirmationModal';
import { fetchTasks as fetchTasksApi } from '../api/Task-api'; // Correct import with alias
import './ProfilePage.css'; // Ensure your CSS is correctly linked

// Helper function to capitalize the first letter of each word
const capitalizeWords = (s) => {
    if (typeof s !== 'string') return '';
    return s.split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
};

const ProfilePage = () => {
    // Define the social media platforms and their connection status
    const [connectedAccounts, setConnectedAccounts] = useState({
        twitter: true,
        linkedin: true,
        facebook: false,
        instagram: false,
    });

    // State for the active tab (e.g., 'all', 'twitter', 'linkedin')
    const [activeTab, setActiveTab] = useState('all');

    // This will store tasks fetched from the backend, with added placeholder fields
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDeleteId, setPostToDeleteId] = useState(null);

    // Function to fetch tasks from the backend and process them for display
    const fetchAndProcessTasks = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchTasksApi(); // Call the actual API function from api.js

            // Map the backend TaskDto objects and add placeholder values for recurrence fields
            const formattedTasks = data.map(task => {
                // Generate some random-looking placeholder values for recurring fields
                const randomHours = Math.floor(Math.random() * (72 - 24 + 1)) + 24; // Between 24 and 72 hours
                const baseDate = task.createdAt ? new Date(task.createdAt) : new Date();

                // Example placeholder dates (adjust format as needed)
                const lastRunPlaceholder = new Date(baseDate.getTime() - (randomHours * 3600 * 1000)).toLocaleString();
                const nextRunPlaceholder = new Date(baseDate.getTime() + (randomHours * 3600 * 1000)).toLocaleString();

                return {
                    ...task, // Spread all original TaskDto fields (id, taskId, contentType, generatedContent, etc.)
                    id: task.taskId || task.id, // Use taskId as primary ID, fallback to DB id
                    platform: task.platform ? task.platform.toLowerCase() : 'N/A', // Ensure platform is lowercase
                    isActive: task.status === 'COMPLETED' || task.status === 'PROCESSING' || task.status === 'PENDING',
                    topic: task.contentType ? capitalizeWords(task.contentType.replace(/_/g, ' ')) : 'Generated Content',
                    // --- NEW: Hardcoded/Random placeholder values for recurring post fields ---
                    frequencyHours: randomHours,
                    lastRun: lastRunPlaceholder,
                    nextRun: nextRunPlaceholder,
                    // --- END NEW ---
                };
            });
            setTasks(formattedTasks); // Update the 'tasks' state
        } catch (err) {
            console.error("Failed to load tasks:", err);
            setError("Failed to load tasks. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    // Fetch tasks when the component mounts
    useEffect(() => {
        fetchAndProcessTasks();
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

    const handleConfirmDelete = () => {
        setTasks(prevTasks =>
            prevPosts.filter(post => post.id !== postToDeleteId)
        );
        setShowDeleteConfirm(false);
        setPostToDeleteId(null);
        console.log(`Confirmed delete for task: ${postToDeleteId}`);
        // TODO: In a real app, implement an API call to delete the task from the backend
    };

    const handleCancelDelete = () => {
        setShowDeleteConfirm(false);
        setPostToDeleteId(null);
    };

    const handleConnectNewAccount = () => {
        console.log("Connect New Account clicked");
    };

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
                <div className="account-status">
                    {Object.entries(connectedAccounts).map(([platform, isConnected]) => (
                        <span key={platform} className={`platform-icon ${isConnected ? 'connected' : 'not-connected'}`}>
                            {getPlatformIcon(platform)} {capitalizeWords(platform)}: {isConnected ? 'Connected' : 'Not Connected'}
                        </span>
                    ))}
                </div>
                <button className="connect-new-btn" onClick={handleConnectNewAccount}>
                    Connect New Account
                </button>
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
                                {/* RE-ADDED: Hardcoded/placeholder recurrence fields */}
                                <span><i className="fas fa-redo-alt"></i> Every {task.frequencyHours} hours</span>
                                <span><i className="fas fa-clock"></i> Last Run: {task.lastRun}</span>
                                {task.isActive && <span><i className="fas fa-calendar-alt"></i> Next Run: {task.nextRun}</span>}
                                {/* END RE-ADDED */}
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
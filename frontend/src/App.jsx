// src/App.js - Updated PrivateRoute component
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/header';
import HomePage from './pages/HomePage';
import SocialPostGenerator from './pages/SocialPostGenerator';
import ProfilePage from './pages/ProfilePage';
import Login from './pages/Login';
import { AuthProvider, useAuth } from './context/AuthContext';
import './App.css';

// Updated PrivateRoute component with loading state
const PrivateRoute = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();
    
    // Show loading spinner while checking authentication
    if (isLoading) {
        return (
            <div style={{ 
                display: 'flex', 
                justifyContent: 'center', 
                alignItems: 'center', 
                height: '100vh',
                fontSize: '18px',
                color: '#666'
            }}>
                <i className="fas fa-spinner fa-spin" style={{ marginRight: '10px' }}></i>
                Loading...
            </div>
        );
    }
    
    return isAuthenticated ? children : <Navigate to="/login" replace />;
};

const AppContent = () => {
    return (
        <>
            <Header />
            <main>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/login" element={<Login />} />
                    <Route
                        path="/generate"
                        element={
                            <PrivateRoute>
                                <SocialPostGenerator />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/profile"
                        element={
                            <PrivateRoute>
                                <ProfilePage />
                            </PrivateRoute>
                        }
                    />
                </Routes>
            </main>
        </>
    );
};

const App = () => {
    return (
        <Router>
            <AuthProvider>
                <AppContent />
            </AuthProvider>
        </Router>
    );
};

export default App;
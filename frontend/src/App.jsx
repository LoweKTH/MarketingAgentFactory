// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/header';
import HomePage from './pages/HomePage'; // Assuming you have a Home component
import SocialPostGenerator from './pages/SocialPostGenerator'; // Assuming you have a GeneratePost component
import ProfilePage from './pages/ProfilePage'; // Assuming you have a Profile component
import Login from './pages/Login'; // Import the new Login component
import { AuthProvider, useAuth } from './context/AuthContext'; // Import AuthProvider and useAuth
import './App.css'; // Your global app styles

// A component to protect routes
const PrivateRoute = ({ children }) => {
    const { isAuthenticated } = useAuth();
    return isAuthenticated ? children : <Navigate to="/login" replace />;
};

const AppContent = () => {
    return (
        <>
            <Header />
            <main>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/login" element={<Login />} /> {/* Login route */}
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
                    {/* Add other public routes as needed */}
                </Routes>
            </main>
        </>
    );
};

const App = () => {
    return (
        <Router>
            <AuthProvider> {/* Wrap your entire app with AuthProvider */}
                <AppContent />
            </AuthProvider>
        </Router>
    );
};

export default App;
// src/context/AuthContext.js
import React, { createContext, useState, useEffect, useContext } from 'react';
import { loginUser } from '../api/Auth-api';
import { setupInterceptors } from '../api/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true); // Add loading state
    const [user, setUser] = useState(null);
    const [authToken, setAuthToken] = useState(null);

    // Effect to check for token in localStorage on initial load
    useEffect(() => {
        const checkAuthStatus = () => {
            const token = localStorage.getItem('jwt_token');
            const userData = localStorage.getItem('user_data');
            
            if (token) {
                setAuthToken(token);
                setIsAuthenticated(true);
                
                // Load user data if available
                if (userData) {
                    try {
                        setUser(JSON.parse(userData));
                    } catch (error) {
                        console.error('Error parsing user data:', error);
                        localStorage.removeItem('user_data');
                    }
                }
                
                // Set up axios interceptors with the token
                setupInterceptors(token);
                
                // Optionally, verify token with backend
                // You could add a call to verifyToken() here if you have that endpoint
            }
            
            setIsLoading(false); // Set loading to false after checking
        };

        checkAuthStatus();
    }, []);

    const login = async (username, password) => {
        try {
            const response = await loginUser(username, password);
            const { token, user: userData } = response;
            
            // Store token and user data
            localStorage.setItem('jwt_token', token);
            if (userData) {
                localStorage.setItem('user_data', JSON.stringify(userData));
                setUser(userData);
            }
            
            setAuthToken(token);
            setIsAuthenticated(true);
            
            // Setup axios interceptors immediately after login
            setupInterceptors(token);
            
            console.log('Login successful');
            return true;
        } catch (error) {
            console.error('Login failed:', error.message);
            setIsAuthenticated(false);
            setAuthToken(null);
            setUser(null);
            localStorage.removeItem('jwt_token');
            localStorage.removeItem('user_data');
            setupInterceptors(null); // Clear interceptor if login fails
            return false;
        }
    };

    const logout = async () => {        
        // Clear all authentication state
        setIsAuthenticated(false);
        setUser(null);
        setAuthToken(null);
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_data');
        setupInterceptors(null); // Clear the token from the axios interceptor
        console.log('Logged out');
    };

    const value = {
        isAuthenticated,
        isLoading,
        user,
        authToken,
        login,
        logout
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook for easier access to AuthContext
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
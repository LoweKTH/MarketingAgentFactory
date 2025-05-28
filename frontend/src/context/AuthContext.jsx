// src/context/AuthContext.js
import React, { createContext, useState, useEffect, useContext } from 'react';
import axios from 'axios'; // We will use this directly for login before interceptor is set
import { setupInterceptors } from '../api/api'; // Import the interceptor setup utility

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null); // You can store user details here if needed
    const [authToken, setAuthToken] = useState(null);
    const API_BASE_URL = 'http://localhost:8080/api'; // Your backend API base URL

    // Effect to check for token in localStorage on initial load
    useEffect(() => {
        const token = localStorage.getItem('jwt_token');
        if (token) {
            setAuthToken(token);
            setIsAuthenticated(true);
            // Optionally, fetch user details from backend using the token
            // axios.get(`${API_BASE_URL}/auth/me`, { headers: { Authorization: `Bearer ${token}` } })
            //   .then(response => setUser(response.data))
            //   .catch(() => {
            //     console.error("Failed to fetch user details with stored token.");
            //     logout(); // If token is invalid, log out
            //   });
            setupInterceptors(token); // Set up axios interceptors with the token
        }
    }, []);

    const login = async (username, password) => {
        try {
            const response = await axios.post(`${API_BASE_URL}/auth/login`, { username, password });
            const { token } = response.data; // Assuming your backend returns { token: "your.jwt.here" }
            console.log("token: ",token);
            localStorage.setItem('jwt_token', token);
            setAuthToken(token);
            setIsAuthenticated(true);
            // Optionally, if your login response includes user details, set them here
            // setUser(response.data.user);
            setupInterceptors(token); // Setup axios interceptors immediately after login
            console.log('Login successful');
            return true;
        } catch (error) {
            console.error('Login failed:', error.response?.data || error.message);
            setIsAuthenticated(false);
            setAuthToken(null);
            localStorage.removeItem('jwt_token');
            // Remove the interceptor or reset axios if needed
            setupInterceptors(null); // Clear interceptor if login fails
            return false;
        }
    };

    const logout = () => {
        setIsAuthenticated(false);
        setUser(null);
        setAuthToken(null);
        localStorage.removeItem('jwt_token');
        setupInterceptors(null); // Clear the token from the axios interceptor
        console.log('Logged out');
    };

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, login, logout, authToken }}>
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook for easier access to AuthContext
export const useAuth = () => {
    return useContext(AuthContext);
};
// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './App.css'; // Global styles
import Header from './components/Header';
import HomePage from './pages/HomePage'; // Import the new HomePage
import ProfilePage from './pages/ProfilePage';
import SocialPostGenerator from './pages/SocialPostGenerator';

function App() {
  return (
    <Router>
      <Header /> {/* Render the Header on all pages */}
      
      <main className="app-main-content"> {/* Use the app-main-content class defined in app.css */}
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/generate" element={<SocialPostGenerator />} />
          <Route path="/profile" element={<ProfilePage />} />
          {/* Add other routes here if you have them */}
        </Routes>
      </main>
    </Router>
  );
}

export default App;
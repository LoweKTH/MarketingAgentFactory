// src/App.js
import React from 'react';
import './App.css'; // Global styles
import Header from './components/header';
import SocialPostGenerator from './pages/SocialPostGenerator';

function App() {
  return (
    <div className="App">
      <Header />
      <main className="app-main-content">
        <SocialPostGenerator />
      </main>
      {/* You can add a footer here if needed */}
    </div>
  );
}

export default App;
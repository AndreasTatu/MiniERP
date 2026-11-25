import React from 'react';
import { createRoot } from 'react-dom/client';
import { AuthGate } from './Auth';
import App from './App';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <React.StrictMode><AuthGate><App /></AuthGate></React.StrictMode>,
);

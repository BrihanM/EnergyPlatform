import React from 'react';
import { LoginForm } from '../components';
import './LoginPage.css';

export const LoginPage: React.FC = () => {
  const handleLogin = (data: any) => {
    console.log('Login data:', data);
  };

  const handleNavigateToSignup = () => {
    console.log('Navigate to signup');
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <LoginForm
          onSubmit={handleLogin}
          onNavigateToSignup={handleNavigateToSignup}
        />
      </div>
    </div>
  );
};

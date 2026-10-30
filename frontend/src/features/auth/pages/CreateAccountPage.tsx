import React from 'react';
import { CreateAccountForm } from '../components';
import './CreateAccountPage.css';

export const CreateAccountPage: React.FC = () => {
  const handleCreateAccount = (data: any) => {
    console.log('Create account data:', data);
  };

  const handleNavigateToLogin = () => {
    console.log('Navigate to login');
  };

  return (
    <div className="create-account-page">
      <div className="create-account-container">
        <CreateAccountForm
          onSubmit={handleCreateAccount}
          onNavigateToLogin={handleNavigateToLogin}
        />
      </div>
    </div>
  );
};

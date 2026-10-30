import React from 'react';
import './Input.css';

interface InputProps {
  id: string;
  type?: 'text' | 'email' | 'password' | 'number';
  placeholder?: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  label?: string;
  error?: string;
  disabled?: boolean;
}

export const Input: React.FC<InputProps> = ({
  id,
  type = 'text',
  placeholder,
  value,
  onChange,
  label,
  error,
  disabled = false,
}) => {
  return (
    <div className="input-wrapper">
      {label && <label htmlFor={id} className="input-label">{label}</label>}
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        disabled={disabled}
        className={`input-field ${error ? 'input-field--error' : ''}`}
      />
      {error && <span className="input-error">{error}</span>}
    </div>
  );
};

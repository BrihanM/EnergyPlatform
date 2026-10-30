import React, { useState } from 'react';
import { Input } from '../../../shared/components/Input';
import { Button } from '../../../shared/components/Button';
import './LoginForm.css';

interface LoginFormProps {
  onSubmit?: (data: LoginFormData) => void;
  onNavigateToSignup?: () => void;
}

export interface LoginFormData {
  email: string;
  password: string;
}

export const LoginForm: React.FC<LoginFormProps> = ({
  onSubmit,
  onNavigateToSignup,
}) => {
  const [formData, setFormData] = useState<LoginFormData>({
    email: '',
    password: '',
  });

  const [errors, setErrors] = useState<Partial<LoginFormData>>({});

  const handleChange = (field: keyof LoginFormData, value: string) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: undefined,
      }));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit?.(formData);
  };

  return (
    <form className="login-form" onSubmit={handleSubmit}>
      <div className="login-header">
        <h1>Iniciar Sesión</h1>
        <p>Accede a tu cuenta</p>
      </div>

      <div className="login-fields">
        <Input
          id="email"
          type="email"
          label="Correo Electrónico"
          placeholder="tu@email.com"
          value={formData.email}
          onChange={(e) => handleChange('email', e.target.value)}
          error={errors.email}
        />

        <Input
          id="password"
          type="password"
          label="Contraseña"
          placeholder="••••••••"
          value={formData.password}
          onChange={(e) => handleChange('password', e.target.value)}
          error={errors.password}
        />
      </div>

      <div className="login-actions">
        <Button type="submit" fullWidth>
          Iniciar Sesión
        </Button>
      </div>

      <div className="login-footer">
        <p>
          ¿No tienes cuenta?{' '}
          <button
            type="button"
            className="login-link"
            onClick={onNavigateToSignup}
          >
            Regístrate aquí
          </button>
        </p>
      </div>
    </form>
  );
};

import React, { useState } from 'react';
import { Input } from '../../../shared/components/Input';
import { Button } from '../../../shared/components/Button';
import './CreateAccountForm.css';

interface CreateAccountFormProps {
  onSubmit?: (data: CreateAccountFormData) => void;
  onNavigateToLogin?: () => void;
}

export interface CreateAccountFormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export const CreateAccountForm: React.FC<CreateAccountFormProps> = ({
  onSubmit,
  onNavigateToLogin,
}) => {
  const [formData, setFormData] = useState<CreateAccountFormData>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState<Partial<CreateAccountFormData>>({});

  const handleChange = (field: keyof CreateAccountFormData, value: string) => {
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
    <form className="create-account-form" onSubmit={handleSubmit}>
      <div className="create-account-header">
        <h1>Crear Cuenta</h1>
        <p>Únete a nosotros hoy</p>
      </div>

      <div className="create-account-fields">
        <div className="create-account-row">
          <Input
            id="firstName"
            type="text"
            label="Nombre"
            placeholder="Juan"
            value={formData.firstName}
            onChange={(e) => handleChange('firstName', e.target.value)}
            error={errors.firstName}
          />
          <Input
            id="lastName"
            type="text"
            label="Apellido"
            placeholder="Pérez"
            value={formData.lastName}
            onChange={(e) => handleChange('lastName', e.target.value)}
            error={errors.lastName}
          />
        </div>

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

        <Input
          id="confirmPassword"
          type="password"
          label="Confirmar Contraseña"
          placeholder="••••••••"
          value={formData.confirmPassword}
          onChange={(e) => handleChange('confirmPassword', e.target.value)}
          error={errors.confirmPassword}
        />
      </div>

      <div className="create-account-actions">
        <Button type="submit" fullWidth>
          Crear Cuenta
        </Button>
      </div>

      <div className="create-account-footer">
        <p>
          ¿Ya tienes cuenta?{' '}
          <button
            type="button"
            className="create-account-link"
            onClick={onNavigateToLogin}
          >
            Inicia sesión aquí
          </button>
        </p>
      </div>
    </form>
  );
};

import { useState } from 'react'
import './Autenticacao.css'

export default function CampoSenha({
  id,
  label,
  value,
  onChange,
  autoComplete,
  minLength,
  maxLength,
  describedBy,
  autoFocus = false
}) {
  const [visivel, setVisivel] = useState(false)

  return (
    <div className="campo campo-senha">
      <label htmlFor={id}>{label}</label>
      <div className="campo-senha-controle">
        <input
          id={id}
          type={visivel ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          minLength={minLength}
          maxLength={maxLength}
          aria-describedby={describedBy}
          required
          autoFocus={autoFocus}
        />
        <button
          type="button"
          className="alternar-senha"
          onClick={() => setVisivel((atual) => !atual)}
          aria-label={visivel ? `Ocultar ${label.toLowerCase()}` : `Mostrar ${label.toLowerCase()}`}
          aria-pressed={visivel}
        >
          {visivel ? 'Ocultar' : 'Mostrar'}
        </button>
      </div>
    </div>
  )
}

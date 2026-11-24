import { useState } from 'react'
import { erroCnpj, formatarCnpj } from '../../utils/validacao'

export default function CampoCnpj({
  id,
  value,
  onChange,
  erroExterno = '',
  obrigatorio = true
}) {
  const [visitado, setVisitado] = useState(false)
  const erro = erroExterno || (visitado ? erroCnpj(value) : '')
  const descricaoId = `${id}-erro`

  return (
    <div className="campo">
      <label htmlFor={id}>CNPJ{obrigatorio ? ' *' : ''}</label>
      <input
        id={id}
        value={value}
        onChange={(event) => onChange(formatarCnpj(event.target.value))}
        onBlur={() => setVisitado(true)}
        placeholder="00.000.000/0000-00"
        inputMode="numeric"
        autoComplete="off"
        maxLength={18}
        aria-invalid={Boolean(erro)}
        aria-describedby={erro ? descricaoId : undefined}
        required={obrigatorio}
      />
      {erro && <small id={descricaoId} className="campo-erro" role="alert">{erro}</small>}
    </div>
  )
}

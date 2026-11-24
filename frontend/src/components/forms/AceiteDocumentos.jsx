export default function AceiteDocumentos({
  id,
  checked,
  onChange,
  erro = ''
}) {
  const erroId = `${id}-erro`

  return (
    <div className={`aceite-lgpd${erro ? ' campo-invalido' : ''}`}>
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        aria-invalid={Boolean(erro)}
        aria-describedby={erro ? erroId : undefined}
      />
      <div>
        <label htmlFor={id}>
          Li e aceito os <a className="link-texto" href="#termos" target="_blank" rel="noreferrer">Termos de Uso</a>
          {' '}e a <a className="link-texto" href="#privacidade" target="_blank" rel="noreferrer">Política de Privacidade</a>.
        </label>
        {erro && <small id={erroId} className="campo-erro" role="alert">{erro}</small>}
      </div>
    </div>
  )
}

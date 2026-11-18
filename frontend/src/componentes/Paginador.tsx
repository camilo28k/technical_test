interface Props {
  page: number;
  totalPaginas: number;
  totalElementos: number;
  onCambiar: (page: number) => void;
}

export function Paginador({ page, totalPaginas, totalElementos, onCambiar }: Props) {
  if (totalPaginas <= 1) return <p className="nota">{totalElementos} registro(s)</p>;
  return (
    <nav className="paginador" aria-label="Paginación">
      <button type="button" disabled={page === 0} onClick={() => onCambiar(page - 1)}>Anterior</button>
      <span>Página {page + 1} de {totalPaginas} · {totalElementos} registro(s)</span>
      <button type="button" disabled={page + 1 >= totalPaginas} onClick={() => onCambiar(page + 1)}>Siguiente</button>
    </nav>
  );
}

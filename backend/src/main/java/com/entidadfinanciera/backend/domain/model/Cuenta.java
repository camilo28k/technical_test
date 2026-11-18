package com.entidadfinanciera.backend.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import com.entidadfinanciera.backend.domain.exception.CuentaCanceladaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoActivaException;
import com.entidadfinanciera.backend.domain.exception.FondosInsuficientesException;
import com.entidadfinanciera.backend.domain.exception.SaldoDistintoDeCeroException;

/**
 * Cuenta de ahorro o corriente. Propietario, tipo y número no cambian después de la apertura
 * (P-05); el saldo solo cambia mediante movimientos.
 */
public class Cuenta {

	private static final Pattern NUMERO = Pattern.compile("^\\d{10}$");

	private final Long id;
	private final Long clienteId;
	private final TipoCuenta tipo;
	private final String numero;
	private EstadoCuenta estado;
	private BigDecimal saldo;
	private boolean exentaGmf;
	private final OffsetDateTime fechaCreacion;
	private OffsetDateTime fechaModificacion;

	private Cuenta(Long id, Long clienteId, TipoCuenta tipo, String numero, EstadoCuenta estado, BigDecimal saldo,
			boolean exentaGmf, OffsetDateTime fechaCreacion, OffsetDateTime fechaModificacion) {
		this.id = id;
		this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
		this.tipo = Objects.requireNonNull(tipo, "tipo");
		this.numero = Objects.requireNonNull(numero, "numero");
		this.estado = Objects.requireNonNull(estado, "estado");
		this.saldo = Objects.requireNonNull(saldo, "saldo").setScale(2, RoundingMode.UNNECESSARY);
		this.exentaGmf = exentaGmf;
		this.fechaCreacion = fechaCreacion;
		this.fechaModificacion = fechaModificacion;
	}

	/**
	 * Abre una cuenta activa, con saldo cero, sin exención de GMF y fecha de creación automática
	 * (RN-07, RN-09, DA-03, DA-04, P-03).
	 */
	public static Cuenta abrir(Long clienteId, TipoCuenta tipo, String numero, Clock reloj) {
		Objects.requireNonNull(tipo, "tipo");
		if (numero == null || !NUMERO.matcher(numero).matches() || !numero.startsWith(tipo.getPrefijo())) {
			throw new IllegalArgumentException("Número de cuenta inválido para " + tipo + ": " + numero);
		}
		return new Cuenta(null, clienteId, tipo, numero, EstadoCuenta.ACTIVA, BigDecimal.ZERO, false,
				OffsetDateTime.now(reloj), null);
	}

	public static Cuenta reconstruir(Long id, Long clienteId, TipoCuenta tipo, String numero, EstadoCuenta estado,
			BigDecimal saldo, boolean exentaGmf, OffsetDateTime fechaCreacion, OffsetDateTime fechaModificacion) {
		return new Cuenta(id, clienteId, tipo, numero, estado, saldo, exentaGmf, fechaCreacion, fechaModificacion);
	}

	/** Reactiva una cuenta inactiva. Sobre una activa no cambia nada (idempotente). */
	public void activar(Clock reloj) {
		cambiarEstado(EstadoCuenta.ACTIVA, reloj);
	}

	/** Inactiva una cuenta activa. Sobre una inactiva no cambia nada (idempotente). */
	public void inactivar(Clock reloj) {
		cambiarEstado(EstadoCuenta.INACTIVA, reloj);
	}

	/** Cancela la cuenta de forma definitiva; exige saldo cero (RN-08, DA-03). */
	public void cancelar(Clock reloj) {
		exigirNoCancelada();
		if (saldo.signum() != 0) {
			throw new SaldoDistintoDeCeroException(numero, saldo);
		}
		estado = EstadoCuenta.CANCELADA;
		fechaModificacion = OffsetDateTime.now(reloj);
	}

	/** Actualiza el indicador de exención de GMF (DA-04); un cambio real marca la modificación. */
	public void cambiarExencionGmf(boolean exenta, Clock reloj) {
		exigirNoCancelada();
		if (exentaGmf != exenta) {
			exentaGmf = exenta;
			fechaModificacion = OffsetDateTime.now(reloj);
		}
	}

	/** Suma el importe al saldo (RN-10). Exige cuenta activa (DA-03). */
	public void acreditar(BigDecimal importe, Clock reloj) {
		exigirActiva();
		saldo = saldo.add(importe);
		fechaModificacion = OffsetDateTime.now(reloj);
	}

	/** Resta el importe del saldo (RN-10). Exige cuenta activa y fondos suficientes (DA-02, DA-03). */
	public void debitar(BigDecimal importe, Clock reloj) {
		exigirActiva();
		if (saldo.compareTo(importe) < 0) {
			throw new FondosInsuficientesException(numero, importe);
		}
		saldo = saldo.subtract(importe);
		fechaModificacion = OffsetDateTime.now(reloj);
	}

	private void exigirActiva() {
		if (estado != EstadoCuenta.ACTIVA) {
			throw new CuentaNoActivaException(numero, estado.name());
		}
	}

	private void cambiarEstado(EstadoCuenta nuevo, Clock reloj) {
		exigirNoCancelada();
		if (estado != nuevo) {
			estado = nuevo;
			fechaModificacion = OffsetDateTime.now(reloj);
		}
	}

	private void exigirNoCancelada() {
		if (estado == EstadoCuenta.CANCELADA) {
			throw new CuentaCanceladaException(numero);
		}
	}

	public Long getId() {
		return id;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public String getNumero() {
		return numero;
	}

	public EstadoCuenta getEstado() {
		return estado;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	/** Igual al saldo mientras no existan bloqueos ni retenciones (DA-02). */
	public BigDecimal getSaldoDisponible() {
		return saldo;
	}

	public boolean isExentaGmf() {
		return exentaGmf;
	}

	public OffsetDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public OffsetDateTime getFechaModificacion() {
		return fechaModificacion;
	}

}

import { normalizarImporte, validarImporte } from './formato';

describe('validarImporte', () => {
  it.each(['150000', '150000.5', '0.01'])('acepta %s', (v) => expect(validarImporte(v)).toBeNull());
  it.each(['', 'abc', '-5', '1.234', '1,5'])('rechaza el formato %s', (v) => expect(validarImporte(v)).not.toBeNull());
  it('rechaza cero', () => expect(validarImporte('0.00')).toMatch(/mayor que cero/));
});

describe('normalizarImporte', () => {
  it('completa dos decimales sin perder precisión', () => {
    expect(normalizarImporte('150000')).toBe('150000.00');
    expect(normalizarImporte('7.5')).toBe('7.50');
    expect(normalizarImporte('0012.30')).toBe('12.30');
    expect(normalizarImporte('9007199254740993.01')).toBe('9007199254740993.01');
  });
});

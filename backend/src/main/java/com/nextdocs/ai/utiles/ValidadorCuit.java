package com.nextdocs.ai.utiles;

public final class ValidadorCuit {

	private static final int[] PESOS = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

	private static final int LARGO = 11;

	private ValidadorCuit() {
	}

	public static boolean esValido(String candidato) {
		if (candidato == null) {
			return false;
		}
		String digitos = candidato.replaceAll("\\D", "");
		if (digitos.length() != LARGO) {
			return false;
		}
		int suma = 0;
		for (int posicion = 0; posicion < PESOS.length; posicion++) {
			suma += Character.getNumericValue(digitos.charAt(posicion)) * PESOS[posicion];
		}
		return verificadorDe(suma) == Character.getNumericValue(digitos.charAt(LARGO - 1));
	}

	private static int verificadorDe(int suma) {
		int resto = 11 - (suma % 11);
		if (resto == 11) {
			return 0;
		}
		return resto == 10 ? 9 : resto;
	}
}

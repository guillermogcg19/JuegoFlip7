package server.cartas;

public class Carta {

    private final CartaTipo tipo;
    private final int valor;
    private final String nombre;

    public Carta(CartaTipo tipo, int valor, String nombre) {
        this.tipo = tipo;
        this.valor = valor;
        this.nombre = nombre;
    }

    public CartaTipo getTipo() {
        return tipo;
    }

    public int getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

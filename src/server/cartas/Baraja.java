package server.cartas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Baraja {

    private final List<Carta> mazo = new ArrayList<>();

    public Baraja() {
        reiniciar();
    }

    private void agregarCopias(int valor, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            mazo.add(new Carta(CartaTipo.NUMERO, valor, String.valueOf(valor)));
        }
    }

    public final void reiniciar() {
        mazo.clear();

        // Cartas numericas
        agregarCopias(12, 12);
        agregarCopias(11, 11);
        agregarCopias(10, 10);
        agregarCopias(9, 9);
        agregarCopias(8, 8);
        agregarCopias(7, 7);
        agregarCopias(6, 6);
        agregarCopias(5, 5);
        agregarCopias(4, 4);
        agregarCopias(3, 3);
        agregarCopias(2, 2);
        agregarCopias(1, 1);
        agregarCopias(0, 1);

        // Modificadores
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 2, "+2"));
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 4, "+4"));
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 6, "+6"));
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 8, "+8"));
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 10, "+10"));
        mazo.add(new Carta(CartaTipo.MODIFICADOR, 0, "x2"));

        // Cartas de accion: Freeze, Flip Three, Second Chance
        for (int i = 0; i < 3; i++) {
            mazo.add(new Carta(CartaTipo.ACCION, 0, "Freeze"));
            mazo.add(new Carta(CartaTipo.ACCION, 0, "Flip Three"));
            mazo.add(new Carta(CartaTipo.ACCION, 0, "Second Chance"));
        }

        Collections.shuffle(mazo);
    }

    public Carta robar() {
        if (mazo.isEmpty()) {
            reiniciar();
        }
        if (mazo.isEmpty()) return null;
        return mazo.remove(0);
    }

    public int size() {
        return mazo.size();
    }
}

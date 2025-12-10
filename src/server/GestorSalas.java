package server;

import java.util.HashMap;
import java.util.Map;

public class GestorSalas {

    private static final Map<String, Sala> salas = new HashMap<>();

    public static synchronized Sala obtenerOScrear(String nombreSala) {
        return salas.computeIfAbsent(nombreSala, Sala::new);
    }

    public static synchronized String listarSalas() {
        if (salas.isEmpty()) return "\n(No hay salas creadas)\n";

        StringBuilder sb = new StringBuilder("\nSalas disponibles:\n");
        for (Sala s : salas.values()) {
            sb.append(" - ").append(s.getNombre()).append(" (")
              .append(s.getJugadorCount()).append("/6 jugadores)\n");
        }
        return sb.toString();
    }

    public static synchronized boolean usuarioYaConectado(String nombre) {
        for (Sala sala : salas.values()) {
            for (ClienteHandler j : sala.getJugadores()) {
                if (j.getNombre().equalsIgnoreCase(nombre)) return true;
            }
        }
        return false;
    }

    public static synchronized void limpiarSalasVacias() {
        salas.values().removeIf(Sala::estaVacia);
    }
}

package server;

import java.util.HashMap;

public class GestorSalas {

    private static final HashMap<String, Sala> salas = new HashMap<>();

    public static synchronized Sala obtenerOScrear(String nombre) {
        return salas.computeIfAbsent(nombre, Sala::new);
    }

    public static synchronized String listarSalas() {
        if (salas.isEmpty()) return "No hay salas creadas aun.";

        StringBuilder sb = new StringBuilder("Salas disponibles:\n\n");

        for (Sala sala : salas.values()) {
            sb.append("- ").append(sala.getNombre()).append("\n");
        }
        return sb.toString();
    }
}

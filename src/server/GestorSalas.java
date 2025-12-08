package server;

import java.util.HashMap;

public class GestorSalas {

    private static final HashMap<String, Sala> salas = new HashMap<>();

    public static synchronized Sala obtenerOScrear(String nombreSala) {
        return salas.computeIfAbsent(nombreSala, Sala::new);
    }

    public static synchronized String listarSalas() {
        if (salas.isEmpty()) return "No hay salas creadas aún.";

        StringBuilder sb = new StringBuilder("=== Salas Disponibles ===\n");
        for (Sala sala : salas.values()) {
            sb.append(" - ").append(sala.resumenSala()).append("\n");
        }
        return sb.toString();
    }
}
package server;

import java.util.ArrayList;
import java.util.List;

public class Sala {

    private final String nombreSala;
    private final List<ClienteHandler> jugadores = new ArrayList<>();
    private final List<ClienteHandler> espectadores = new ArrayList<>();
    public static final int MAX_JUGADORES = 6;

    public Sala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public String getNombre() {
        return nombreSala;
    }

    public synchronized boolean agregarJugador(ClienteHandler cliente) {
        if (jugadores.size() < MAX_JUGADORES) {
            jugadores.add(cliente);
            return true;
        }
        return false;
    }

    public synchronized void agregarEspectador(ClienteHandler cliente) {
        espectadores.add(cliente);
    }

    public synchronized void removerCliente(ClienteHandler cliente) {
        jugadores.remove(cliente);
        espectadores.remove(cliente);
    }

    public synchronized void broadcast(String mensaje) {
        for (ClienteHandler c : jugadores) {
            c.enviar(mensaje);
        }
        for (ClienteHandler c : espectadores) {
            c.enviar(mensaje);
        }
    }

    public synchronized String resumenSala() {
        return "[Sala: " + nombreSala + " | Jugadores: " + jugadores.size() + "/" + MAX_JUGADORES + " | Espectadores: " + espectadores.size() + "]";
    }

    public synchronized String resumenCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Sala: ").append(nombreSala).append(" ===\n");

        sb.append("\nJugadores (" + jugadores.size() + "/" + MAX_JUGADORES + "):\n");
        for (ClienteHandler j : jugadores) {
            sb.append(" - ").append(j.getNombre()).append("\n");
        }

        sb.append("\nEspectadores (" + espectadores.size() + "):\n");
        for (ClienteHandler e : espectadores) {
            sb.append(" - ").append(e.getNombre()).append("\n");
        }

        return sb.toString();
    }

}

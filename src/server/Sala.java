package server;

import server.cartas.*;
import java.util.*;

public class Sala {

    private final String nombreSala;
    private final List<ClienteHandler> jugadores = new ArrayList<>();
    private final List<ClienteHandler> espectadores = new ArrayList<>();
    private final List<ClienteHandler> eliminados = new ArrayList<>();
    public static final int MAX_JUGADORES = 6;

    private final Baraja baraja = new Baraja();

    public Sala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public String getNombre() {
        return nombreSala;
    }

    public synchronized boolean agregarJugador(ClienteHandler jugador) {
        if (jugadores.size() < MAX_JUGADORES) {
            jugadores.add(jugador);
            return true;
        }
        return false;
    }

    public synchronized void agregarEspectador(ClienteHandler jugador) {
        espectadores.add(jugador);
    }

    public synchronized void removerCliente(ClienteHandler jugador) {
        jugadores.remove(jugador);
        espectadores.remove(jugador);
        eliminados.remove(jugador);
    }

    public synchronized void broadcast(String mensaje) {
        for (ClienteHandler c : jugadores) c.enviar(mensaje);
        for (ClienteHandler c : espectadores) c.enviar(mensaje);
    }

    public synchronized String resumenCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sala: ").append(nombreSala).append("\n");

        sb.append("\nJugadores: ").append(jugadores.size()).append("/").append(MAX_JUGADORES).append("\n");
        for (ClienteHandler j : jugadores) sb.append("- ").append(j.getNombre()).append("\n");

        sb.append("\nEspectadores: ").append(espectadores.size()).append("\n");
        for (ClienteHandler e : espectadores) sb.append("- ").append(e.getNombre()).append("\n");

        return sb.toString();
    }

    public synchronized Carta robarCarta() {
        return baraja.robar();
    }

    public synchronized void eliminarJugador(ClienteHandler jugador) {
        jugadores.remove(jugador);
        eliminados.add(jugador);
    }
}

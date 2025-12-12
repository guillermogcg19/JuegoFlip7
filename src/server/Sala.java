package server;

import server.cartas.*;
import java.util.*;

public class Sala {

    private final String nombre;
    private final List<ClienteHandler> jugadores = new ArrayList<>();
    private final List<ClienteHandler> espectadores = new ArrayList<>();
    private final Baraja baraja = new Baraja();
    private final List<String> log = new ArrayList<>();

    private ClienteHandler creador;
    private boolean partidaIniciada = false;
    private boolean rondaActiva = false;
    private boolean partidaTerminada = false;

    private int turnoIndex = 0;
    private ClienteHandler bonusJugador7;

    public Sala(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
    public List<ClienteHandler> getJugadores() { return jugadores; }
    public int getJugadorCount() { return jugadores.size(); }

    // -------------------------
    //  Gestión de jugadores
    // -------------------------
    public synchronized boolean agregarJugador(ClienteHandler c) {
        if (jugadores.size() >= 6) return false;

        jugadores.add(c);

        if (creador == null) creador = c;

        return true;
    }

    public synchronized void agregarEspectador(ClienteHandler c) {
        if (!espectadores.contains(c)) espectadores.add(c);
    }

    public synchronized boolean esJugador(ClienteHandler c) {
        return jugadores.contains(c);
    }

    public synchronized void removerJugador(ClienteHandler c) {
        jugadores.remove(c);
        espectadores.remove(c);

        if (creador == c && !jugadores.isEmpty()) {
            creador = jugadores.get(0);
            agregarLog("Nuevo creador: " + creador.getNombre());
        }

        GestorSalas.limpiarSalasVacias();
    }

    public synchronized boolean puedeIniciar(ClienteHandler jugador) {
        return jugador == creador && jugadores.size() >= 2;
    }

    // -------------------------
    //    UI Lobby
    // -------------------------
    public synchronized String generarLobby(ClienteHandler jugador) {
        StringBuilder sb = new StringBuilder("\n====== Lobby ======\n");
        sb.append("Sala: ").append(nombre).append("\n");
        sb.append("Jugadores: ").append(jugadores.size()).append("/6\n");
        sb.append("Creador: ").append(creador.getNombre()).append("\n\n");

        sb.append("Conectados:\n");
        for (ClienteHandler p : jugadores) sb.append(" - ").append(p.getNombre()).append("\n");

        sb.append("\nOpciones:\n");

        if (partidaTerminada) {
            if (jugador == creador && jugadores.size() >= 2) {
                sb.append("1) Revancha (nueva partida, reinicia puntajes)\n");
                sb.append("2) Salir\n");
            } else {
                sb.append("(La partida ha terminado. Esperando decision del creador sobre la revancha...)\n");
                sb.append("2) Salir\n");
            }
        } else {
            if (puedeIniciar(jugador)) sb.append("1) Iniciar partida\n");
            else sb.append("(Esperando al creador...)\n");

            sb.append("2) Salir\n");
        }

        sb.append("\nTip: escribe :mensaje para chatear con la sala.\n");

        return sb.toString();
    }

    // -------------------------
    //     Partida / Rondas
    // -------------------------
    public synchronized void iniciarPartida() {
        if (!puedeIniciar(creador)) return;

        if (partidaTerminada) {
            for (ClienteHandler p : jugadores) {
                p.reiniciarPuntuacion();
            }
            partidaTerminada = false;
        }

        baraja.reiniciar();

        partidaIniciada = true;
        iniciarNuevaRonda();
        agregarLog("La partida ha comenzado.");
    }

    public synchronized boolean estaRondaActiva() { return rondaActiva; }

    private synchronized void iniciarNuevaRonda() {

        rondaActiva = true;
        turnoIndex = 0;
        bonusJugador7 = null;

        log.clear();

        for (ClienteHandler p : jugadores) {
            p.iniciarNuevaRonda();
        }

        agregarLog("Nueva ronda iniciada");
    }

    // -------------------------
    //     Turnos
    // -------------------------
    public synchronized ClienteHandler turnoActual() {
        if (!rondaActiva || jugadores.isEmpty()) return null;
        if (turnoIndex >= jugadores.size()) turnoIndex = 0;
        return jugadores.get(turnoIndex);
    }

    public synchronized void siguienteTurno() {
        if (jugadores.isEmpty()) return;
        turnoIndex = (turnoIndex + 1) % jugadores.size();
    }

    // -------------------------
    //   Robar carta
    // -------------------------
    public synchronized Carta robarCarta() {
        Carta c = baraja.robar();
        agregarLog("Cartas restantes en mazo: " + baraja.size());
        return c;
    }

    public synchronized boolean hayDuplicado(ClienteHandler jugador, Carta carta) {
        return jugador.getMano().stream().anyMatch(c ->
                (c.getTipo() == CartaTipo.NUMERO &&
                 carta.getTipo() == CartaTipo.NUMERO &&
                 c.getValor() == carta.getValor()));
    }

    // -------------------------
    //   Bonus 7 cartas
    // -------------------------
    public synchronized void asignarBonus7(ClienteHandler jugador) {
        if (bonusJugador7 == null) {
            bonusJugador7 = jugador;
        }
    }

    // -------------------------
    //   Finalizar ronda
    // -------------------------
    public synchronized void verificarFinRonda() {

        int vivos = (int) jugadores.stream().filter(j -> !j.eliminado).count();
        boolean todosPlantados = jugadores.stream()
                .allMatch(j -> j.eliminado || j.plantado || j.congelado);

        if (bonusJugador7 != null || vivos <= 1 || todosPlantados) {
            finalizarRonda();
        }
    }

    public synchronized void forzarFinRonda() {
        finalizarRonda();
    }

    private synchronized void finalizarRonda() {

        if (!rondaActiva) return;

        rondaActiva = false;

        StringBuilder sb = new StringBuilder("\nResultados de la ronda:\n");

        for (ClienteHandler p : jugadores) {
            int puntos = p.calcularPuntosRonda(p == bonusJugador7);
            p.sumarPuntos(puntos);
            sb.append("- ").append(p.getNombre()).append(": +")
              .append(puntos).append(" (Total: ").append(p.getPuntosTotales()).append(")\n");
        }

        boolean hayGanador = jugadores.stream()
                .anyMatch(p -> p.getPuntosTotales() >= 200);

        if (hayGanador) {
            partidaTerminada = true;

            List<ClienteHandler> ordenados = new ArrayList<>(jugadores);
            ordenados.sort((a, b) -> Integer.compare(b.getPuntosTotales(), a.getPuntosTotales()));

            sb.append("\n=== Clasificacion final ===\n");
            int lugar = 1;
            for (ClienteHandler p : ordenados) {
                sb.append(lugar++).append(") ")
                  .append(p.getNombre())
                  .append(" - ")
                  .append(p.getPuntosTotales())
                  .append(" puntos\n");
            }

            sb.append("\nLa partida ha terminado (alguien alcanzo 200 puntos o mas).\n");
            sb.append("El creador puede decidir si hay revancha desde el lobby.\n");

        } else {
            agregarLog("Ronda finalizada. Comenzando otra...");
        }

        String msg = sb.toString();
        jugadores.forEach(j -> j.enviar(msg));
        espectadores.forEach(e -> e.enviar(msg));

        if (!hayGanador) {
            iniciarNuevaRonda();
        }
    }

    // -------------------------
    //     UI Turno
    // -------------------------
    public synchronized String generarPantallaTurno(ClienteHandler jugador) {

        StringBuilder sb = new StringBuilder("\n----------------------------------------\n");
        sb.append("Sala: ").append(nombre).append("\n");

        ClienteHandler turno = turnoActual();
        sb.append("Turno actual: ").append(turno != null ? turno.getNombre() : "—").append("\n");

        sb.append("Puntos totales: ").append(jugador.getPuntosTotales()).append("\n");
        sb.append("Cartas en mazo: ").append(baraja.size()).append("\n");
        sb.append("Tus cartas: ").append(jugador.getMano()).append("\n");

        sb.append("\nHistorial:\n");
        log.stream().skip(Math.max(0, log.size() - 5)).forEach(x -> sb.append(" - ").append(x).append("\n"));

        sb.append("\nTip: escribe :mensaje para chatear con la sala.\n");
        sb.append("----------------------------------------\n");

        return sb.toString();
    }

    // -------------------------
    //     Logging
    // -------------------------
    public synchronized void agregarLog(String msg) {
        log.add(msg);
        jugadores.forEach(p -> p.enviar("[LOG] " + msg));
        espectadores.forEach(e -> e.enviar("[LOG] " + msg));
    }

    public synchronized boolean estaVacia() {
        return jugadores.isEmpty() && espectadores.isEmpty();
    }

    // -------------------------
    //     Chat
    // -------------------------
    public synchronized void enviarChat(ClienteHandler emisor, String mensaje) {
        String linea = "[CHAT] " + emisor.getNombre() + ": " + mensaje;
        jugadores.forEach(j -> j.enviar(linea));
        espectadores.forEach(e -> e.enviar(linea));
    }

    // Helpers para acciones
    public synchronized List<ClienteHandler> getJugadoresVivos() {
        List<ClienteHandler> vivos = new ArrayList<>();
        for (ClienteHandler c : jugadores) {
            if (!c.eliminado) vivos.add(c);
        }
        return vivos;
    }

    public synchronized List<ClienteHandler> getJugadoresVivosExcepto(ClienteHandler yo) {
        List<ClienteHandler> vivos = new ArrayList<>();
        for (ClienteHandler c : jugadores) {
            if (c != yo && !c.eliminado) vivos.add(c);
        }
        return vivos;
    }
}

package server;

import server.cartas.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class ClienteHandler extends Thread {

    private final Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;

    private String nombre;
    private Sala salaActual;

    private final List<Carta> mano = new ArrayList<>();

    public boolean eliminado = false;
    public boolean plantado = false;
    public boolean congelado = false;
    public boolean segundaOportunidad = false;

    private int puntosTotales = 0;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            autenticarUsuario();
            seleccionarSala();
            cicloJuego();

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + nombre);
        } finally {
            cerrarConexion();
        }
    }

    // ------------------------------
    // Lector con soporte de chat
    // ------------------------------
    private String leerEntradaConChat() throws IOException {
        while (true) {
            String linea = entrada.readUTF();
            if (linea == null) return null;

            linea = linea.trim();

            // Si empieza con ':' y ya está en una sala, lo tomamos como chat
            if (linea.startsWith(":") && salaActual != null) {
                String msg = linea.substring(1).trim();
                if (!msg.isEmpty()) {
                    salaActual.enviarChat(this, msg);
                }
                // Volvemos a esperar entrada, no retornamos esto como comando de juego
                continue;
            }

            return linea;
        }
    }

    private int leerOpcion(int min, int max) throws IOException {
        while (true) {
            String linea = leerEntradaConChat();
            try {
                int op = Integer.parseInt(linea);
                if (op >= min && op <= max) return op;
            } catch (Exception ignored) {}
            enviar("Opción inválida.");
        }
    }

    // ------------------------------
    //    Login
    // ------------------------------
    private void autenticarUsuario() throws IOException {
        enviar("Flip7\n1) Iniciar sesion\n2) Registrarse");

        int opcion = leerOpcion(1, 2);

        while (true) {
            enviar("Usuario:");
            String user = leerEntradaConChat();

            enviar("Password:");
            String pass = leerEntradaConChat();

            // Comprobar que no exista ya conectado
            if (ServidorFlip7.usuarioYaConectado(user)) {
                enviar("Ese usuario ya está conectado.");
                continue;
            }

            if (opcion == 1 && ServidorFlip7.getDB().validarLogin(user, pass)) {
                nombre = user;
                enviar("Sesion correcta. Bienvenido " + nombre);
                break;
            }

            if (opcion == 2 && ServidorFlip7.getDB().registrarUsuario(user, pass)) {
                nombre = user;
                enviar("Registro exitoso. Bienvenido " + nombre);
                break;
            }

            enviar("Datos incorrectos, intenta de nuevo.");
        }
    }

    // ------------------------------
    //    Sala
    // ------------------------------
    private void seleccionarSala() throws IOException {

        enviar("Seleccion de sala:\n1) Ver salas\n2) Crear o unirse");

        int op = leerOpcion(1, 2);

        if (op == 1) enviar(GestorSalas.listarSalas());

        enviar("Nombre de sala:");
        String nombreSala = leerEntradaConChat();
        salaActual = GestorSalas.obtenerOScrear(nombreSala);

        boolean pudoEntrar = salaActual.agregarJugador(this);

        if (!pudoEntrar) {
            salaActual.agregarEspectador(this);
            enviar("La sala está llena, entras como espectador.");
        } else {
            salaActual.agregarLog(nombre + " entro como jugador.");
        }
    }

    // ------------------------------
    //    Ciclo principal
    // ------------------------------
    private void cicloJuego() throws Exception {

        while (true) {

            if (!salaActual.estaRondaActiva() && salaActual.esJugador(this)) {

                enviar(salaActual.generarLobby(this));

                if (salaActual.puedeIniciar(this)) {
                    int op = leerOpcion(1, 2);
                    if (op == 1) salaActual.iniciarPartida();
                    continue;
                }

                Thread.sleep(400);
                continue;
            }

            if (!salaActual.estaRondaActiva() || !salaActual.esJugador(this)) {
                Thread.sleep(300);
                continue;
            }

            // Eliminado o plantado solo esperan que termine la ronda
            if (eliminado || plantado) {
                Thread.sleep(300);
                continue;
            }

            //  Lógica de Freeze: Si es mi turno y estoy congelado, salto turno
            if (salaActual.turnoActual() == this && congelado) {
                salaActual.agregarLog(nombre + " está congelado y pierde su turno.");
                salaActual.siguienteTurno();
                continue;
            }

            // Si no es mi turno, espero
            if (salaActual.turnoActual() != this) {
                Thread.sleep(200);
                continue;
            }

            // Aquí sí es mi turno y puedo jugar
            turnoInteractivo();
        }
    }

    // ------------------------------
    //    Turno
    // ------------------------------
    private void turnoInteractivo() throws IOException {

        enviar(salaActual.generarPantallaTurno(this));

        enviar("\n1) Robar carta\n2) Plantarse\nOpcion:");

        int decision = leerOpcion(1, 2);

        if (decision == 1) robarCarta();
        else {
            plantado = true;
            salaActual.agregarLog(nombre + " se plantó.");
        }

        salaActual.verificarFinRonda();
        salaActual.siguienteTurno();
    }

    // ------------------------------
    //    Robar carta
    // ------------------------------
    private void robarCarta() throws IOException {

        Carta carta = salaActual.robarCarta();
        salaActual.agregarLog(nombre + " robó " + carta);

        if (salaActual.hayDuplicado(this, carta)) {

            if (segundaOportunidad) {
                segundaOportunidad = false;
                salaActual.agregarLog(nombre + " usó Second Chance.");
            } else {
                eliminado = true;
                salaActual.agregarLog(nombre + " fue eliminado por duplicado.");
                return;
            }
        }

        if (carta.getTipo() == CartaTipo.ACCION) {
            procesarAccion(carta);
        } else {
            mano.add(carta);
        }

        //  Solo las cartas numéricas cuentan para el bonus de 7
        long numericas = mano.stream()
                .filter(c -> c.getTipo() == CartaTipo.NUMERO)
                .count();

        if (numericas >= 7) {
            salaActual.asignarBonus7(this);
            salaActual.agregarLog(nombre + " llegó a 7 cartas numéricas. Ronda finalizada con bonus.");
            salaActual.forzarFinRonda();
        }
    }

    // ------------------------------
    // Acciones especiales
    // ------------------------------
    private void procesarAccion(Carta carta) throws IOException {

        switch (carta.toString()) {

            case "Second Chance":
                segundaOportunidad = true;
                salaActual.agregarLog(nombre + " ganó Second Chance.");
                break;

            case "Freeze":
                aplicarFreeze();
                break;

            case "Flip Three":
                aplicarFlipThree();
                break;
        }
    }

    private void aplicarFreeze() throws IOException {

        List<ClienteHandler> vivos = salaActual.getJugadoresVivosExcepto(this);

        if (vivos.isEmpty()) {
            salaActual.agregarLog(nombre + " intentó Freeze pero no hay objetivos.");
            return;
        }

        enviar("Seleccione jugador a congelar:");

        for (int i = 0; i < vivos.size(); i++) {
            enviar((i + 1) + ") " + vivos.get(i).getNombre());
        }

        int op = leerOpcion(1, vivos.size());
        ClienteHandler objetivo = vivos.get(op - 1);

        objetivo.congelado = true;

        salaActual.agregarLog(nombre + " aplicó Freeze a " + objetivo.getNombre());
    }

    //  Flip Three mejorado: recursividad en acciones y check de 7 cartas
    private void aplicarFlipThree() throws IOException {

        List<ClienteHandler> vivos = salaActual.getJugadoresVivos();

        // Lista de opciones EXACTAMENTE como las mostramos
        List<ClienteHandler> opciones = new ArrayList<>();
        opciones.add(this); // 1) Yo mismo

        enviar("Flip Three — Seleccione destino:\n1) Yo mismo");

        int numeroOpcion = 2;
        for (ClienteHandler j : vivos) {
            if (j != this) {
                enviar(numeroOpcion + ") " + j.getNombre());
                opciones.add(j);   // 2) kaka, 3) otro, etc.
                numeroOpcion++;
            }
        }

        int target = leerOpcion(1, opciones.size());
        ClienteHandler objetivo = opciones.get(target - 1);

        salaActual.agregarLog(nombre + " aplicó Flip Three a " + objetivo.getNombre());

        for (int i = 0; i < 3 && !objetivo.eliminado; i++) {

            Carta extra = salaActual.robarCarta();
            salaActual.agregarLog(objetivo.getNombre() + " robó (FlipThree) " + extra);

            if (salaActual.hayDuplicado(objetivo, extra)) {

                if (objetivo.segundaOportunidad) {
                    objetivo.segundaOportunidad = false;
                    salaActual.agregarLog(objetivo.getNombre() + " usó Second Chance.");
                } else {
                    objetivo.eliminado = true;
                    salaActual.agregarLog(objetivo.getNombre() + " fue eliminado.");
                    break;
                }
            }

            // Si sale una acción durante Flip Three, el objetivo la ejecuta
            if (extra.getTipo() == CartaTipo.ACCION) {
                try {
                    // Accedemos a procesarAccion del objetivo (es privado pero estamos en la misma clase)
                    objetivo.procesarAccion(extra);
                } catch (IOException e) {
                    salaActual.agregarLog("Error procesando acción de FlipThree para " + objetivo.getNombre());
                    objetivo.eliminado = true;
                    break;
                }
            } else {
                objetivo.getMano().add(extra);
            }

            // Check bonus 7 dentro del loop de Flip Three
            long numericas = objetivo.getMano().stream()
                    .filter(c -> c.getTipo() == CartaTipo.NUMERO)
                    .count();

            if (numericas >= 7) {
                salaActual.asignarBonus7(objetivo);
                salaActual.forzarFinRonda();
                break;
            }
        }
    }

    // ------------------------------
    // Ronda
    // ------------------------------
    public void iniciarNuevaRonda() {

        mano.clear();
        eliminado = false;
        plantado = false;
        congelado = false; // Se resetea el congelado al iniciar ronda
        segundaOportunidad = false;

        Carta inicial = salaActual.robarCarta();
        salaActual.agregarLog(nombre + " recibió carta inicial " + inicial);

        if (inicial.getTipo() == CartaTipo.ACCION) {
            try {
                procesarAccion(inicial);
            } catch (IOException e) {
                salaActual.agregarLog("Error procesando acción inicial de " + nombre);
                eliminado = true;
            }
        } else {
            mano.add(inicial);
        }
    }

    // ------------------------------
    //  Puntos
    // ------------------------------
    public int calcularPuntosRonda(boolean bonus7) {

        if (eliminado) return 0;

        // Números + modificadores (+2, +4, etc) cuentan al total
        int suma = mano.stream()
                .filter(c -> c.getTipo() == CartaTipo.NUMERO || c.getTipo() == CartaTipo.MODIFICADOR)
                .mapToInt(Carta::getValor)
                .sum();

        boolean x2 = mano.stream().anyMatch(c -> c.toString().equals("x2"));
        if (x2) suma *= 2;

        if (bonus7) suma += 15;

        return Math.max(suma, 0);
    }

    public void sumarPuntos(int p) {
        puntosTotales += p;
    }

    public int getPuntosTotales() {
        return puntosTotales;
    }

    public List<Carta> getMano() {
        return mano;
    }

    public String getNombre() {
        return nombre;
    }

    // ------------------------------
    // Utilidades
    // ------------------------------
    public synchronized void enviar(String msg) {
        try {
            salida.writeUTF("\n" + msg);
            salida.flush();
        } catch (Exception ignored) {}
    }

    private void cerrarConexion() {
        try {
            if (salaActual != null) salaActual.removerJugador(this);
            ServidorFlip7.removerCliente(this);
            socket.close();
        } catch (IOException ignored) {}
    }
    public void reiniciarPuntuacion() {
        puntosTotales = 0;
    }
}
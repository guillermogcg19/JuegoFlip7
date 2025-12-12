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

            if (linea.startsWith(":") && salaActual != null) {
                String msg = linea.substring(1).trim();
                if (!msg.isEmpty()) {
                    salaActual.enviarChat(this, msg);
                }
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
            enviar("Opcion invalida.");
        }
    }

    // ------------------------------
    //    Login con menu y regresar
    // ------------------------------
    private void autenticarUsuario() throws IOException {

        while (true) {
            enviar("Flip7\n1) Iniciar sesion\n2) Registrarse\n3) Salir");

            int opcion = leerOpcion(1, 3);

            if (opcion == 3) {
                throw new IOException("Usuario salio en login");
            }

            while (true) {
                enviar("Usuario (o 0 para volver al menu principal):");
                String user = leerEntradaConChat();

                if ("0".equals(user)) {
                    break;
                }

                enviar("Password (o 0 para volver al menu principal):");
                String pass = leerEntradaConChat();

                if ("0".equals(pass)) {
                    break;
                }

                if (ServidorFlip7.usuarioYaConectado(user)) {
                    enviar("Ese usuario ya esta conectado.");
                    continue;
                }

                if (opcion == 1 && ServidorFlip7.getDB().validarLogin(user, pass)) {
                    nombre = user;
                    enviar("Sesion correcta. Bienvenido " + nombre);
                    return;
                }

                if (opcion == 2 && ServidorFlip7.getDB().registrarUsuario(user, pass)) {
                    nombre = user;
                    enviar("Registro exitoso. Bienvenido " + nombre);
                    return;
                }

                enviar("Datos incorrectos, intenta de nuevo o escribe 0 en usuario/password para volver al menu.");
            }
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
            enviar("La sala esta llena, entras como espectador.");
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
                    if (op == 1) {
                        salaActual.iniciarPartida();
                    } else if (op == 2) {
                        return;
                    }
                } else {
                    int op = leerOpcion(2, 2);
                    if (op == 2) {
                        return;
                    }
                }

                continue;
            }

            if (!salaActual.estaRondaActiva() || !salaActual.esJugador(this)) {
                Thread.sleep(300);
                continue;
            }

            if (eliminado || plantado) {
                Thread.sleep(300);
                continue;
            }

            if (salaActual.turnoActual() == this && congelado) {
                salaActual.agregarLog(nombre + " esta congelado y pierde su turno.");
                salaActual.siguienteTurno();
                continue;
            }

            if (salaActual.turnoActual() != this) {
                Thread.sleep(200);
                continue;
            }

            turnoInteractivo();
        }
    }

    // ------------------------------
    //    Turno
    // ------------------------------
    private void turnoInteractivo() throws IOException {

        enviar(salaActual.generarPantallaTurno(this));

        enviar("\n1) Robar carta\n2) Plantarse\n3) Salir\nOpcion:");

        int decision = leerOpcion(1, 3);

        if (decision == 3) {
            throw new IOException("Jugador solicito salir durante su turno");
        }

        if (decision == 1) {
            robarCarta();
        } else {
            plantado = true;
            salaActual.agregarLog(nombre + " se planto.");
        }

        salaActual.verificarFinRonda();
        salaActual.siguienteTurno();
    }

    // ------------------------------
    //    Robar carta
    // ------------------------------
    private void robarCarta() throws IOException {

        Carta carta = salaActual.robarCarta();
        salaActual.agregarLog(nombre + " robo " + carta);

        if (salaActual.hayDuplicado(this, carta)) {

            if (segundaOportunidad) {
                segundaOportunidad = false;
                salaActual.agregarLog(nombre + " uso Second Chance.");
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

        long numericas = mano.stream()
                .filter(c -> c.getTipo() == CartaTipo.NUMERO)
                .count();

        if (numericas >= 7) {
            salaActual.asignarBonus7(this);
            salaActual.agregarLog(nombre + " llego a 7 cartas numericas. Ronda finalizada con bonus.");
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
                salaActual.agregarLog(nombre + " gano Second Chance.");
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
            salaActual.agregarLog(nombre + " intento Freeze pero no hay objetivos.");
            return;
        }

        enviar("Seleccione jugador a congelar:");

        for (int i = 0; i < vivos.size(); i++) {
            enviar((i + 1) + ") " + vivos.get(i).getNombre());
        }

        int op = leerOpcion(1, vivos.size());
        ClienteHandler objetivo = vivos.get(op - 1);

        objetivo.congelado = true;

        salaActual.agregarLog(nombre + " aplico Freeze a " + objetivo.getNombre());
    }

    private void aplicarFlipThree() throws IOException {

        List<ClienteHandler> vivos = salaActual.getJugadoresVivos();

        List<ClienteHandler> opciones = new ArrayList<>();
        opciones.add(this);

        enviar("Flip Three — Seleccione destino:\n1) Yo mismo");

        int numeroOpcion = 2;
        for (ClienteHandler j : vivos) {
            if (j != this) {
                enviar(numeroOpcion + ") " + j.getNombre());
                opciones.add(j);
                numeroOpcion++;
            }
        }

        int target = leerOpcion(1, opciones.size());
        ClienteHandler objetivo = opciones.get(target - 1);

        salaActual.agregarLog(nombre + " aplico Flip Three a " + objetivo.getNombre());

        for (int i = 0; i < 3 && !objetivo.eliminado; i++) {

            Carta extra = salaActual.robarCarta();
            salaActual.agregarLog(objetivo.getNombre() + " robo (FlipThree) " + extra);

            if (salaActual.hayDuplicado(objetivo, extra)) {

                if (objetivo.segundaOportunidad) {
                    objetivo.segundaOportunidad = false;
                    salaActual.agregarLog(objetivo.getNombre() + " uso Second Chance.");
                } else {
                    objetivo.eliminado = true;
                    salaActual.agregarLog(objetivo.getNombre() + " fue eliminado.");
                    break;
                }
            }

            if (extra.getTipo() == CartaTipo.ACCION) {
                try {
                    objetivo.procesarAccion(extra);
                } catch (IOException e) {
                    salaActual.agregarLog("Error procesando accion de FlipThree para " + objetivo.getNombre());
                    objetivo.eliminado = true;
                    break;
                }
            } else {
                objetivo.getMano().add(extra);
            }

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
        congelado = false;
        segundaOportunidad = false;

        Carta inicial = salaActual.robarCarta();
        salaActual.agregarLog(nombre + " recibio carta inicial " + inicial);

        if (inicial.getTipo() == CartaTipo.ACCION) {
            try {
                procesarAccion(inicial);
            } catch (IOException e) {
                salaActual.agregarLog("Error procesando accion inicial de " + nombre);
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

    public void reiniciarPuntuacion() {
        puntosTotales = 0;
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
            if (salaActual != null) {
                salaActual.agregarLog(nombre + " se ha desconectado. Puede volver a unirse escribiendo el nombre de la sala de nuevo.");
                salaActual.removerJugador(this);
            }
            ServidorFlip7.removerCliente(this);
            socket.close();
        } catch (IOException ignored) {}
    }
}

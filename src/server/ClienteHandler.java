package server;

import server.cartas.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClienteHandler extends Thread {

    private final Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private String nombre;
    private Sala salaActual;
    private final List<Carta> mano = new ArrayList<>();

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            enviar("Flip7");
            enviar("1) Iniciar sesion");
            enviar("2) Registrarse");

            int opcion = validarOpcion(2);

            while (true) {
                enviar("Usuario:");
                String user = entrada.readUTF();

                enviar("Password:");
                String pass = entrada.readUTF();

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

                enviar("Datos incorrectos. Intenta nuevamente.");
            }

            enviar("Seleccion de sala:");
            enviar("1) Ver salas");
            enviar("2) Crear o unirse a una sala");

            int opcSala = validarOpcion(2);

            if (opcSala == 1) enviar(GestorSalas.listarSalas());

            enviar("Nombre de sala:");
            salaActual = GestorSalas.obtenerOScrear(entrada.readUTF());

            if (salaActual.agregarJugador(this)) {
                salaActual.broadcast(nombre + " entro como jugador.");
                repartirCartaInicial();
            } else {
                salaActual.agregarEspectador(this);
                salaActual.broadcast(nombre + " entro como espectador.");
            }

            enviar("Comandos disponibles:");
            enviar("/cartas");
            enviar("/robar");
            enviar("/lista");
            enviar("/renombrar NOMBRE");
            enviar("/salir");

            while (true) {
                String msg = entrada.readUTF().trim();

                if (msg.equalsIgnoreCase("/salir")) break;
                if (msg.equalsIgnoreCase("/lista")) { enviar(salaActual.resumenCompleto()); continue; }
                if (msg.equalsIgnoreCase("/cartas")) { enviar("Tus cartas: " + mano); continue; }
                if (msg.equalsIgnoreCase("/robar")) { robarCarta(); continue; }
                if (msg.startsWith("/renombrar ")) {
                    String nuevo = msg.replace("/renombrar ", "");
                    salaActual.broadcast(nombre + " ahora es " + nuevo);
                    nombre = nuevo;
                    continue;
                }

                salaActual.broadcast(nombre + ": " + msg);
            }

        } catch (Exception e) {
            System.out.println("Error con cliente: " + nombre);
        }

        cerrar();
    }

    private int validarOpcion(int max) throws IOException {
        while (true) {
            try {
                int op = Integer.parseInt(entrada.readUTF());
                if (op >= 1 && op <= max) return op;
            } catch (Exception ignored) {}
            enviar("Opcion invalida.");
        }
    }

    private void repartirCartaInicial() {
        Carta carta = salaActual.robarCarta();
        mano.add(carta);
        enviar("Tu carta inicial: " + carta);
    }

    private void robarCarta() {
        Carta nueva = salaActual.robarCarta();
        if (nueva == null) { enviar("No hay mas cartas."); return; }

        boolean repetida = mano.stream()
                .anyMatch(c -> c.getTipo()==CartaTipo.NUMERO 
                        && nueva.getTipo()==CartaTipo.NUMERO 
                        && c.getValor()==nueva.getValor());

        if (repetida) {
            salaActual.broadcast(nombre + " robo carta repetida y quedo fuera.");
            salaActual.eliminarJugador(this);
            return;
        }

        mano.add(nueva);
        enviar("Robaste: " + nueva);
        salaActual.broadcast(nombre + " robo una carta.");
    }

    public void enviar(String msg) {
        try { salida.writeUTF(msg); } catch (Exception ignored) {}
    }

    private void cerrar() {
        salaActual.broadcast(nombre + " salio.");
        salaActual.removerCliente(this);
        ServidorFlip7.removerCliente(this);
        try { socket.close(); } catch (Exception ignored) {}
    }

    public String getNombre() { return nombre; }
}

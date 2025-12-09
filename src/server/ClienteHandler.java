package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClienteHandler extends Thread {

    private final Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private String nombre;
    private Sala salaActual;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // ---------- AUTENTICACIÓN ----------
            enviar("===== Sistema de Autenticacion Flip7 =====");
            enviar("1) Iniciar sesion");
            enviar("2) Registrarse");

            int opcion = -1;
            while (true) {
                enviar("Selecciona una opcion (1 o 2):");
                try {
                    opcion = Integer.parseInt(entrada.readUTF());
                    if (opcion == 1 || opcion == 2) break;
                    enviar("❌ Opcion invalida. Intenta nuevamente.");
                } catch (Exception e) {
                    enviar("❌ Entrada inválida. Solo escribe 1 o 2.");
                }
            }

            while (true) {
                enviar("Nombre de usuario:");
                String nombreInput = entrada.readUTF();

                enviar("Contraseña:");
                String pass = entrada.readUTF();

                if (opcion == 1) {
                    if (ServidorFlip7.getDB().validarLogin(nombreInput, pass)) {
                        nombre = nombreInput;
                        enviar("Inicio de sesion exitoso. Bienvenido " + nombre + "!");
                        break;
                    } else {
                        enviar("❌ Usuario o contraseña incorrectos. Intenta de nuevo.");
                    }
                } else {
                    if (ServidorFlip7.getDB().registrarUsuario(nombreInput, pass)) {
                        nombre = nombreInput;
                        enviar("✔ Registro exitoso. Bienvenido " + nombre + "!");
                        break;
                    } else {
                        enviar("❌ Ese usuario ya existe. Intenta con otro.");
                    }
                }
            }


            // ----------- SELECCIÓN DE SALA ANTES DEL CHAT ----------
            enviar("\n=== Selección de sala ===");
            enviar("1) Ver salas existentes");
            enviar("2) Crear o unirse a sala");

            int opcionSala = -1;
            while (true) {
                enviar("Escribe 1 o 2:");
                try {
                    opcionSala = Integer.parseInt(entrada.readUTF());
                    if (opcionSala == 1 || opcionSala == 2) break;
                    enviar("❌ Opcion invalida. Intenta nuevamente.");
                } catch (Exception e) {
                    enviar("❌ Entrada inválida. Solo escribe 1 o 2.");
                }
            }

            if (opcionSala == 1) {
                enviar(GestorSalas.listarSalas());
            }

            enviar("Escribe el nombre de la sala para entrar o crear una nueva:");
            String nombreSala = entrada.readUTF();

            salaActual = GestorSalas.obtenerOScrear(nombreSala);

            if (salaActual.agregarJugador(this)) {
                enviar("🟢 Te uniste como JUGADOR a la sala: " + nombreSala);
                salaActual.broadcast("[Sistema] " + nombre + " se unió como jugador.");
            } else {
                salaActual.agregarEspectador(this);
                enviar("🟡 Sala llena. Entraste como ESPECTADOR.");
                salaActual.broadcast("[Sistema] " + nombre + " se unió como espectador.");
            }

            // ----------- MENU DEL CHAT -----------
            enviar("""
                    
===== Comandos disponibles =====
/ayuda         - Mostrar comandos
/lista         - Ver jugadores/espectadores
/renombrar XYZ - Cambiar tu nombre
/salir         - Salir del servidor
================================
""");

            // ----------- BUCLE PRINCIPAL DEL CHAT -----------
            String mensaje;

            while (true) {
                mensaje = entrada.readUTF().trim();

                if (mensaje.equalsIgnoreCase("/salir")) {
                    enviar("👋 Saliendo...");
                    break;
                }

                if (mensaje.equalsIgnoreCase("/lista")) {
                    enviar(salaActual.resumenCompleto());
                    continue;
                }

                if (mensaje.startsWith("/renombrar ")) {
                    String nuevo = mensaje.replace("/renombrar ", "").trim();
                    salaActual.broadcast("[Sistema] " + nombre + " ahora es " + nuevo);
                    nombre = nuevo;
                    continue;
                }

                if (mensaje.equalsIgnoreCase("/ayuda")) {
                    enviar("""
===== Comandos =====
/ayuda
/lista
/renombrar XYZ
/salir
""");
                    continue;
                }

                // --- Mensaje normal ---
                salaActual.broadcast(nombre + ": " + mensaje);
            }

        } catch (Exception e) {
            System.out.println("⚠ Error con cliente: " + nombre);
        } finally {
            cerrar();
        }
    }

    // ---------- MÉTODOS AUXILIARES ----------
    public void enviar(String mensaje) {
        try {
            salida.writeUTF(mensaje);
        } catch (IOException ignored) {}
    }

    private void cerrar() {
        try {
            if (salaActual != null) {
                salaActual.broadcast("[Sistema] " + nombre + " salió de la sala.");
                salaActual.removerCliente(this);
            }
            socket.close();
        } catch (IOException ignored) {}
    }

    public String getNombre() { return nombre; }
}

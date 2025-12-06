package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Maneja a UN cliente:
 * - Recibe mensajes.
 * - Los reenvía (broadcast) al resto.
 * - Soporta el comando /salir.
 */
public class ClienteHandler extends Thread {

    private final Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private String nombre;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

@Override
public void run() {
    try {
        entrada = new DataInputStream(socket.getInputStream());
        salida = new DataOutputStream(socket.getOutputStream());

        enviar("===== Sistema de Autenticación Flip7 =====");
        enviar("1) Iniciar sesión");
        enviar("2) Registrarse");
        enviar("Selecciona una opción (1 o 2):");

        int opcion = Integer.parseInt(entrada.readUTF());

        while (true) {
            enviar("Nombre de usuario:");
            String nombreInput = entrada.readUTF();

            enviar("Contraseña:");
            String pass = entrada.readUTF();

            if (opcion == 1) { // login
                if (ServidorFlip7.getDB().validarLogin(nombreInput, pass)) {
                    nombre = nombreInput;
                    enviar("[sistema] Inicio de sesión exitoso. Bienvenido " + nombre + "!");
                    break;
                } else {
                    enviar("[error] Usuario o contraseña incorrectos. Intenta otra vez.");
                }

            } else if (opcion == 2) { // registro
                if (ServidorFlip7.getDB().registrarUsuario(nombreInput, pass)) {
                    nombre = nombreInput;
                    enviar("[sistema] Registro exitoso. Bienvenido " + nombre + "!");
                    break;
                } else {
                    enviar("[error] Ese usuario ya existe. Intenta con otro.");
                }
            }
        }

        System.out.println("Jugador conectado como: " + nombre);

        ServidorFlip7.broadcast("[sistema] " + nombre + " se ha unido al servidor.", this);
        enviar("[sistema] Ya estás autenticado. Usa /salir para desconectarte.");

        String mensaje;

        while (true) {
            mensaje = entrada.readUTF();

            if (mensaje.equalsIgnoreCase("/salir")) {
                enviar("[sistema] Desconectando...");
                break;
            }

            ServidorFlip7.broadcast(nombre + ": " + mensaje, this);
        }

    } catch (Exception e) {
        System.err.println("Error con cliente " + nombre + ": " + e.getMessage());
    } finally {
        cerrar();
    }
}


    public void enviar(String mensaje) {
        try {
            if (salida != null) {
                salida.writeUTF(mensaje);
            }
        } catch (IOException e) {
            System.err.println("No se pudo enviar mensaje a " + nombre + ": " + e.getMessage());
        }
    }

    private void cerrar() {
        try {
            if (nombre != null) {
                System.out.println("Cliente desconectado: " + nombre);
                ServidorFlip7.broadcast("[sistema] " + nombre + " se ha desconectado.", this);
            }

            ServidorFlip7.removerCliente(this);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.err.println("Error al cerrar conexión de " + nombre + ": " + e.getMessage());
        }
    }
}

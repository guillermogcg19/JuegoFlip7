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

            // Pedimos nombre simple (después lo conectamos a la BD)
            enviar("Bienvenido al servidor Flip7.");
            enviar("Escribe tu nombre de jugador:");

            nombre = entrada.readUTF();
            System.out.println("Jugador conectado como: " + nombre);

            ServidorFlip7.broadcast("[sistema] " + nombre + " se ha unido al servidor.", this);
            enviar("[sistema] Escribe mensajes para el chat. Usa /salir para desconectarte.");

            String mensaje;

            while (true) {
                mensaje = entrada.readUTF();

                if (mensaje.equalsIgnoreCase("/salir")) {
                    enviar("[sistema] Desconectando...");
                    break;
                }

                // Por ahora solo reenviamos al resto (chat general)
                String salidaChat = nombre + ": " + mensaje;
                System.out.println("Mensaje recibido de " + nombre + ": " + mensaje);
                ServidorFlip7.broadcast(salidaChat, this);
            }

        } catch (IOException e) {
            System.err.println("Error con el cliente " + nombre + ": " + e.getMessage());
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

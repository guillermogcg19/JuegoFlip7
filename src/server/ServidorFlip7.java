package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor base Flip7:
 * - Escucha en un puerto fijo.
 * - Acepta múltiples clientes.
 * - Crea un hilo (ClienteHandler) por cada cliente.
 */
public class ServidorFlip7 {

    public static final int PUERTO = 5000;

    // Lista de clientes conectados (para broadcast más adelante)
    private static final CopyOnWriteArrayList<ClienteHandler> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Servidor Flip7 escuchando en el puerto " + PUERTO + "...");

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress());

                ClienteHandler handler = new ClienteHandler(socket);
                clientes.add(handler);
                handler.start();
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enviar un mensaje a todos los clientes excepto al origen.
     */
    public static void broadcast(String mensaje, ClienteHandler origen) {
        for (ClienteHandler c : clientes) {
            if (c != origen) {
                c.enviar(mensaje);
            }
        }
    }

    /**
     * Eliminar al cliente de la lista cuando se desconecta.
     */
    public static void removerCliente(ClienteHandler handler) {
        clientes.remove(handler);
    }
}

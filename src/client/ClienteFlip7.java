package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente por consola:
 * - Se conecta al servidor.
 * - Lee mensajes del servidor en un hilo.
 * - Envía lo que el usuario escribe en consola.
 */
public class ClienteFlip7 {

    private static final String HOST = "localhost"; // Limitación: solo localhost
    private static final int PUERTO = 5000;

    public static void main(String[] args) {

        try (Socket socket = new Socket(HOST, PUERTO)) {
            System.out.println("Conectado al servidor Flip7 en " + HOST + ":" + PUERTO);

            DataInputStream entrada = new DataInputStream(socket.getInputStream());
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);

            // Hilo que está escuchando siempre al servidor
            Thread lectorServidor = new Thread(() -> {
                try {
                    while (true) {
                        String recibido = entrada.readUTF();
                        System.out.println(recibido);
                    }
                } catch (IOException e) {
                    System.out.println("[sistema] Conexión cerrada por el servidor.");
                }
            });

            lectorServidor.setDaemon(true);
            lectorServidor.start();

            // Hilo principal: leer del teclado y mandar al servidor
            while (true) {
                String linea = scanner.nextLine();
                salida.writeUTF(linea);

                if (linea.equalsIgnoreCase("/salir")) {
                    System.out.println("[sistema] Saliendo del servidor...");
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}

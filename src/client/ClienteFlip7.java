package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteFlip7 {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {

        try (Socket socket = new Socket(HOST, PUERTO);
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado al servidor Flip7 en " + HOST + ":" + PUERTO);

            // Hilo para recibir mensajes del servidor
            Thread receptor = new Thread(() -> {
                try {
                    while (true) {
                        String mensaje = entrada.readUTF();
                        System.out.println(mensaje);
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println("\n[Desconexion del servidor]");
                }
            });

            receptor.start();

            // Hilo para enviar lo que escriba el usuario
            while (true) {
                System.out.print("> ");
                String texto = scanner.nextLine();

                if (texto.trim().isEmpty()) continue;

                salida.writeUTF(texto);
                salida.flush();
            }

        } catch (IOException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }
}

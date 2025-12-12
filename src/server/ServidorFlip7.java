package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServidorFlip7 {

    public static final int PUERTO = 5000;
    private static final CopyOnWriteArrayList<ClienteHandler> clientes = new CopyOnWriteArrayList<>();
    private static Database db = new Database();

    public static Database getDB() {
        return db;
    }

    public static void removerCliente(ClienteHandler c) {
        clientes.remove(c);
    }

    public static boolean usuarioYaConectado(String nombre) {
        for (ClienteHandler c : clientes) {
            if (c.getNombre() != null && c.getNombre().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Flip7 escuchando en puerto " + PUERTO);

            while (true) {
                Socket socket = servidor.accept();
                ClienteHandler ch = new ClienteHandler(socket);
                clientes.add(ch);
                ch.start();
            }

        } catch (Exception e) {
            System.out.println("Error en servidor: " + e.getMessage());
        }
    }
}

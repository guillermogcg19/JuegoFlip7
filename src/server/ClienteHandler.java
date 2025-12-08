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
private Sala SalaActual;
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
    mensaje = entrada.readUTF().trim();

    // ----------------- COMANDOS -----------------
    if (mensaje.equalsIgnoreCase("/salir")) {
        enviar("[sistema] Saliendo del servidor...");
        break;
    }

    if (mensaje.equalsIgnoreCase("/lista")) {
        enviar(SalaActual.resumenCompleto());
        continue;
    }

    if (mensaje.startsWith("/renombrar ")) {
        String nuevoNombre = mensaje.replace("/renombrar ", "").trim();
        SalaActual.broadcast("[sistema] " + nombre + " ahora es " + nuevoNombre);
        nombre = nuevoNombre;
        continue;
    }

    if (mensaje.equalsIgnoreCase("/ayuda")) {
        enviar("""
===== Bienvenido al chat =====
Comandos disponibles:
/ayuda   Ver comandos
/lista   Ver jugadores/espectadores
/renombrar NUEVO_NOMBRE
/salir
""");
        continue;
    }

    // ----------------- CHAT NORMAL -----------------
    SalaActual.broadcast(nombre + ": " + mensaje);
}
        

    } catch (Exception e) {
        System.err.println("Error con cliente " + nombre + ": " + e.getMessage());
    } finally {
        cerrar();
    }

 enviar("=== Selección de sala ===");
enviar("1) Ver salas");
enviar("2) Unirse o crear una sala");
enviar("Escribe 1 o 2:");

int opcionSala = 0;

try {
    opcionSala = Integer.parseInt(entrada.readUTF());
} catch (Exception e) {
    enviar("[error] Entrada inválida. Se asignará opción 2 por defecto.");
    opcionSala = 2;
}

if (opcionSala == 1) {
    enviar(GestorSalas.listarSalas());
}

enviar("Escribe el nombre de la sala a la que deseas entrar:");
String nombreSala;

try {
    nombreSala = entrada.readUTF();
} catch (Exception e) {
    enviar("[error] No se pudo leer el nombre de la sala. Se usará 'SalaDefault'.");
    nombreSala = "SalaDefault";
}

// Obtener sala (si no existe, se crea)
SalaActual = GestorSalas.obtenerOScrear(nombreSala);

// Intentar entrar como jugador
if (SalaActual.agregarJugador(this)) {
    enviar("Te uniste como JUGADOR a la sala: " + nombreSala);
    SalaActual.broadcast("[sistema] " + nombre + " se unió como jugador.");
} else {
    // Si ya hay 6, entra como espectador
    SalaActual.agregarEspectador(this);
    enviar("La sala está llena. Entraste como ESPECTADOR en: " + nombreSala);
    SalaActual.broadcast("[sistema] " + nombre + " se unió como espectador.");
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
if (SalaActual != null ){
SalaActual.removerCliente(this);
SalaActual.broadcast("[sistema] " + nombre + " salió de la sala.");
}
            if (socket != null && !socket.isClosed()) {
                
                socket.close();
            }

        } catch (IOException e) {
            System.err.println("Error al cerrar conexión de " + nombre + ": " + e.getMessage());
        }
    }

    public String getNombre() {
        return nombre;
    }
}

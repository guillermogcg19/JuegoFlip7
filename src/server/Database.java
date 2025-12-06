package server;

import java.sql.*;

public class Database {

    private static final String URL = "jdbc:sqlite:flip7.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.out.println("No se pudo cargar SQLite: " + e.getMessage());
        }
    }

    public Database() {
        crearTablaUsuarios();
    }

    private void crearTablaUsuarios() {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            );
        """;

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.execute();
            System.out.println("[DB] Tabla usuarios lista.");

        } catch (SQLException e) {
            System.err.println("[DB] Error creando tabla: " + e.getMessage());
        }
    }

    public boolean registrarUsuario(String nombre, String password) {
        String sql = "INSERT INTO usuarios(nombre, password) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre);
            stmt.setString(2, password);
            stmt.execute();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    public boolean validarLogin(String nombre, String password) {
        String sql = "SELECT * FROM usuarios WHERE nombre = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            return false;
        }
    }
}

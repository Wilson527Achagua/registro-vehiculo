package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    private static final String URL = "jdbc:postgresql://localhost:5432/molinos_llano_arroz";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1006567942";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Error en conexión: " + e.getMessage());
            return null;
        }
    }
}


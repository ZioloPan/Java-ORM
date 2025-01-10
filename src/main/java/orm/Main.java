package orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            ConnectionPool connectionPool = new ConnectionPool();

            // Uzyskanie połączenia z puli
            Connection connection = connectionPool.getConnection();

            try {
                // Przykładowe zapytanie
                String query = "SELECT 1";
                try (PreparedStatement statement = connection.prepareStatement(query);
                     ResultSet resultSet = statement.executeQuery()) {

                    while (resultSet.next()) {
                        System.out.println("Wynik zapytania: " + resultSet.getInt(1));
                    }
                }
            } finally {
                // Zwrócenie połączenia do puli
                connectionPool.releaseConnection(connection);
            }

            // Zamknięcie puli połączeń
            connectionPool.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

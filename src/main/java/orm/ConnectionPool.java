package orm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {

    private final BlockingQueue<Connection> connections; // Pula połączeń

    public ConnectionPool() throws SQLException {

        Config config = Config.getInstance();
        int size = config.getPoolSize();

        // Tworzymy kolejkę o ustalonym rozmiarze
        connections = new ArrayBlockingQueue<>(size);

        // Tworzymy połączenia i dodajemy je do kolejki
        for (int i = 0; i < size; i++) {
            connections.add(DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword()));
        }
    }

    // Uzyskanie połączenia (blokuje, jeśli nie ma dostępnych połączeń)
    public Connection getConnection() throws InterruptedException {
        return connections.take(); // Czeka, aż połączenie stanie się dostępne
    }

    // Zwrócenie połączenia do puli
    public void releaseConnection(Connection connection) {
        try {
            connections.put(connection); // Dodaje połączenie z powrotem do kolejki
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Błąd podczas zwalniania połączenia: " + e.getMessage());
        }
    }

    // Zamknięcie wszystkich połączeń w puli
    public void close() {
        while (!connections.isEmpty()) {
            try {
                connections.poll().close(); // Pobiera i zamyka każde połączenie
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

package orm;

import orm.logging.Observer;
import orm.iterator.CustomList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {

    private static ConnectionPool instance;
    private final BlockingQueue<Connection> connections;
    private final CustomList<Observer> observers = new CustomList<>();


    private ConnectionPool() throws SQLException {

        Config config = Config.getInstance();
        int size = config.getPoolSize();

        connections = new ArrayBlockingQueue<>(size);

        for (int i = 0; i < size; i++) {
            connections.add(DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword()));
        }
    }

    public static synchronized ConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    public Connection getConnection() throws InterruptedException {
        return connections.take();
    }

    public void releaseConnection(Connection connection) {
        try {
            connections.put(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Błąd podczas zwalniania połączenia: " + e.getMessage());
        }
    }

    public void close() {
        while (!connections.isEmpty()) {
            try {
                connections.poll().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers(String message) {
        for (int i = 0; i < observers.size(); i++) {
            observers.get(i).notify(message);
        }
    }
}

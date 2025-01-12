import org.junit.jupiter.api.*;
import orm.*;
import orm.logging.*;
import orm.models.*;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ObserverTest {

    @Test
    void observerTest() {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Runnable task1 = () -> {
            try {
                ConnectionPool connectionPool = ConnectionPool.getInstance();
                EntityManager entityManager = new EntityManager(connectionPool);

                LoggerObserver loggerObserver = new LoggerObserver();
                connectionPool.addObserver(loggerObserver);

                Employee e1 = new Employee();
                e1.setName("Gabi");
                e1.setId(20);
                entityManager.save(e1);

                Car c1 = new Car();
                c1.setModel("bmw");
                c1.setId(21);
                entityManager.save(c1);

                c1.addEmployee(e1);
                entityManager.update(c1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runnable task2 = () -> {
            try {
                ConnectionPool connectionPool = ConnectionPool.getInstance();
                LoggerObserver loggerObserver = new LoggerObserver();
                connectionPool.addObserver(loggerObserver);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            while (true);
        };

        executorService.execute(task2);
        executorService.execute(task1);

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Czas oczekiwania minął. Próbujemy wymusić zamknięcie wątków...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Oczekiwanie na zakończenie wątków zostało przerwane.");
            executorService.shutdownNow();
        }
        System.out.println("ExecutorService zakończony.");
    }

}

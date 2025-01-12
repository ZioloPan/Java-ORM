package orm;

import orm.logging.LoggerObserver;
import orm.models.Ocean;
import orm.models.Species;

public class Main {
    public static void main(String[] args) {
        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            entityManager.addObserver(loggerObserver);

            Ocean ocean = new Ocean();
            ocean.setName("Atlantic Ocean");

            Species species = new Species();
            species.setName("Shark");
            species.setOcean(ocean);

            System.out.println("Zapisywanie gatunku i oceanu...");
            entityManager.save(ocean);
            Thread.sleep(10000);
            entityManager.save(species);

            System.out.println("Dane zostały zapisane w bazie!");

            connectionPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}






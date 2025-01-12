package orm;

public class Main {
    public static void main(String[] args) {
        try {
            ConnectionPool connectionPool = new ConnectionPool();
            EntityManager entityManager = new EntityManager(connectionPool);

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






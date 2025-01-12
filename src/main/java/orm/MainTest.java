package orm;

import orm.logging.LoggerObserver;
import orm.models.*;

import java.sql.SQLException;

public class MainTest {
    public static void main(String[] args) {
        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

            Department d1 = new Department();
            d1.setId(10);
            d1.setName("Pakowanie");
            Department d2 = new Department();
            d2.setId(11);
            d2.setName("Malowanie");

            Employee e1 = new Employee();
            e1.setName("Gabi");
            e1.setId(10);
            Employee e2 = new Employee();
            e2.setName("Bartek");
            e2.setId(11);
            Employee e3 = new Employee();
            e3.setName("Ala");
            e3.setId(12);

            entityManager.save(d1);
            entityManager.save(d2);

            d1.addEmployee(e1);
            d2.addEmployee(e2);
            d1.addEmployee(e3);

            entityManager.save(e1);
            entityManager.save(e2);
            entityManager.save(e3);

            entityManager.delete(d1);
            entityManager.delete(e2);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
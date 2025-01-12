import org.junit.jupiter.api.*;
import orm.*;
import orm.logging.LoggerObserver;
import orm.models.Car;
import orm.models.Department;
import orm.models.Employee;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RelationsTest {

    @Test
    @Order(1)
    void oneToOneTest() {
        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

            Car c1 = new Car();
            c1.setModel("bmw");
            c1.setId(1);
            Car c2 = new Car();
            c2.setModel("audi");
            c2.setId(2);

            Employee e1 = new Employee();
            e1.setName("Gabi");
            e1.setId(1);
            Employee e2 = new Employee();
            e2.setName("Bartek");
            e2.setId(2);
            Employee e3 = new Employee();
            e3.setName("Ala");
            e3.setId(3);

            entityManager.save(e1);
            entityManager.save(e2);
            entityManager.save(e3);

            c1.addEmployee(e3);
            c2.addEmployee(e1);

            entityManager.save(c1);
            entityManager.save(c2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(2)
    void oneToManyTest() {
        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

//            Department d1 = new Department();
//            d1.setId(1);
//            d1.setName("Pakowanie");
//            Department d2 = new Department();
//            d2.setId(2);
//            d2.setName("Malowanie");

//            Employee e1 = new Employee();
//            e1.setName("Gabi");
//            e1.setId(4);
//            Employee e2 = new Employee();
//            e2.setName("Bartek");
//            e2.setId(5);
//            Employee e3 = new Employee();
//            e3.setName("Ala");
//            e3.setId(6);
            Employee e1 = entityManager.find(Employee.class, 1);
            System.out.println(e1);
            Employee e2 = entityManager.find(Employee.class, 2);
            Employee e3 = entityManager.find(Employee.class, 3);


//            entityManager.save(d1);
//            entityManager.save(d2);
//
//            d1.addEmployee(e1);
//            d2.addEmployee(e2);
//            d1.addEmployee(e3);
//
////            System.out.println(d1);
////            System.out.println(d2);
//
//            entityManager.save(e1);
//            entityManager.save(e2);
//            entityManager.save(e3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

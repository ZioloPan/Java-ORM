package orm;

import orm.logging.LoggerObserver;
import orm.models.Employee;
import orm.models.Car;

public class MainTest {
    public static void main(String[] args) {

////        relacja 1-* test
//        try {
//            ConnectionPool connectionPool = ConnectionPool.getInstance();
//            EntityManager entityManager = new EntityManager(connectionPool);
//
//            LoggerObserver loggerObserver = new LoggerObserver();
//            connectionPool.addObserver(loggerObserver);
//
//            System.out.println("Pobieranie działu o ID 1...");
//            Department department = entityManager.find(Department.class, 1);
//
//            if (department != null) {
//                System.out.println("Dział: " + department.getName());
//                System.out.println("Pracownicy:");
//                for (Employee employee : department.getEmployees()) {
//                    System.out.println(" - " + employee.getName());
//                }
//            }
//
//            connectionPool.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }



        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

            Car c1 = new Car();
            c1.setModel("bmw");
            Car c2 = new Car();
            c1.setModel("audi");

            Employee e1 = new Employee();
            e1.setName("Gabi");
            Employee e2 = new Employee();
            e1.setName("Bartek");
            Employee e3 = new Employee();
            e1.setName("Ala");

            c1.addEmployee(e1);
            c2.addEmployee(e2);

            //entityManager.save(e1);
//            entityManager.save(e2);
//            entityManager.save(e3);
            entityManager.save(c1);
//            entityManager.save(c2);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}






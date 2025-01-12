package orm;

import orm.logging.LoggerObserver;
import orm.models.Department;
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

            Department d1 = new Department();
            d1.setId(1);
            d1.setName("Pakowanie");
            Department d2 = new Department();
            d2.setId(2);
            d2.setName("Malowanie");

            Employee e1 = new Employee();
            e1.setName("Gabi");
            e1.setId(4);
            Employee e2 = new Employee();
            e2.setName("Bartek");
            e2.setId(5);
            Employee e3 = new Employee();
            e3.setName("Ala");
            e3.setId(6);

//            entityManager.save(d1);
//            entityManager.save(d2);

            d1.addEmployee(e1);
            d2.addEmployee(e2);
            d1.addEmployee(e3);

            System.out.println(d1);
            System.out.println(d2);

            entityManager.save(e1);
            entityManager.save(e2);
            entityManager.save(e3);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}






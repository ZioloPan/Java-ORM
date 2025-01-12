package orm;

import orm.logging.LoggerObserver;
import orm.models.Department;
import orm.models.Employee;

public class MainTest {
    public static void main(String[] args) {

//        relacja 1-* test
        try {
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

            System.out.println("Pobieranie działu o ID 1...");
            Department department = entityManager.find(Department.class, 1);

            if (department != null) {
                System.out.println("Dział: " + department.getName());
                System.out.println("Pracownicy:");
                for (Employee employee : department.getEmployees()) {
                    System.out.println(" - " + employee.getName());
                }
            }

            connectionPool.close();

        } catch (Exception e) {
            e.printStackTrace();
        }







    }
}






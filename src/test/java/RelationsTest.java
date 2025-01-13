import org.junit.jupiter.api.*;
import orm.*;
import orm.logging.LoggerObserver;
import orm.models.Car;
import orm.models.Department;
import orm.models.Employee;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RelationsTest {

    @Test
    @Order(1)
    void oneToOneTest() {
        try {
            EntityManager entityManager = new EntityManager(new LoggerObserver());

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
            EntityManager entityManager = new EntityManager(new LoggerObserver());

            Department d1 = new Department();
            d1.setId(1);
            d1.setName("Pakowanie");
            Department d2 = new Department();
            d2.setId(2);
            d2.setName("Malowanie");

            entityManager.save(d1);
            entityManager.save(d2);

            Employee e1 = new Employee();
            e1.setName("Gabi");
            e1.setId(4);
            Employee e2 = new Employee();
            e2.setName("Bartek");
            e2.setId(5);
            Employee e3 = new Employee();
            e3.setName("Ala");
            e3.setId(6);

            d1.addEmployee(e1);
            d2.addEmployee(e2);
            d1.addEmployee(e3);

            entityManager.save(e1);
            entityManager.save(e2);
            entityManager.save(e3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(3)
    void updateManyToOne() {
        try {
            EntityManager entityManager = new EntityManager(new LoggerObserver());

            Employee e = entityManager.find(Employee.class, 4);
            Employee e1 = entityManager.find(Employee.class, 1);
            Employee e2 = entityManager.find(Employee.class, 2);
            Employee e3 = entityManager.find(Employee.class, 3);

            Department d1 = entityManager.find(Department.class, 2);
            Department d2 = entityManager.find(Department.class, 1);
            System.out.println(d1.getEmployees());

            e.setDepartment(d1);
            e1.setDepartment(d2);
            e2.setDepartment(d2);
            e3.setDepartment(d1);

            entityManager.update(e);
            entityManager.update(e1);
            entityManager.update(e2);
            entityManager.update(e3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(4)
    void updateOneToOne() {
        try {
            EntityManager entityManager = new EntityManager(new LoggerObserver());

            Employee e1 = entityManager.find(Employee.class, 4);
            Car c1 = entityManager.find(Car.class, 2);
            c1.setEmployee(e1);
            entityManager.update(c1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(5)
    void findRelatedFields() {
        try {
            EntityManager entityManager = new EntityManager(new LoggerObserver());

            Employee e = entityManager.find(Employee.class, 4);
            System.out.println("Got OneToOne:");
            System.out.println(e.getCar());

            Department d = entityManager.find(Department.class, 1);
            System.out.println("\nGot OneToMany:");
            System.out.println(d.getEmployees());

            System.out.println("\nGot ManyToOne:");
            System.out.println(e.getDepartment());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(6)
    void customQuery() {
        try {
            EntityManager entityManager = new EntityManager(new LoggerObserver());

            List<Employee> entities = entityManager.executeQuery(
                    "SELECT * FROM employees WHERE name = ? AND department_id = ?",
                    Employee.class,
                    "Ala", 1
            );

            for (Employee entity : entities) {
                System.out.println(entity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

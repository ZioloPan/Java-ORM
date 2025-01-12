package orm;

import orm.logging.LoggerObserver;
import orm.models.*;

public class MainTest {
    public static void main(String[] args) {
        try {
            // Tworzenie puli połączeń i EntityManagera
            ConnectionPool connectionPool = ConnectionPool.getInstance();
            EntityManager entityManager = new EntityManager(connectionPool);

            // Dodanie obserwatora logów
            LoggerObserver loggerObserver = new LoggerObserver();
            connectionPool.addObserver(loggerObserver);

            // Tworzenie studentów
            Student s1 = new Student();
            Student s2 = new Student();
            Student s3 = new Student();
            s1.setName("Gabi");
            s2.setName("Bartek");
            s3.setName("Ala");
            s1.setId(1); // Ręczne ustawienie ID
            s2.setId(2);
            s3.setId(3);

            // Tworzenie projektów
            Project p1 = new Project();
            Project p2 = new Project();
            Project p3 = new Project();
            Project p4 = new Project();
            Project p5 = new Project();
            p1.setName("Bookit");
            p2.setName("Prosze");
            p3.setName("Dzialaj");
            p4.setName("Design");
            p5.setName("Patterns");
            p1.setId(1); // Ręczne ustawienie ID
            p2.setId(2);
            p3.setId(3);
            p4.setId(4);
            p5.setId(5);

            // Ustawianie relacji ManyToMany (student ↔ projekty)
            s1.addProjects(p1); // Gabi pracuje nad Bookit
            s2.addProjects(p2); // Bartek pracuje nad Prosze
            s3.addProjects(p1); // Ala pracuje nad Bookit
            s1.addProjects(p2); // Gabi pracuje nad Prosze
            s2.addProjects(p3); // Bartek pracuje nad Dzialaj

            // Zapisywanie encji w bazie danych
            entityManager.save(p1);
            entityManager.save(p2);
            entityManager.save(p3);
            entityManager.save(p4);
            entityManager.save(p5);

            entityManager.save(s1);
            entityManager.save(s2);
            entityManager.save(s3);

            // Pobieranie i sprawdzanie danych z bazy
            System.out.println("Pobieranie studenta o ID 1...");
            Student fetchedStudent = entityManager.find(Student.class, 1);
            if (fetchedStudent != null) {
                System.out.println("Student: " + fetchedStudent.getName());
                System.out.println("Projekty studenta:");
                for (Project project : fetchedStudent.getProjects()) {
                    System.out.println(" - " + project.getName());
                }
            }

            // Zamknięcie puli połączeń
            connectionPool.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

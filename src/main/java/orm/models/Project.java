package orm.models;

import orm.annotations.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "projects")
public class Project {
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToMany(
            isMapped = true,
            joinTable = "students_projects",
            joinColumn = "project_id",
            inverseJoinColumn = "student_id"
    )
    private List<Student> students = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Student> getStudents() {
        return students;
    }

    @Override
    public String toString() {
        return name;
    }
}

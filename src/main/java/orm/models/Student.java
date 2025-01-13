package orm.models;

import orm.annotations.*;
import java.util.ArrayList;

@Table(name = "students")
public class Student {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToMany(
            isMapped = false,
            joinTable = "students_projects",
            joinColumn = "student_id",
            inverseJoinColumn = "project_id"
    )
    private ArrayList<Project> projects = new ArrayList<>();

    public void addProjects(Project p) {
        projects.add(p);
        p.getStudents().add(this);
    }

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

    public ArrayList<Project> getProjects() {
        return projects;
    }

    public void setProjects(ArrayList<Project> projects) {
        this.projects = projects;
    }

    @Override
    public String toString() {
        return name;
    }
}

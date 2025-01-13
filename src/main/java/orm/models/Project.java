package orm.models;

import orm.annotations.*;

import orm.iterator.CustomList;


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
    private CustomList<Student> students = new CustomList<>();

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

    public CustomList<Student> getStudents() {
        return students;
    }

    @Override
    public String toString() {
        return name;
    }
}

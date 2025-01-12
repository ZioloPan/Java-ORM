package orm.models;

import orm.annotations.Column;
import orm.annotations.Id;
import orm.annotations.ManyToOne;
import orm.annotations.Table;

@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToOne(column = "department_id")
    private Department department;

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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}

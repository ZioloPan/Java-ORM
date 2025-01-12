package orm.models;

import orm.annotations.Column;
import orm.annotations.Id;
import orm.annotations.OneToMany;
import orm.annotations.Table;
import java.util.List;

@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "department")
    private List<Employee> employees;

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

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
}



package orm.models;

import orm.annotations.Column;
import orm.annotations.Id;
import orm.annotations.OneToMany;
import orm.annotations.Table;

import orm.iterator.CustomList;


@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "department_id")
    private CustomList<Employee> employees = new CustomList<>();

    public void addEmployee(Employee e) {
        employees.add(e);
        e.setDepartment(this);
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

    public CustomList<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(CustomList<Employee> employees) {
        this.employees = employees;
    }

    @Override
    public String toString() {
        return "Department(id: " + id + ", name: " + name + ")";
    }
}



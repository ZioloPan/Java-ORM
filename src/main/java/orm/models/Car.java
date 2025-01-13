package orm.models;

import orm.annotations.*;

@Table(name = "cars")
public class Car {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "model")
    private String model;

    @OneToOne(column = "employee_id", foreignKeyInThisTable = true)
    private Employee employee;

    public void addEmployee(Employee e) {
        this.employee = e;
        e.setCar(this);
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @Override
    public String toString() {
        return "Car(id: " + id + ", model: " + model + ", employee: " + (employee != null ? employee.getName() : null) + ")";
    }
}

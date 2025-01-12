package orm.models;

import orm.annotations.*;

@Table(name = "cars")
public class Car {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "model")
    private String model;

    @OneToOne(column = "employee_id")
    private Employee employee;

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
}

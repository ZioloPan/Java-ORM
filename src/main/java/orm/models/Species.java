package orm.models;

import orm.annotations.*;

@Table(name = "species")
public class Species {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @OneToOne(column = "ocean_id")
    private Ocean ocean;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Ocean getOcean() {
        return ocean;
    }

    public void setOcean(Ocean ocean) {
        this.ocean = ocean;
    }
}

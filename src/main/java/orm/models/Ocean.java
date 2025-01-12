package orm.models;

import orm.annotations.*;

@Table(name = "oceans")
public class Ocean {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @OneToOne(column = "id")
    private Species species;

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

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }
}

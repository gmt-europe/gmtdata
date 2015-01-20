package nl.gmt.data.test.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;

@Entity
@Table(name = "Address")
public class Address extends nl.gmt.data.Entity {
    public Address() {
    }

    public Address(String street, Integer houseNumber, String city, Relation relation) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.relation = relation;
    }

    private java.util.UUID id;

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "Id", nullable = false)
    @Override
    public java.util.UUID getId() {
        return this.id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    private String street;

    @Column(name = "Street", nullable = false, length = 200)
    public String getStreet() {
        return this.street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    private Integer houseNumber;

    @Column(name = "HouseNumber", nullable = false)
    public Integer getHouseNumber() {
        return this.houseNumber;
    }

    public void setHouseNumber(Integer houseNumber) {
        this.houseNumber = houseNumber;
    }

    private String city;

    @Column(name = "City", nullable = false, length = 200)
    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    private Relation relation;

    @ManyToOne
    @JoinColumn(name = "RelationId")
    public Relation getRelation() {
        return this.relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }
}

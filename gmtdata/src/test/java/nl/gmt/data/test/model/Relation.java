package nl.gmt.data.test.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.Set;

@Entity
@Table(name = "`Relation`")
public class Relation extends nl.gmt.data.Entity {
    public Relation() {
    }

    public Relation(String name, Gender gender, byte[] picture) {
        this.name = name;
        this.gender = gender;
        this.picture = picture;
    }

    private java.util.UUID id;

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "`Id`", nullable = false)
    @Override
    public java.util.UUID getId() {
        return this.id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    private byte[] picture;

    @Column(name = "`Picture`")
    public byte[] getPicture() {
        return this.picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    private String name;

    @Column(name = "`Name`", nullable = false, length = 200)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Gender gender;

    @Column(name = "`Gender`", nullable = false)
    @Type(type = "nl.gmt.data.test.model.Gender$UserType")
    public Gender getGender() {
        return this.gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    private Set<Address> addresses;

    @OneToMany(mappedBy = "relation")
    public Set<Address> getAddresses() {
        return this.addresses;
    }

    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }
}

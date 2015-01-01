package nl.gmt.data;

import org.hibernate.Hibernate;

public abstract class Entity {
    public abstract Object getId();

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Entity) || Hibernate.getClass(this) != Hibernate.getClass(obj)) {
            return false;
        }

        Entity other = (Entity)obj;

        return getId() != null && other.getId() != null && getId().equals(other.getId());
    }

    public int hashCode() {
        if (getId() == null) {
            return 0;
        }
        return getId().hashCode();
    }
}

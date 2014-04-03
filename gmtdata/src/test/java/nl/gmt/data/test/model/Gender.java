package nl.gmt.data.test.model;

import nl.gmt.data.hibernate.PersistentEnum;
import nl.gmt.data.hibernate.PersistentEnumUserType;

public enum Gender implements PersistentEnum {
    FEMALE(2),
    MALE(1);

    private final int value;

    private Gender(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static class UserType extends PersistentEnumUserType<Gender> {
        @Override
        public Class<Gender> returnedClass() {
            return Gender.class;
        }
    }
}

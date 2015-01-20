/*
 *  Copyright 2001-2011 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package nl.gmt.data.contrib.joda;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.EnhancedUserType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.DateTime} via hibernate.
 *
 * Changes to support Hibernate 4 by Pieter van Ginkel (pginkel@gmt.nl).
 *
 * @author Mario Ivankovits (mario@ops.co.at)
 * @author Pieter van Ginkel (pginkel@gmt.nl)
 */
public class PersistentDateTime implements EnhancedUserType, Serializable {

    public static final PersistentDateTime INSTANCE = new PersistentDateTime();

    private static final int[] SQL_TYPES = new int[]{Types.TIMESTAMP,};

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Class returnedClass() {
        return DateTime.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        DateTime dtx = (DateTime)x;
        DateTime dty = (DateTime)y;

        return dtx.equals(dty);
    }

    @Override
    public int hashCode(Object object) throws HibernateException {
        return object.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] strings, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return nullSafeGet(resultSet, strings[0], session, owner);

    }

    public Object nullSafeGet(ResultSet resultSet, String string, SessionImplementor session, Object owner) throws SQLException {
        Object timestamp = StandardBasicTypes.TIMESTAMP.nullSafeGet(resultSet, string, session, owner);
        if (timestamp == null) {
            return null;
        }

        return new DateTime(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            StandardBasicTypes.TIMESTAMP.nullSafeSet(preparedStatement, null, index, session);
        } else {
            StandardBasicTypes.TIMESTAMP.nullSafeSet(preparedStatement, ((DateTime)value).toDate(), index, session);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    @Override
    public Object assemble(Serializable cached, Object value) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public String objectToSQLString(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public String toXMLString(Object object) {
        return object.toString();
    }

    @Override
    @Deprecated
    public Object fromXMLString(String string) {
        return new DateTime(string);
    }

}

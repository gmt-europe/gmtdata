/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package nl.gmt.data.contrib.postgres;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * A {@link UserType} that persists objects as JSONB.
 * <p>
 * Unlike the default JPA object mapping, {@code JSONBUserType} can also be used
 * for properties that do not implement {@link Serializable}.
 */
public abstract class PGobjectUserType extends AbstractUserType {
    private final String postgresType;

    public PGobjectUserType(String postgresType) {
        this.postgresType = postgresType;
    }

    @Override
    public Class<Object> returnedClass() {
        return Object.class;
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.CLOB};
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return resultSet.getString(names[0]);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        PGobject pgo = new PGobject();
        pgo.setType(postgresType);
        pgo.setValue((String)value);
        st.setObject(index, pgo);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }
}

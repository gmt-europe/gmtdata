package nl.gmt.data;

import nl.gmt.data.test.TestConnection;
import nl.gmt.data.test.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class UniqueIndexFixture extends DbConnectionFixtureBase {
    @Test
    public void simpleUniqueIndexInsert() throws Exception {
        TestConnection db = openDb();

        // First create a relation that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            ctx.saveOrUpdate(new Relation("Relation1", Gender.MALE, null));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            boolean hadOne = false;

            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = new Relation();
            relation.setName("Relation1");
            relation.setGender(Gender.MALE);

            validator.validate(relation, EntityValidatorMode.CREATE);
            for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
                if ("name".equals(entry.getKey().getFieldName())) {
                    hadOne = true;
                    assertEquals("Must be unique", entry.getValue());
                }
            }

            assertTrue(hadOne);

            ctx.commit();
        }
    }

    @Test
    public void simpleUniqueIndexUpdate() throws Exception {
        TestConnection db = openDb();

        // First create a relation that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            ctx.saveOrUpdate(new Relation("Relation1", Gender.MALE, null));
            ctx.saveOrUpdate(new Relation("Relation2", Gender.MALE, null));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            boolean hadOne = false;

            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation2");
            relation.setName("Relation1");

            validator.validate(relation, EntityValidatorMode.UPDATE);
            for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
                if ("name".equals(entry.getKey().getFieldName())) {
                    hadOne = true;
                    assertEquals("Must be unique", entry.getValue());
                }
            }

            assertTrue(hadOne);

            // Evict the entity so that we can commit.

            ctx.evict(relation);

            ctx.commit();
        }
    }

    @Test
    public void simpleUniqueIndexUpdateNoChange() throws Exception {
        TestConnection db = openDb();

        // First create a relation that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            ctx.saveOrUpdate(new Relation("Relation1", Gender.MALE, null));
            ctx.saveOrUpdate(new Relation("Relation2", Gender.MALE, null));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation2");

            validator.validate(relation, EntityValidatorMode.UPDATE);
            assertEquals(0, validator.getMessages().size());

            ctx.commit();
        }
    }

    @Test
    public void ensureFlushAfterIndexCheck() throws Exception {
        // The purpose of this test is to verify that the changes are still committed after we temporarily
        // change the flush mode. We need to change the flush mode to manual because we need to perform queries
        // to test unique indexes. Not changing the flush mode would flush the entity before we do these queries.

        TestConnection db = openDb();

        // First create a relation that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            ctx.saveOrUpdate(new Relation("Relation1", Gender.MALE, null));
            ctx.saveOrUpdate(new Relation("Relation2", Gender.MALE, null));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation2");
            relation.setName("Relation3");

            validator.validate(relation, EntityValidatorMode.UPDATE);
            assertEquals(0, validator.getMessages().size());

            ctx.commit();
        }

        // And check that the name was actually changed.

        try (DbContext ctx = db.openContext()) {
            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation3");
            assertNotNull(relation);

            ctx.commit();
        }
    }

    @Test
    public void multiPropertyUniqueIndexInsert() throws Exception {
        TestConnection db = openDb();

        // First create an address that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            Relation relation = new Relation("Relation1", Gender.MALE, null);
            ctx.saveOrUpdate(relation);

            ctx.saveOrUpdate(new Address("Street1", 1, "City1", relation));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            boolean hadOne = false;

            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation1");

            Address address = new Address("Street1", 1, "City1", relation);

            validator.validate(address, EntityValidatorMode.CREATE);
            for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
                if ("street".equals(entry.getKey().getFieldName())) {
                    hadOne = true;
                    assertEquals("Must be unique", entry.getValue());
                }
            }

            assertTrue(hadOne);

            ctx.commit();
        }
    }

    @Test
    public void multiPropertyUniqueIndexUpdate() throws Exception {
        TestConnection db = openDb();

        // First create an address that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            Relation relation = new Relation("Relation1", Gender.MALE, null);
            ctx.saveOrUpdate(relation);

            ctx.saveOrUpdate(new Address("Street1", 1, "City1", relation));
            ctx.saveOrUpdate(new Address("Street2", 1, "City1", relation));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            boolean hadOne = false;

            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation1");
            Address address = ctx.getRepository(AddressRepository.class).findByStreetAndHouseNumberAndCityAndRelation("Street2", 1, "City1", relation);

            address.setStreet("Street1");

            validator.validate(address, EntityValidatorMode.UPDATE);
            for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
                if ("street".equals(entry.getKey().getFieldName())) {
                    hadOne = true;
                    assertEquals("Must be unique", entry.getValue());
                }
            }

            assertTrue(hadOne);

            // Evict the entity so that we can commit.

            ctx.evict(address);

            ctx.commit();
        }
    }

    @Test
    public void multiPropertyUniqueIndexUpdateNoChange() throws Exception {
        TestConnection db = openDb();

        // First create an address that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            Relation relation = new Relation("Relation1", Gender.MALE, null);
            ctx.saveOrUpdate(relation);

            ctx.saveOrUpdate(new Address("Street1", 1, "City1", relation));
            ctx.saveOrUpdate(new Address("Street2", 1, "City1", relation));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation1");
            Address address = ctx.getRepository(AddressRepository.class).findByStreetAndHouseNumberAndCityAndRelation("Street2", 1, "City1", relation);

            validator.validate(address, EntityValidatorMode.UPDATE);
            assertEquals(0, validator.getMessages().size());

            ctx.commit();
        }
    }

    @Test
    public void multiPropertyUniqueIndexInsertWithNull() throws Exception {
        TestConnection db = openDb();

        // First create an address that is going to conflict.

        try (DbContext ctx = db.openContext()) {
            Relation relation = new Relation("Relation1", Gender.MALE, null);
            ctx.saveOrUpdate(relation);

            ctx.saveOrUpdate(new Address("Street1", 1, "City1", relation));

            ctx.commit();
        }

        // Then, validate a new insert.

        try (DbContext ctx = db.openContext()) {
            EntityValidator validator = new EntityValidator(ctx);

            Relation relation = ctx.getRepository(RelationRepository.class).findByName("Relation1");

            Address address = new Address("Street1", null, "City1", relation);

            validator.validate(address, EntityValidatorMode.CREATE);
            assertEquals(0, validator.getMessages().size());

            ctx.saveOrUpdate(address);

            ctx.commit();
        }
    }
}

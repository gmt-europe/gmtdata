package nl.gmt.data;

import nl.gmt.data.test.TestConnection;
import nl.gmt.data.test.model.Address;
import nl.gmt.data.test.model.Gender;
import nl.gmt.data.test.model.Relation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class DbUsageFixture extends DbConnectionFixtureBase {
    @Test
    public void findNoUsage() throws Exception {
        try (TestConnection db = openDb()) {
            Relation relation = buildData(db, false);

            DbEntityUsage usage;

            try (DbContext ctx = db.openContext()) {
                usage = db.getUsage(ctx, relation);

                ctx.commit();
            }

            assertEquals(0, usage.getCount());
            assertEquals(0, usage.getRelations().size());
        }
    }

    @Test
    public void findNoUsageByExclusion() throws Exception {
        try (TestConnection db = openDb()) {
            Relation relation = buildData(db, true);

            DbEntityUsage usage;

            try (DbContext ctx = db.openContext()) {
                EntityType addressType = db.getEntitySchema().getEntityType("Address");

                usage = db.getUsage(ctx, relation, addressType);

                ctx.commit();
            }

            assertEquals(0, usage.getCount());
            assertEquals(0, usage.getRelations().size());
        }
    }

    @Test
    public void findUsage() throws Exception {
        try (TestConnection db = openDb()) {
            Relation relation = buildData(db, true);

            DbEntityUsage usage;

            try (DbContext ctx = db.openContext()) {
                usage = db.getUsage(ctx, relation);

                ctx.commit();
            }

            EntityType addressType = db.getEntitySchema().getEntityType("Address");

            assertEquals(3, usage.getCount());
            assertEquals(1, usage.getRelations().size());
            assertEquals(addressType, usage.getRelations().get(0).getType());
            assertEquals(3, usage.getRelations().get(0).getCount());
        }
    }

    private Relation buildData(TestConnection db, boolean createDependent) throws Exception {
        try (DbContext ctx = db.openContext()) {
            for (Relation relation : ctx.<Relation>createQuery("from Relation r")) {
                ctx.delete(relation);
            }

            ctx.commit();
        }

        Relation relation;

        try (DbContext ctx = db.openContext()) {
            relation = new Relation(
                "Pieter van Ginkel",
                Gender.MALE,
                null
            );

            ctx.saveOrUpdate(relation);

            if (createDependent) {
                for (int i = 1; i <= 3; i++) {
                    ctx.saveOrUpdate(new Address(
                        "Street",
                        i,
                        "City",
                        relation
                    ));
                }
            }

            ctx.commit();
        }

        return relation;
    }
}

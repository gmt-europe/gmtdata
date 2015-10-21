package nl.gmt.data;

import nl.gmt.data.test.TestConnection;
import nl.gmt.data.test.model.Address;
import nl.gmt.data.test.model.AddressRepository;
import nl.gmt.data.test.model.Relation;
import nl.gmt.data.test.model.RelationRepository;
import org.apache.commons.lang.Validate;

public abstract class DbConnectionFixtureBase {
    protected TestConnection openDb() throws Exception {
        return openDb(createConfiguration());
    }

    protected TestConnection openDb(DbConfiguration cfg) throws Exception {
        Validate.notNull(cfg, "cfg");

        TestConnection db = new TestConnection(cfg);

        db.migrateDatabase();

        // Clear the database.

        try (DbContext ctx = db.openContext()) {
            for (Address address : ctx.getRepository(AddressRepository.class).getAll()) {
                ctx.delete(address);
            }

            for (Relation relation : ctx.getRepository(RelationRepository.class).getAll()) {
                ctx.delete(relation);
            }

            ctx.commit();
        }

        return db;
    }

    protected DbConfiguration createConfiguration() {
        DbConfiguration cfg = new DbConfiguration();

        cfg.setConnectionString("jdbc:postgresql://attissrv02/nhtest?user=nhtest&password=w92Nbz3curpXxXuK&currentSchema=public");
        cfg.setType(DbType.POSTGRES);

        return cfg;
    }
}

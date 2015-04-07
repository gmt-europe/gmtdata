package nl.gmt.data;

import nl.gmt.data.test.TestConnection;
import nl.gmt.data.test.model.Relation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TranslationFixture extends DbConnectionFixtureBase {
    @Test
    public void translation() throws Exception {
        TestConnection db = this.openDb();

        EntityValidator validator = new EntityValidator(db);
        boolean hadOne = false;
        validator.validate(new Relation(), EntityValidatorMode.CREATE);
        for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
            if ("name".equals(entry.getKey().getFieldName())) {
                hadOne = true;
                assertEquals("Cannot be empty", entry.getValue());
            }
        }

        assertTrue(hadOne);
    }

    @Test
    public void customTranslation() throws Exception {
        DbConfiguration cfg = createConfiguration();
        cfg.setMessageResolver(new DbConfiguration.OnResolveMessage() {
            @Override
            public String onResolveMessage(String key) {
                if ("gmtdata.validation.cannot-be-empty".equals(key)) {
                    return "TEST";
                }

                return null;
            }
        });

        TestConnection db = this.openDb(cfg);

        EntityValidator validator = new EntityValidator(db);
        boolean hadOne = false;
        validator.validate(new Relation(), EntityValidatorMode.CREATE);
        for (Map.Entry<EntityField, String> entry : validator.getMessages().entrySet()) {
            if ("name".equals(entry.getKey().getFieldName())) {
                hadOne = true;
                assertEquals("TEST", entry.getValue());
            }
        }

        assertTrue(hadOne);
    }
}

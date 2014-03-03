package nl.gmt.data;

import nl.gmt.data.drivers.SQLiteDriver;
import nl.gmt.data.model.Address;
import nl.gmt.data.model.Gender;
import nl.gmt.data.model.Relation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

@RunWith(JUnit4.class)
public class DbConnectionFixture {
    @Test
    public void performTests() throws Exception {
        String path = "./tmp/gmtdatatest.db3";

        new File(path).getParentFile().mkdirs();

        SQLiteDriver.setPragma("journal_mode", "TRUNCATE");

        try (TestConnection db = new TestConnection("jdbc:sqlite:" + path, DbType.SQLITE)) {
            db.migrateDatabase();

            try (DbContext ctx = db.openContext()) {
                for (Relation relation : ctx.<Relation>createQuery("from Relation r")) {
                    ctx.delete(relation);
                }

                ctx.commit();
            }

            try (DbContext ctx = db.openContext()) {
                Relation relation = new Relation(
                    "Pieter van Ginkel",
                    Gender.MALE,
                    null
                    //FileUtils.readFileToByteArray(new File("/home/pvginkel/Desktop/apple.jpg"))
                );

                ctx.saveOrUpdate(relation);

                ctx.saveOrUpdate(new Address("Impostmeester", 13, "Assendelft", relation));
                ctx.saveOrUpdate(new Address("Muiderstraatweg", 15, "Diemen", relation));

                ctx.commit();
            }

            try (DbContext ctx = db.openContext()) {
                ctx.saveOrUpdate(new Relation("Laura van Ginkel", Gender.FEMALE, null));

                ctx.commit();
            }

            try (DbContext ctx = db.openContext()) {
                for (Relation relation : ctx.<Relation>createQuery("from Relation r")) {
                    System.out.println(relation.getName() + ": " + relation.getGender());

                    if (relation.getPicture() != null) {
                        try (OutputStream os = new FileOutputStream("/home/pvginkel/Desktop/" + relation.getName() + ".jpg")) {
                            IOUtils.write(relation.getPicture(), os);
                        }
                    }

                    for (Address address : relation.getAddresses()) {
                        System.out.println("  " + address.getStreet() + " " + address.getHouseNumber() + " " + address.getCity());
                    }
                }

                ctx.commit();
            }
        }
    }
}

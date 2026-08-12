package vn.nguongocso;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseConnectionTest(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {

        try (Connection connection = dataSource.getConnection()) {

            System.out.println("=================================");
            System.out.println("Connect MySQL Successfully!");
            System.out.println("URL      : " + connection.getMetaData().getURL());
            System.out.println("Database : " + connection.getCatalog());
            System.out.println("User     : " + connection.getMetaData().getUserName());
            System.out.println("=================================");

        } catch (Exception e) {

            System.out.println("Connect MySQL Failed!");
            e.printStackTrace();

        }

    }
}
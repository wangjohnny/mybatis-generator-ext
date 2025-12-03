import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

public class DemoTest {

    @Test
    public void test() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:hsqldb:file:demo;ifexists=true", "SA", "");
        Statement statement = c.createStatement();
        statement.executeQuery("create table user");
    }

}

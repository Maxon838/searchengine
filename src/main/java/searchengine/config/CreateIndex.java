package searchengine.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class CreateIndex {

    private static Connection connection;

    @PostConstruct
    public void createIndexPath()
    {
        System.out.println("Create Index");
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/search_engine?user=engine_user&password=access");
                connection.createStatement().execute("CREATE INDEX index_path ON page (path(50))");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

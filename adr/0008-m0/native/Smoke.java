import java.sql.*;
public class Smoke {
  public static void main(String[] a) throws Exception {
    Class.forName("org.duckdb.DuckDBDriver");
    try (Connection c = DriverManager.getConnection("jdbc:duckdb:"); Statement s = c.createStatement()) {
      ResultSet r = s.executeQuery("SELECT version(), CAST(timezone('UTC', to_timestamp(1704067199)) AS DATE)");
      r.next(); System.out.println("native OK engine=" + r.getString(1) + " date=" + r.getString(2));
    }
  }
}

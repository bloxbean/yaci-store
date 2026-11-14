import java.sql.*;
public class Ext {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:duckdb:"); Statement s = c.createStatement()) {
      ResultSet r = s.executeQuery("SELECT version(), (SELECT source_id FROM pragma_version())");
      r.next(); System.out.println("engine=" + r.getString(1) + " " + r.getString(2));
      r = s.executeQuery("SELECT extension_name, loaded, installed, install_mode FROM duckdb_extensions() WHERE install_mode='STATICALLY_LINKED' OR loaded OR installed ORDER BY 1");
      while (r.next()) System.out.println(r.getString(1)+" loaded="+r.getBoolean(2)+" installed="+r.getBoolean(3)+" mode="+r.getString(4));
      for (String q : new String[]{"SELECT timezone('UTC', TIMESTAMP '2024-01-01 00:00:00')", "SELECT TIMESTAMPTZ '2024-01-01 00:00:00+00' AT TIME ZONE 'UTC'", "SELECT current_setting('TimeZone')"}) {
        try { r = s.executeQuery(q); r.next(); System.out.println("OK  " + q + " -> " + r.getString(1)); }
        catch (SQLException e) { System.out.println("ERR " + q + " -> " + e.getMessage().split("\n")[0]); }
      }
    }
  }
}

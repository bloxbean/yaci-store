import java.sql.*;
public class Dates {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:duckdb:"); Statement s = c.createStatement()) {
      s.execute("SET TimeZone = 'Asia/Singapore'");
      s.execute("SET autoload_known_extensions = false"); s.execute("SET autoinstall_known_extensions = false");
      s.execute("SET enable_external_access = false"); s.execute("SET lock_configuration = true");
      String src = "1704067199"; // 2023-12-31 23:59:59 UTC = 2024-01-01 07:59:59 +08
      String[][] qs = {
        {"query layer today (UnifiedViewBuilder:210)", "CAST(to_timestamp(" + src + ") AT TIME ZONE 'UTC' AS DATE)"},
        {"writer today (DuckLakeWriterService:83)", "CAST(timezone('UTC', to_timestamp(" + src + ")) AS DATE)"},
        {"naive CAST(to_timestamp AS DATE)", "CAST(to_timestamp(" + src + ") AS DATE)"},
        {"ICU-free: make_timestamp(micros)", "CAST(make_timestamp(" + src + "::BIGINT * 1000000) AS DATE)"},
        {"ICU-free: epoch_ms", "CAST(epoch_ms(" + src + "::BIGINT * 1000) AS DATE)"},
        {"ICU-free: DATE + days", "DATE '1970-01-01' + CAST(floor(" + src + " / 86400) AS INTEGER)"},
      };
      for (String[] q : qs) {
        try { ResultSet r = s.executeQuery("SELECT " + q[1] + ", typeof(" + q[1] + ")"); r.next(); System.out.println(String.format("%-44s %s (%s)", q[0], r.getString(1), r.getString(2))); }
        catch (SQLException e) { System.out.println(String.format("%-44s ERR %s", q[0], e.getMessage().split("\n")[0])); }
      }
    }
  }
}

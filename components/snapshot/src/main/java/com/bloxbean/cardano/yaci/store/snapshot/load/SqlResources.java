package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SpecException;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and parameterises the trusted DuckDB transform resources used by {@code SQL} mode.
 *
 * <p>Resources come only from the local classpath of the matching Yaci Store release; a downloaded
 * archive contributes nothing but a spec id, version and digest. Each resource must be a single
 * read-only {@code SELECT} (a leading {@code WITH} is allowed) and may not contain DDL, transaction
 * control, {@code ATTACH}, {@code COPY} or {@code INSTALL}.
 *
 * <p>A transform names its inputs as relations, {@code FROM ${files}} and {@code FROM ${dep.<id>}},
 * and never scans files itself. The caller decides what each one is: a scan of the batch's files
 * at import, a sample at export, or an empty relation of the recorded types when only the output
 * types are wanted.
 */
public final class SqlResources {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}");

    /** Statement keywords that must never appear in a transform resource. */
    private static final String[] FORBIDDEN = {
            "insert", "update", "delete", "create", "drop", "alter", "truncate", "attach", "detach",
            "copy", "install", "load", "call", "pragma", "begin", "commit", "rollback", "export",
            "import", "set ", "grant", "revoke", "vacuum"
    };

    /** File scans a transform must leave to the caller; see {@code DuckDb.readParquet}. */
    private static final String[] SCANS = {"read_parquet", "parquet_scan"};

    private SqlResources() {
    }

    public static String read(String resourceRef) {
        if (!resourceRef.startsWith("classpath:")) {
            throw new SpecException("Only classpath SQL resources are supported: " + resourceRef);
        }
        String path = resourceRef.substring("classpath:".length());
        try (InputStream in = SqlResources.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new SpecException("SQL transform resource not found on the classpath: " + path);
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            validate(path, sql);
            return sql;
        } catch (IOException e) {
            throw new SpecException("Unable to read SQL transform resource " + path, e);
        }
    }

    static void validate(String path, String sql) {
        String stripped = stripComments(sql).trim();
        if (stripped.isEmpty()) {
            throw new SpecException(path + ": transform resource is empty");
        }
        String lower = stripped.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            throw new SpecException(path + ": a transform must be a single SELECT (optionally with a "
                    + "leading WITH clause)");
        }
        if (stripped.contains(";")) {
            throw new SpecException(path + ": a transform must contain exactly one statement and no "
                    + "semicolon");
        }
        for (String kw : FORBIDDEN) {
            if (containsKeyword(lower, kw)) {
                throw new SpecException(path + ": transform resources must be read-only; found '"
                        + kw.trim() + "'");
            }
        }
        for (String scan : SCANS) {
            if (containsKeyword(lower, scan)) {
                throw new SpecException(path + ": a transform reads its inputs through ${files} and "
                        + "${dep.<id>}, not by calling " + scan + " itself");
            }
        }
    }

    private static boolean containsKeyword(String lower, String keyword) {
        String kw = keyword.trim();
        int from = 0;
        while (true) {
            int i = lower.indexOf(kw, from);
            if (i < 0) {
                return false;
            }
            boolean leftOk = i == 0 || !Character.isLetterOrDigit(lower.charAt(i - 1)) && lower.charAt(i - 1) != '_';
            int end = i + kw.length();
            boolean rightOk = end >= lower.length()
                    || !Character.isLetterOrDigit(lower.charAt(end)) && lower.charAt(end) != '_';
            if (leftOk && rightOk) {
                return true;
            }
            from = i + 1;
        }
    }

    static String stripComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\n")) {
            int i = line.indexOf("--");
            out.append(i >= 0 ? line.substring(0, i) : line).append('\n');
        }
        return out.toString();
    }

    /**
     * Bind a transform to its inputs and the consistency point.
     *
     * @param source       the relation {@code ${files}} stands for
     * @param dependencies dependency spec id to the relation {@code ${dep.<id>}} stands for
     */
    public static String bindTransform(String sql, String source, Map<String, String> dependencies,
                                       long cutSlot, int completedEpoch) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("files", source);
        params.put("cutSlot", Long.toString(cutSlot));
        params.put("completedEpoch", Integer.toString(completedEpoch));
        params.put("uuidNamespace", Identifiers.literal(ConverterRegistry.UUID_NAMESPACE));
        dependencies.forEach((dep, relation) -> params.put("dep." + dep, relation));
        return bind(sql, params);
    }

    /**
     * Substitute {@code ${name}} placeholders. Values are produced by the importer itself (file
     * lists, the cutoff slot, the completed epoch), never by the snapshot archive.
     */
    public static String bind(String sql, Map<String, String> parameters) {
        Matcher m = PLACEHOLDER.matcher(sql);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = parameters.get(name);
            if (value == null) {
                throw new SpecException("SQL transform references unknown parameter ${" + name + "}");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}

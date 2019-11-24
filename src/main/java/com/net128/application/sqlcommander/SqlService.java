package com.net128.application.sqlcommander;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.CSVWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@Service
@SuppressWarnings("unused")
class SqlService {
    private static Logger logger = LoggerFactory.getLogger(SqlService.class);

    @Inject
    private DataSource dataSource;

    boolean executeSql(String sql, OutputStream os, String outputMimeType) {
        sql = sql.trim().replaceAll(";$", "");
        try (Connection connection = dataSource.getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery(sql);
            if ("application/json".equals(outputMimeType+"")) {
                new StreamingJsonResultSetExtractor(os).extractData(rs);
            } else {
                boolean tabDelimited = !"text/csv".equals(outputMimeType+"");
                new StreamingCsvResultSetExtractor(os, tabDelimited).extractData(rs);
            }
            return true;
        } catch (AbortedException e) {
            logger.error("{} while executing: {} ...", e.getMessage(), sql.substring(0, Math.min(100, sql.length())));
            return false;
        } catch (Exception e) {
            handleSqlExecutionException(e, sql, new PrintStream(os));
            return false;
        }
    }

    boolean updateSql(String sql, OutputStream os) {
        PrintStream pos=new PrintStream(os);
        try (InputStream is = new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8.name()))) {
            Resource resource = new InputStreamResource(is);
            ResourceDatabasePopulator databasePopulator =
                new ResourceDatabasePopulator(false,
                true, StandardCharsets.UTF_8.name(), resource);
            databasePopulator.execute(dataSource);
            pos.println("Success\n<OK>");
            return true;
        } catch (Exception e) {
            handleSqlExecutionException(e, sql, pos);
            return false;
        }
    }

    private void handleSqlExecutionException(Exception e, String sql, PrintStream pos) {
        String message = "Failed to execute: " + sql;
        if(e.getClass().getPackage().getName().startsWith("org.spring") && ! (e instanceof ScriptStatementFailedException)) {
            throw new RuntimeException(String.format("Error executing:\n\t\t%s", sql.trim()), e);
        } else if (logger.isDebugEnabled() || (!(e instanceof SQLSyntaxErrorException) &&
                !e.getClass().getName().contains("OracleDatabaseException"))) {
            logger.error("{}", sql, e);
        } else {
            logger.error("{}: {}", message, e.getMessage());
        }
        if(e instanceof SQLException || e instanceof ScriptStatementFailedException) {
            pos.print(e.getMessage()
                .replaceAll("[\t\n]", " ")
                .replaceAll("[ ]+", " ").trim());
        }
    }

    private class StreamingCsvResultSetExtractor {
        private final OutputStream os;
        private final boolean tabDelimited;

        StreamingCsvResultSetExtractor(OutputStream os, boolean tabDelimited) {
            this.os = os;
            this.tabDelimited = tabDelimited;
        }

        void extractData(final ResultSet rs) throws SQLException, IOException {
            char separator = tabDelimited ? '\t' : CSVWriter.DEFAULT_SEPARATOR;
            try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (CSVWriter writer = new CSVWriter(osw, separator,
                        CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END) {
                    @Override
                    protected void processCharacter(Appendable appendable, char nextChar) throws IOException {
                        if (nextChar == '\t') {
                            appendable.append('\\').append('t');
                        } else if (nextChar == '\n') {
                            appendable.append('\\').append('n');
                        } else {
                            super.processCharacter(appendable, nextChar);
                        }
                    }
                }) {
                    writer.writeAll(rs, true);
                }
            }
        }
    }

    private class StreamingJsonResultSetExtractor implements ResultSetExtractor<Void> {
        private final OutputStream os;
        StreamingJsonResultSetExtractor(OutputStream os) {
            this.os = os;
        }

        @Override
        public Void extractData(@Nonnull ResultSet rs) throws SQLException {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            try (JsonGenerator jg =
                    objectMapper.getFactory().createGenerator(os, JsonEncoding.UTF8)) {
                writeResultSetToJson(rs, jg);
            } catch (IOException e) {
                throw new AbortedException(e.getMessage(), e);
            }
            return null;
        }

        private void writeResultSetToJson(final ResultSet rs, final JsonGenerator jg)
                throws SQLException, IOException {
            final ResultSetMetaData rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            jg.writeStartArray();
            while (rs.next()) {
                jg.writeStartObject();
                for (int i = 1; i <= columnCount; i++) {
                    jg.writeObjectField(rsmd.getColumnName(i), rs.getObject(i));
                }
                jg.writeEndObject();
                jg.flush();
            }
            jg.writeEndArray();
        }
    }

    private class AbortedException extends RuntimeException {
        AbortedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Profile("customds")
    @Configuration
    @ConfigurationProperties(prefix = "spring.custom.datasource")
    @SuppressWarnings("unused")
    public class CustomHikariDSConfiguration extends HikariConfig {
        @Bean
        @Lazy
        public DataSource dataSource() {
            return new HikariDataSource(this);
        }
    }
}

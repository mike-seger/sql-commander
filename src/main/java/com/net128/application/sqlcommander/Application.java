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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@SpringBootApplication
@SuppressWarnings("unused")
public class Application extends SpringBootServletInitializer {
	private static Logger logger = LoggerFactory.getLogger(Application.class);

	@Inject
	@Lazy
	private DataSource dataSource;

	@Controller
	public class UiController {
		@Inject
		private SqlService sqlService;

		public class SqlQuery {
			private String query;
			public void setQuery(String query) {
				this.query = query;
			}
			String getQuery() {
				return query;
			}
		}

		@GetMapping({"/sql", "/"})
		public String sql() {
			return "index.html";
		}

		@PostMapping("/sql")
		public void postSql(SqlQuery sqlQuery, HttpServletResponse response, @RequestHeader("Accept") String accept) throws IOException {
			String sql=sqlQuery.getQuery().trim().replaceAll(";$", "");
			if(sql.toLowerCase().startsWith("select")) {
				sqlService.executeSql(sql, response.getOutputStream(), accept);
			} else {
				sqlService.updateSql(sql, response.getOutputStream());
			}
		}
	}

	@RestController
	public class ApiController {
		@Inject
		private SqlService sqlService;

		@PostMapping(value = "/select",
			consumes = "text/plain",
			produces = {"text/csv", "application/json", "text/tab-separated-values"})
		public void executeSql(@RequestBody String sql, HttpServletResponse response, @RequestHeader("Accept") String accept) throws IOException {
			sqlService.executeSql(sql, response.getOutputStream(), accept);
		}

		@PostMapping("/update")
		public void updateSql(@RequestBody String sql, HttpServletResponse response) throws IOException {
			sqlService.updateSql(sql, response.getOutputStream());
		}
	}

	@Service
	public class SqlService {
		void executeSql(String sql, OutputStream os, String mimeType) {
			sql = sql.trim().replaceAll(";$", "");
			try (Connection connection = dataSource.getConnection()) {
				ResultSet rs = connection.createStatement().executeQuery(sql);
				if (MediaType.APPLICATION_JSON.getType().equals(mimeType)) {
					new StreamingJsonResultSetExtractor(os).extractData(rs);
				} else {
					boolean tabDelimited = !"text/csv".equals(mimeType);
					new StreamingCsvResultSetExtractor(os, tabDelimited).extractData(rs);
				}
			} catch (AbortedException e) {
				logger.error("{} while executing: {} ...", e.getMessage(), sql.substring(0, Math.min(100, sql.length())));
			} catch (Exception e) {
				new PrintStream(os).println(e.getMessage());
				handleSqlExecutionException(e, sql);
			}
		}

		void updateSql(String sql, OutputStream os) {
			PrintStream pos=new PrintStream(os);
			try (InputStream is = new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8.name()))) {
				Resource resource = new InputStreamResource(is);
				ResourceDatabasePopulator databasePopulator =
					new ResourceDatabasePopulator(false,
					true, StandardCharsets.UTF_8.name(), resource);
				databasePopulator.execute(dataSource);
				pos.println("<OK>");
			} catch (Exception e) {
				pos.println(e.getMessage());
				handleSqlExecutionException(e, sql);
			}
		}

		private void handleSqlExecutionException(Exception e, String sql) {
			String message = "Failed to execute: " + sql;
			if (logger.isDebugEnabled() || (!(e instanceof SQLSyntaxErrorException) &&
					!e.getClass().getName().contains("OracleDatabaseException"))) {
				logger.error("{}", sql, e);
			} else {
				logger.error("{}: {}", message, e.getMessage());
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
	}

	@Profile("customds")
	@Configuration
	@ConfigurationProperties(prefix = "spring.custom.datasource")
	public class CustomHikariDSConfiguration extends HikariConfig {
		@Bean
		public DataSource dataSource() {
			return new HikariDataSource(this);
		}
	}

	static {
		String addLoc="spring.config.additional-location";
		if(System.getProperty(addLoc)==null) {
			String userHome=System.getProperty("user.home");
			String location=userHome+"/.springboot/"+
				Application.class.getPackage().getName()+".properties";
			System.setProperty(addLoc, location);
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}
}

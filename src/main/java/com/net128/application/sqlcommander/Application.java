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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@SpringBootApplication
@RestController
public class Application extends SpringBootServletInitializer {
	private static Logger logger = LoggerFactory.getLogger(Application.class);

	@Autowired
	private DataSource dataSource;

	@PostMapping(value="/select",
			consumes = "text/plain",
			produces = {"text/csv", "application/json", "text/tab-separated-values"})
	public void executeSql(@RequestBody String sql, HttpServletResponse response, @RequestHeader("Accept") String accept) {
		try (Connection connection = dataSource.getConnection()) {
			ResultSet rs = connection.createStatement().executeQuery(sql);
			if(MediaType.APPLICATION_JSON.getType().equals(accept)) {
				new StreamingJsonResultSetExtractor(response.getOutputStream()).extractData(rs);
			} else {
				boolean tabDelimited=!"text/csv".equals(accept);
				new StreamingCsvResultSetExtractor(response.getOutputStream(), tabDelimited).extractData(rs);
			}
		} catch (AbortedException e) {
			logger.error("{} while executing: {} ...", e.getMessage(),
					sql.substring(0, Math.min(100, sql.length())));
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute: " + sql, e);
		}
	}

	@PostMapping("/update")
	public int updateSql(@RequestBody String sql, HttpServletResponse response) {
		try (Connection connection = dataSource.getConnection()) {
			return connection.createStatement().executeUpdate(sql);
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute: " + sql, e);
		}
	}

	private class StreamingCsvResultSetExtractor {
		private final OutputStream os;
		private final boolean tabDelimited;

		public StreamingCsvResultSetExtractor(OutputStream os, boolean tabDelimited) throws IOException {
			this.os = os;
			this.tabDelimited = tabDelimited;
		}

		public void extractData(final ResultSet rs) throws SQLException, IOException {
			char separator=tabDelimited?'\t':CSVWriter.DEFAULT_SEPARATOR;
			try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
				try (CSVWriter writer = new CSVWriter(osw, separator,
						CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER,
						CSVWriter.DEFAULT_LINE_END){
					@Override
					protected void processCharacter(Appendable appendable, char nextChar) throws IOException {
						if(nextChar == '\t') {
							appendable.append('\\').append('t');
						} else if(nextChar == '\n') {
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

		public StreamingJsonResultSetExtractor(OutputStream os) throws IOException {
			this.os = os;
		}

		@Override
		public Void extractData(final ResultSet rs) throws SQLException  {
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
		public AbortedException(String message, Throwable cause) {
			super(message, cause);
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

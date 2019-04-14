package com.net128.application.sqlcommander;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
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

	@PostMapping("/select")
	public void executeSql(@RequestBody String sql, HttpServletResponse response) {
		try (Connection connection = dataSource.getConnection()) {
			ResultSet rs = connection.createStatement().executeQuery(sql);
			new StreamingJsonResultSetExtractor(response.getOutputStream()).extractData(rs);
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

package com.net128.application.sqlcommander;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@SuppressWarnings("unused")
public class UiController {
    @Inject
    private SqlService sqlService;

    public class SqlQuery {
        private String query;
        private String accept;
        public void setQuery(String query) {
            this.query = query;
        }
        String getQuery() {
            return query;
        }
        public void setAccept(String accept) { this.accept = accept; }
        String getAccept() { return accept; }
    }

    @GetMapping({"/sql", "/"})
    public String sql() {
        return "index.html";
    }

    @GetMapping({"/plain"})
    public String sqlPlain() {
        return "plain/index.html";
    }

    @PostMapping("/sql")
    public ResponseEntity postSql(SqlQuery sqlQuery, HttpServletResponse response) throws IOException {
        String sql=sqlQuery.getQuery().trim().replaceAll(";$", "");
        boolean success;
        if(sql.toLowerCase().startsWith("select")) {
            success=sqlService.executeSql(sql, response.getOutputStream(), sqlQuery.getAccept());
        } else {
            success=sqlService.updateSql(sql, response.getOutputStream());
        }
        if(!success) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }
}

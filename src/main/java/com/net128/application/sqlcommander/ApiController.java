package com.net128.application.sqlcommander;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@SuppressWarnings("unused")
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

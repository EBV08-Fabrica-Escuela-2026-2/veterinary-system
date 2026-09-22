package com.veterinaria.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "DATABASE_URL")
public class DatabaseUrlDataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String[] credentials = uri.getUserInfo().split(":", 2);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(buildJdbcUrl(uri));
        dataSource.setUsername(decode(credentials[0]));
        dataSource.setPassword(credentials.length > 1 ? decode(credentials[1]) : "");
        return dataSource;
    }

    private String buildJdbcUrl(URI uri) {
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        return uri.getQuery() == null ? jdbcUrl : jdbcUrl + "?" + uri.getQuery();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}

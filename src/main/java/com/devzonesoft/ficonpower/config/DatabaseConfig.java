package com.devzonesoft.ficonpower.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class DatabaseConfig {

    @Bean
    DataSource dataSource(Environment environment, ZoneId applicationTimeZone) {
        HikariConfig config = new HikariConfig();
        String databaseUrl = environment.getProperty("DATABASE_URL");

        if (StringUtils.hasText(databaseUrl)) {
            configureFromUrl(config, databaseUrl);
            if (!StringUtils.hasText(config.getUsername())) {
                config.setUsername(environment.getProperty("DB_USER", "postgres"));
                config.setPassword(environment.getProperty("DB_PASSWORD", ""));
            }
        } else {
            String host = environment.getProperty("DB_HOST", "localhost");
            String port = environment.getProperty("DB_PORT", "5432");
            String database = environment.getProperty("DB_NAME", "postgres");
            String sslMode = environment.getProperty("DB_SSLMODE", "require");
            config.setJdbcUrl("jdbc:postgresql://%s:%s/%s?sslmode=%s"
                    .formatted(host, port, database, sslMode));
            config.setUsername(environment.getProperty("DB_USER", "postgres"));
            config.setPassword(environment.getProperty("DB_PASSWORD", ""));
        }

        if (config.getJdbcUrl().startsWith("jdbc:postgresql:")) {
            // Supabase's transaction pooler cannot preserve server-side prepared
            // statements between borrowed connections.
            config.addDataSourceProperty("prepareThreshold", "0");
            config.setConnectionInitSql(
                    "SET TIME ZONE '%s'".formatted(applicationTimeZone.getId()));
        }
        config.setPoolName("ficon-power-pool");
        return new HikariDataSource(config);
    }

    private void configureFromUrl(HikariConfig config, String rawUrl) {
        if (rawUrl.startsWith("jdbc:")) {
            config.setJdbcUrl(rawUrl);
            return;
        }

        URI uri = URI.create(rawUrl);
        String query = StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "";
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        config.setJdbcUrl("jdbc:postgresql://%s:%d%s%s"
                .formatted(uri.getHost(), port, uri.getPath(), query));

        if (StringUtils.hasText(uri.getUserInfo())) {
            String[] credentials = uri.getUserInfo().split(":", 2);
            config.setUsername(decode(credentials[0]));
            if (credentials.length == 2) {
                config.setPassword(decode(credentials[1]));
            }
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}

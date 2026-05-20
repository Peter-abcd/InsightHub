//package com.greate.community.config;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//
//import javax.sql.DataSource;
//
//@Configuration
//public class ClickHouseConfig {
//
//    @Value("${clickhouse.datasource.url}")
//    private String url;
//
//    @Value("${clickhouse.datasource.username}")
//    private String username;
//
//    @Value("${clickhouse.datasource.password}")
//    private String password;
//
//    @Value("${clickhouse.datasource.driver-class-name}")
//    private String driverClassName;
//
//    @Bean(name = "clickHouseDataSource")
//    public DataSource clickHouseDataSource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setUrl(url);
//        dataSource.setUsername(username);
//        dataSource.setPassword(password);
//        dataSource.setDriverClassName(driverClassName);
//        return dataSource;
//    }
//
//    @Bean(name = "clickHouseJdbcTemplate")
//    public JdbcTemplate clickHouseJdbcTemplate(
//            @Qualifier("clickHouseDataSource") DataSource dataSource) {
//        return new JdbcTemplate(dataSource);
//    }
//}

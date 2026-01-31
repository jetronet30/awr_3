package com.jaba.awr_3.seversettings.configurations;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.sql.DataSource;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jaba.awr_3.inits.postgres.DataService;
import com.jaba.awr_3.seversettings.basic.BasicService;
import com.zaxxer.hikari.HikariDataSource;



import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ServerConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(BasicService.LISTING_PORT);
        factory.setServerHeader(null);
        try {
            factory.setAddress(InetAddress.getByName(BasicService.LISTING_ADDRESS));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(DataService.getDataSettingsStatic().getDataUrl());
        ds.setUsername(DataService.getDataSettingsStatic().getDataUser());
        ds.setPassword(DataService.getDataSettingsStatic().getDataPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

}

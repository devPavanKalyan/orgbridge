package org.verse.orgbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrgBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgBridgeApplication.class, args);
    }

}

package com.example.portal_oportunidades_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PortalOportunidadesBackApplication {

    public static void main(String[] args) {
        var application = new SpringApplication(PortalOportunidadesBackApplication.class);
        if (args.length > 0 && "bootstrap-admin".equals(args[0])) {
            application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        }
        application.run(args);
    }

}

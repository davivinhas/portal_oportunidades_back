package com.example.portal_oportunidades_back.auth;

import com.example.portal_oportunidades_back.auth.service.AdministratorBootstrapService;
import java.io.Console;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final AdministratorBootstrapService service;
    private final ConfigurableApplicationContext context;

    public BootstrapAdminRunner(AdministratorBootstrapService service, ConfigurableApplicationContext context) {
        this.service = service; this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getSourceArgs().length == 0 || !"bootstrap-admin".equals(args.getSourceArgs()[0])) return;
        int exitCode = 1;
        try {
            Console console = System.console();
            if (console == null) throw new IllegalStateException("Interactive console is required to read the password securely");
            String name = console.readLine("Name: ");
            String email = console.readLine("Email: ");
            char[] password = console.readPassword("Password: ");
            try {
                Long id = service.create(name, email, password);
                console.printf("Administrator created successfully (id=%d).%n", id);
                exitCode = 0;
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        } catch (Exception exception) {
            System.err.println("Could not bootstrap administrator: " + exception.getMessage());
        }
        int code = exitCode;
        int status = org.springframework.boot.SpringApplication.exit(context, () -> code);
        System.exit(status);
    }
}

package ch.unibas.medizin.depot;

import org.springframework.boot.SpringApplication;

public class DepotTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(DepotApplication::main)
                .run(args);
    }

}

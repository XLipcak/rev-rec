package muni.fi.revrec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@SpringBootApplication
@EnableScheduling
public class ReviewrecommendationssystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewrecommendationssystemApplication.class, args);
    }
}

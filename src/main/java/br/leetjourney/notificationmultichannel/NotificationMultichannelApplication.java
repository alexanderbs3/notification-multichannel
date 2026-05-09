package br.leetjourney.notificationmultichannel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotificationMultichannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationMultichannelApplication.class, args);
    }

}

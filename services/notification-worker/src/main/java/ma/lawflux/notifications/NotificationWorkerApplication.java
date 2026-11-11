package ma.lawflux.notifications;
import ma.lawflux.platform.config.PlatformConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
@SpringBootApplication
@Import(PlatformConfiguration.class)
public class NotificationWorkerApplication {
    public static void main(String[] args) { SpringApplication.run(NotificationWorkerApplication.class, args); }
}

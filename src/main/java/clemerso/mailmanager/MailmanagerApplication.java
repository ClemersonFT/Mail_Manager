package clemerso.mailmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MailmanagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(
				MailmanagerApplication.class,
				args
		);
		App app = new App();
		app.start();
	}
}
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

		if (args.length > 0 && args[0].equals("--manual")) {
			Config config = new Config("manual"); // pede PIN
			App app = new App(config);
			app.start();
		} else {
			Config config = new Config("auto"); // lê PIN do arquivo
			EmailCleaner emailCleaner = new EmailCleaner(
					config.getEmail(),
					config.getPassword()
			);
			emailCleaner.autoClean();
			emailCleaner.cleanSpam();
		}
	}
}
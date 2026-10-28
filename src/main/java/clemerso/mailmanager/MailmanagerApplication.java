package clemerso.mailmanager;

import java.util.Scanner;

public class MailmanagerApplication {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		Config config = new Config(scanner);
		new App(config, scanner).start();
	}
}
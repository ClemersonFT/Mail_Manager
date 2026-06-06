package clemerso.mailmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    private final Config config;

    public App(Config config) {
        this.config = config;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        String email = config.getEmail();
        String password = config.getPassword();

        System.out.println("1 - Limpar spam");
        System.out.println("2 - Limpar por palavra-chave");
        System.out.println("3 - Listar por quantidade de emails enviados");
        System.out.println("4 - Cancelar");

        int option = scanner.nextInt();
        scanner.nextLine();

        switch (option) {

            case 1:
                EmailCleaner emailCleaner = new EmailCleaner(email, password);
                emailCleaner.cleanSpam();
                break;

            case 2:
                List<String> keywords = new ArrayList<>();
                String response;

                do {
                    System.out.print("Digite uma palavra-chave: ");
                    String kw = scanner.nextLine().trim().toLowerCase();
                    if (!kw.isEmpty()) keywords.add(kw);

                    System.out.print("Adicionar outra? (s/n): ");
                    response = scanner.nextLine().trim().toLowerCase();
                } while (response.equals("s"));

                emailCleaner = new EmailCleaner(email, password);
                emailCleaner.cleanByKeywords(keywords);
                break;

            case 3:
                emailCleaner = new EmailCleaner(email, password);
                emailCleaner.topSenders();
                break;

            case 4:
                System.out.println("Cancelado.");
                break;
        }
    }
}
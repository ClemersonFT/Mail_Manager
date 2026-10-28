package clemerso.mailmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    private final Config config;
    private final Scanner scanner;

    public App(Config config, Scanner scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public void start() {
        EmailCleaner cleaner = new EmailCleaner(config.getEmail(), config.getPassword(), scanner);

        System.out.println("1 - Limpar spam");
        System.out.println("2 - Limpar por palavra-chave");
        System.out.println("3 - Listar por quantidade de emails enviados");
        System.out.println("4 - Limpar por blacklist");
        System.out.println("5 - Cancelar");

        String option = scanner.nextLine().trim();

        switch (option) {
            case "1" -> cleaner.cleanSpam();

            case "2" -> {
                List<String> keywords = new ArrayList<>();
                String response;

                do {
                    System.out.print("Digite uma palavra-chave: ");
                    String kw = scanner.nextLine().trim().toLowerCase();
                    if (!kw.isEmpty()) keywords.add(kw);

                    System.out.print("Adicionar outra? (s/n): ");
                    response = scanner.nextLine().trim().toLowerCase();
                } while (response.equals("s"));

                cleaner.cleanByKeywords(keywords);
            }

            case "3" -> cleaner.topSenders();
            case "4" -> cleaner.cleanByBlacklist();
            case "5" -> System.out.println("Cancelado.");
            default -> System.out.println("Opção inválida.");
        }
    }
}
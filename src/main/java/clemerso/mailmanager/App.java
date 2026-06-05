package clemerso.mailmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {
    public void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite seu email");
        String email = scanner.nextLine();

        System.out.println("Digite sua senha");
        String password = scanner.nextLine();

        System.out.println("1 - Limpar spam");
        System.out.println("2 - Limpar lojas");
        System.out.println("3 - Cancelar");

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
                System.out.println("Cancelado.");
                break;
        }
    }
    }

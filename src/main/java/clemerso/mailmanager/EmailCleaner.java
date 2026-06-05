package clemerso.mailmanager;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class EmailCleaner {

    private final MailConnection connection;

    public EmailCleaner(String email, String password) {
        this.connection = new MailConnection(email, password);
    }

    public void cleanByKeywords(List<String> keywords) {
        try {
            Store store = connection.connect();

            Folder allMail = store.getFolder("[Gmail]/Todos os e-mails");
            allMail.open(Folder.READ_WRITE);

            Folder trash = store.getFolder("[Gmail]/Lixeira");
            trash.open(Folder.READ_WRITE);

            int total = allMail.getMessageCount();
            int batchSize = 500;

            List<Message> toDelete = new ArrayList<>();

            for (int i = 1; i <= total; i += batchSize) {
                int end = Math.min(i + batchSize - 1, total);
                Message[] messages = allMail.getMessages(i, end);

                for (Message message : messages) {
                    Address[] fromAddresses = message.getFrom();
                    if (fromAddresses == null || fromAddresses.length == 0) continue;

                    InternetAddress addr = (InternetAddress) fromAddresses[0];
                    String combined = (addr.getAddress() + " " +
                            (addr.getPersonal() != null ? addr.getPersonal() : "")).toLowerCase();

                    if (keywords.stream().anyMatch(kw -> combined.contains(kw))) {
                        toDelete.add(message);
                        System.out.println("Marcado: " + combined);
                    }
                }
            }

            if (!toDelete.isEmpty()) {
                System.out.println("\nOs seguintes emails serão deletados:");
                for (Message m : toDelete) {
                    InternetAddress addr = (InternetAddress) m.getFrom()[0];
                    String combined = (addr.getAddress() + " " +
                            (addr.getPersonal() != null ? addr.getPersonal() : "")).toLowerCase();
                    System.out.println(" - " + combined);
                }

                System.out.print("\nConfirma exclusão de " + toDelete.size() + " emails? (s/n): ");
                Scanner scanner = new Scanner(System.in);
                String confirm = scanner.nextLine().trim().toLowerCase();

                if (confirm.equals("s")) {

                    int flagBatch = 50;
                    Message[] deleteArray = toDelete.toArray(new Message[0]);
                    for (int i = 0; i < deleteArray.length; i += flagBatch) {
                        int end = Math.min(i + flagBatch, deleteArray.length);
                        Message[] batch = Arrays.copyOfRange(deleteArray, i, end);

                        for (Message m : batch) {
                            m.setFlag(Flags.Flag.SEEN, true);
                        }

                        Thread.sleep(500);
                        System.out.println("Processados: " + end + "/" + deleteArray.length);
                    }

                    allMail.copyMessages(deleteArray, trash);
                    System.out.println("Movidos para lixeira: " + toDelete.size());

                    Message[] trashMessages = trash.getMessages();
                    for (Message m : trashMessages) {
                        m.setFlag(Flags.Flag.DELETED, true);
                    }
                    trash.close(true);
                    System.out.println("Lixeira esvaziada.");
                } else {
                    System.out.println("Operação cancelada.");
                    allMail.close(false);
                    trash.close(false);
                    store.close();
                    return;
                }
            } else {
                System.out.println("Nenhum email encontrado com essas palavras-chave.");
            }

            allMail.close(true);
            store.close();

        } catch (MessagingException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cleanSpam() {
        try {
            Store store = connection.connect();

            Folder folder = store.getFolder("[Gmail]/Spam");
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();

            int flagBatch = 50;
            for (int i = 0; i < messages.length; i += flagBatch) {
                int end = Math.min(i + flagBatch, messages.length);
                Message[] batch = Arrays.copyOfRange(messages, i, end);

                for (Message m : batch) {
                    m.setFlag(Flags.Flag.DELETED, true);
                }

                Thread.sleep(500);
                System.out.println("Processados: " + end + "/" + messages.length);
            }

            folder.close(true);
            store.close();
            System.out.println("Total deletado: " + messages.length);

        } catch (MessagingException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
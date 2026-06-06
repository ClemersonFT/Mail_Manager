package clemerso.mailmanager;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.*;

import java.util.*;
import java.util.stream.Collectors;

public class EmailCleaner {

    private final MailConnection connection;

    public EmailCleaner(String email, String password) {
        this.connection = new MailConnection(email, password);
    }

    public void topSenders() {
        try {
            Store store = connection.connect();

            Folder allMail = store.getFolder("[Gmail]/Todos os e-mails");
            allMail.open(Folder.READ_WRITE);

            Folder trash = store.getFolder("[Gmail]/Lixeira");
            trash.open(Folder.READ_WRITE);

            int total = allMail.getMessageCount();
            int batchSize = 500;

            Map<String, Integer> contagem = new HashMap<>();

            System.out.println("Analisando " + total + " emails...");

            for (int i = 1; i <= total; i += batchSize) {
                int end = Math.min(i + batchSize - 1, total);
                Message[] messages = allMail.getMessages(i, end);

                for (Message message : messages) {
                    Address[] fromAddresses = message.getFrom();
                    if (fromAddresses == null || fromAddresses.length == 0) continue;

                    InternetAddress addr = (InternetAddress) fromAddresses[0];
                    String sender = addr.getAddress().toLowerCase();

                    contagem.merge(sender, 1, Integer::sum);
                }

                System.out.println("Processados: " + end + "/" + total);
            }

            List<Map.Entry<String, Integer>> top5 = contagem.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            System.out.println("\nTop 5 remetentes:");
            for (int i = 0; i < top5.size(); i++) {
                System.out.println((i + 1) + ". " + top5.get(i).getKey() + " - " + top5.get(i).getValue() + " emails");
            }

            Scanner scanner = new Scanner(System.in);
            List<String> sendersToDelete = new ArrayList<>();
            String response;

            do {
                System.out.print("\nDigite o número para deletar (0 para sair): ");
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 0) break;

                String sender = top5.get(choice - 1).getKey();
                sendersToDelete.add(sender);
                System.out.println("Adicionado: " + sender);

                System.out.print("Adicionar outro? (s/n): ");
                response = scanner.nextLine().trim().toLowerCase();

            } while (response.equals("s"));

            if (sendersToDelete.isEmpty()) {
                System.out.println("Operação cancelada.");
                allMail.close(false);
                trash.close(false);
                store.close();
                return;
            }

            System.out.println("\nRemetentes selecionados:");
            sendersToDelete.forEach(s -> System.out.println(" - " + s));

            List<Message> toDelete = new ArrayList<>();

            for (int i = 1; i <= total; i += batchSize) {
                int end = Math.min(i + batchSize - 1, total);
                Message[] messages = allMail.getMessages(i, end);

                for (Message message : messages) {
                    Address[] fromAddresses = message.getFrom();
                    if (fromAddresses == null || fromAddresses.length == 0) continue;

                    InternetAddress addr = (InternetAddress) fromAddresses[0];
                    String sender = addr.getAddress().toLowerCase();

                    if (sendersToDelete.contains(sender)) {
                        toDelete.add(message);
                    }
                }
            }

            Message[] deleteArray = toDelete.toArray(new Message[0]);
            allMail.setFlags(deleteArray, new Flags(Flags.Flag.SEEN), true);
            allMail.copyMessages(deleteArray, trash);
            System.out.println("Movidos para lixeira: " + toDelete.size());

            Message[] trashMessages = trash.getMessages();
            trash.setFlags(trashMessages, new Flags(Flags.Flag.DELETED), true);
            trash.close(true);
            System.out.println("Lixeira esvaziada.");

            allMail.close(true);
            store.close();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
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
                    Message[] deleteArray = toDelete.toArray(new Message[0]);

                    allMail.setFlags(deleteArray, new Flags(Flags.Flag.SEEN), true);

                    allMail.copyMessages(deleteArray, trash);
                    System.out.println("Movidos para lixeira: " + toDelete.size());

                    Message[] trashMessages = trash.getMessages();
                    allMail.setFlags(trashMessages, new Flags(Flags.Flag.DELETED), true); // mesma coisa aqui
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

        }catch (MessagingException e)  {
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

    public void autoClean() {
        try {
            System.out.println("Iniciando limpeza automática...");
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

                    if (BlackList.SENDERS.stream().anyMatch(combined::contains)) {
                        toDelete.add(message);
                    }
                }

                System.out.println("Verificados: " + end + "/" + total);
            }

            if (!toDelete.isEmpty()) {
                Message[] deleteArray = toDelete.toArray(new Message[0]);
                allMail.setFlags(deleteArray, new Flags(Flags.Flag.SEEN), true);
                allMail.copyMessages(deleteArray, trash);
                System.out.println("Movidos para lixeira: " + toDelete.size());

                Message[] trashMessages = trash.getMessages();
                trash.setFlags(trashMessages, new Flags(Flags.Flag.DELETED), true);
                trash.close(true);
                System.out.println("Lixeira esvaziada.");
            } else {
                System.out.println("Nenhum email encontrado para limpeza automática.");
            }

            allMail.close(true);
            store.close();
            System.out.println("Limpeza automática concluída.");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
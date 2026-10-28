package clemerso.mailmanager;

import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EmailCleaner {

    private static final String ALL_MAIL = "[Gmail]/Todos os e-mails";
    private static final String TRASH = "[Gmail]/Lixeira";
    private static final String SPAM = "[Gmail]/Spam";

    private static final int FETCH_BATCH = 500; // leitura de cabeçalhos
    private static final int OP_BATCH = 100;    // copiar / marcar (evita "muitas operações")

    private final MailConnection connection;
    private final Scanner scanner;

    public EmailCleaner(String email, String password, Scanner scanner) {
        this.connection = new MailConnection(email, password);
        this.scanner = scanner;
    }

    // ---------------------------------------------------------------- opção 3

    public void topSenders() {
        Store store = null;
        Folder allMail = null;

        try {
            store = connection.connect();
            allMail = store.getFolder(ALL_MAIL);
            allMail.open(Folder.READ_WRITE);

            int total = allMail.getMessageCount();
            Map<String, List<Message>> bySender = new HashMap<>();

            System.out.println("Analisando " + total + " emails...");

            for (int i = 1; i <= total; i += FETCH_BATCH) {
                int end = Math.min(i + FETCH_BATCH - 1, total);

                for (Message message : fetchBatch(allMail, i, end)) {
                    String address = addressOf(message);
                    if (address != null) {
                        bySender.computeIfAbsent(address, k -> new ArrayList<>()).add(message);
                    }
                }

                System.out.println("Processados: " + end + "/" + total);
            }

            List<Map.Entry<String, List<Message>>> top5 = bySender.entrySet().stream()
                    .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                    .limit(5)
                    .collect(Collectors.toList());

            System.out.println("\nTop 5 remetentes:");
            for (int i = 0; i < top5.size(); i++) {
                System.out.println((i + 1) + ". " + top5.get(i).getKey()
                        + " - " + top5.get(i).getValue().size() + " emails");
            }

            Set<Integer> chosen = new LinkedHashSet<>();
            while (true) {
                System.out.print("\nDigite o número para deletar (0 para terminar): ");
                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Número inválido.");
                    continue;
                }

                if (choice == 0) break;
                if (choice < 1 || choice > top5.size()) {
                    System.out.println("Fora da lista.");
                    continue;
                }
                if (chosen.add(choice)) {
                    System.out.println("Adicionado: " + top5.get(choice - 1).getKey());
                }
            }

            if (chosen.isEmpty()) {
                System.out.println("Operação cancelada.");
                return;
            }

            List<Message> toDelete = new ArrayList<>();
            System.out.println("\nRemetentes selecionados:");
            for (int c : chosen) {
                Map.Entry<String, List<Message>> entry = top5.get(c - 1);
                System.out.println(" - " + entry.getKey() + " (" + entry.getValue().size() + ")");
                toDelete.addAll(entry.getValue());
            }

            if (confirm("\nConfirma exclusão de " + toDelete.size() + " emails?")) {
                moveToTrashAndPurge(store, allMail, toDelete);
            } else {
                System.out.println("Operação cancelada.");
            }

        } catch (MessagingException e) {
            System.err.println("Erro: " + e.getMessage());
        } finally {
            closeQuietly(allMail, false);
            closeQuietly(store);
        }
    }

    // ---------------------------------------------------------------- opção 2

    public void cleanByKeywords(List<String> keywords) {
        scanAndClean(
                combined -> keywords.stream().anyMatch(combined::contains),
                "Nenhum email encontrado com essas palavras-chave."
        );
    }

    // ---------------------------------------------------------------- opção 4

    public void cleanByBlacklist() {
        System.out.println("Iniciando limpeza por blacklist...");
        scanAndClean(combined -> {
            boolean blacklisted = BlackList.SENDERS.stream()
                    .anyMatch(s -> combined.contains(s.toLowerCase()));
            boolean protectedSender = BlackList.WHITELIST.stream()
                    .anyMatch(w -> combined.contains(w.toLowerCase()));
            return blacklisted && !protectedSender;
        }, "Nenhum email encontrado para limpeza.");
    }

    // ---------------------------------------------------------------- opção 1

    public void cleanSpam() {
        Store store = null;
        Folder folder = null;

        try {
            store = connection.connect();
            folder = store.getFolder(SPAM);
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();

            for (int i = 0; i < messages.length; i += OP_BATCH) {
                int end = Math.min(i + OP_BATCH, messages.length);
                folder.setFlags(Arrays.copyOfRange(messages, i, end), new Flags(Flags.Flag.DELETED), true);
                pause();
                System.out.println("Processados: " + end + "/" + messages.length);
            }

            folder.close(true);
            System.out.println("Total deletado: " + messages.length);

        } catch (MessagingException e) {
            System.err.println("Erro: " + e.getMessage());
        } finally {
            closeQuietly(folder, false);
            closeQuietly(store);
        }
    }

    // ---------------------------------------------------------------- comum

    // varre "Todos os e-mails", mostra resumo por remetente e pede confirmação
    private void scanAndClean(Predicate<String> matches, String emptyMessage) {
        Store store = null;
        Folder allMail = null;

        try {
            store = connection.connect();
            allMail = store.getFolder(ALL_MAIL);
            allMail.open(Folder.READ_WRITE);

            int total = allMail.getMessageCount();
            List<Message> toDelete = new ArrayList<>();
            Map<String, Integer> resumo = new TreeMap<>();

            for (int i = 1; i <= total; i += FETCH_BATCH) {
                int end = Math.min(i + FETCH_BATCH - 1, total);

                for (Message message : fetchBatch(allMail, i, end)) {
                    String sender = senderOf(message);
                    if (sender != null && matches.test(sender)) {
                        toDelete.add(message);
                        resumo.merge(sender, 1, Integer::sum);
                    }
                }

                System.out.println("Verificados: " + end + "/" + total);
            }

            if (toDelete.isEmpty()) {
                System.out.println(emptyMessage);
                return;
            }

            System.out.println("\nRemetentes que serão apagados:");
            resumo.forEach((s, n) -> System.out.println(" - " + s + " (" + n + ")"));

            if (confirm("\nConfirma exclusão de " + toDelete.size() + " emails?")) {
                moveToTrashAndPurge(store, allMail, toDelete);
            } else {
                System.out.println("Operação cancelada.");
            }

        } catch (MessagingException e) {
            System.err.println("Erro: " + e.getMessage());
        } finally {
            closeQuietly(allMail, false);
            closeQuietly(store);
        }
    }

    // copia pra lixeira em lotes e apaga SÓ o que acabou de chegar lá
    private void moveToTrashAndPurge(Store store, Folder allMail, List<Message> toDelete)
            throws MessagingException {

        Folder trash = store.getFolder(TRASH);
        trash.open(Folder.READ_WRITE);

        try {
            int before = trash.getMessageCount();
            Message[] all = toDelete.toArray(new Message[0]);

            for (int i = 0; i < all.length; i += OP_BATCH) {
                allMail.copyMessages(Arrays.copyOfRange(all, i, Math.min(i + OP_BATCH, all.length)), trash);
                pause();
            }
            System.out.println("Movidos para lixeira: " + all.length);

            // reabre pra enxergar as mensagens novas
            trash.close(false);
            trash.open(Folder.READ_WRITE);

            int after = trash.getMessageCount();
            if (after > before) {
                Message[] moved = trash.getMessages(before + 1, after);
                for (int i = 0; i < moved.length; i += OP_BATCH) {
                    trash.setFlags(Arrays.copyOfRange(moved, i, Math.min(i + OP_BATCH, moved.length)),
                            new Flags(Flags.Flag.DELETED), true);
                    pause();
                }
            }

            trash.close(true);
            System.out.println("Apagados definitivamente.");

        } finally {
            closeQuietly(trash, false);
        }
    }

    // busca o lote inteiro de cabeçalhos de uma vez (bem mais rápido que um por um)
    private Message[] fetchBatch(Folder folder, int start, int end) throws MessagingException {
        Message[] messages = folder.getMessages(start, end);
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, fp);
        return messages;
    }

    // "endereco nome", em minúsculas
    private String senderOf(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) return null;

        if (from[0] instanceof InternetAddress addr) {
            String address = addr.getAddress() != null ? addr.getAddress() : "";
            String personal = addr.getPersonal() != null ? addr.getPersonal() : "";
            return (address + " " + personal).toLowerCase().trim();
        }
        return from[0].toString().toLowerCase();
    }

    // só o endereço, em minúsculas
    private String addressOf(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) return null;

        if (from[0] instanceof InternetAddress addr && addr.getAddress() != null) {
            return addr.getAddress().toLowerCase();
        }
        return from[0].toString().toLowerCase();
    }

    private boolean confirm(String question) {
        System.out.print(question + " (s/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("s");
    }

    private void pause() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeQuietly(Folder folder, boolean expunge) {
        try {
            if (folder != null && folder.isOpen()) folder.close(expunge);
        } catch (MessagingException ignored) {
        }
    }

    private void closeQuietly(Store store) {
        try {
            if (store != null && store.isConnected()) store.close();
        } catch (MessagingException ignored) {
        }
    }
}
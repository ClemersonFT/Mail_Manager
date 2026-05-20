package clemerso.mailmanager;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailCleaner {

    private final String email;
    private final String password;
    private final List<String> remetentes = new ArrayList<>();

    public void addRemetentes(List<String> senders) {
        this.remetentes.addAll(senders);
    }

    public EmailCleaner(
            String email,
            String password
    ) {

        this.email = email;
        this.password = password;
    }

    public void cleanStores() {

        try {

            Properties properties = new Properties();

            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(properties);

            session.setDebug(true);

            Store store = session.getStore("imaps");

            store.connect(
                    "imap.gmail.com",
                    email,
                    password
            );

            Folder folder = store.getFolder("[Gmail]/Todos os e-mails");
            folder.open(Folder.READ_WRITE);

            Message [] messages =
                    folder.getMessages();

            int deletedCount = 0;
            for (Message message : messages) {
                Address[] fromAddresses = message.getFrom();
                if (fromAddresses == null || fromAddresses.length == 0) continue;
                InternetAddress internetAddress = (InternetAddress) fromAddresses[0];

                String senderEmail = internetAddress.getAddress();
                String senderName = internetAddress.getPersonal();
                String combined = (
                        senderEmail + " " +
                                (senderName != null ? senderName : "")
                ).toLowerCase();

                if (remetentes.stream().anyMatch(combined::contains)) {
                    message.setFlag(Flags.Flag.DELETED, true);
                    deletedCount++;
                }
            }

            folder.close(true);
            store.close();
            System.out.println("Total deletado: " + deletedCount);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void cleanSpam(){

        try {
            Properties properties = new Properties();

            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(properties);

            session.setDebug(true);

            Store store = session.getStore("imaps");

            store.connect(
                    "imap.gmail.com",
                    email,
                    password
            );

            Folder folder = store.getFolder("[Gmail]/Spam");
            folder.open(Folder.READ_WRITE);

            Message [] messages =
                    folder.getMessages();

            int deletedCount = 0;
            for (Message message : messages) {
                message.setFlag(
                        Flags.Flag.DELETED,
                        true
                );
                deletedCount++;
            }

            folder.close(true);
            store.close();
            System.out.println(
                    "Total deletado: "
                            + deletedCount
            );
        } catch (MessagingException e){
            e.printStackTrace();
        }
    }
}

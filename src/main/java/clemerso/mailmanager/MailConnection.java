package clemerso.mailmanager;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import java.util.Properties;

public class MailConnection {
    private final String email;
    private final String password;

    public MailConnection(String email, String password) {
        this.email = email.trim();
        this.password = password.replaceAll("\\s+", "");
    }

    public Store connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.imaps.timeout", "60000");
        props.put("mail.imaps.connectiontimeout", "60000");
        props.put("mail.imaps.ssl.checkserveridentity", "true");

        Store store = Session.getInstance(props).getStore("imaps");
        store.connect("imap.gmail.com", email, password);
        return store;
    }
}
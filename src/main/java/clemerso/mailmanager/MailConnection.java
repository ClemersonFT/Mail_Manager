package clemerso.mailmanager;

import jakarta.mail.*;
import java.util.Properties;

public class MailConnection {
    private final String email;
    private final String password;

    public MailConnection(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Store connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", "imap.gmail.com");
        props.put("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", "true");

        Store store = Session.getInstance(props).getStore("imaps");
        store.connect("imap.gmail.com", email, password);
        return store;
    }
}
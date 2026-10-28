package clemerso.mailmanager;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Console;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Scanner;

public class Config {

    private static final Path DIR = Path.of(System.getProperty("user.home"), ".mailmanager");
    private static final Path ENC_FILE = DIR.resolve("credentials.enc");

    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String email;
    private final String password;

    public Config(Scanner scanner) {
        String data;
        try {
            data = load(scanner);
        } catch (AEADBadTagException e) {
            throw new RuntimeException("Senha mestra incorreta.");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar credenciais: " + e.getMessage(), e);
        }

        int nl = data.indexOf('\n');
        this.email = data.substring(0, nl);
        this.password = data.substring(nl + 1);
    }

    private static String load(Scanner scanner) throws Exception {
        if (!Files.exists(ENC_FILE)) {
            // primeira vez - pede tudo
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            String password = readSecret(scanner, "Senha de app: ").replaceAll("\\s+", "");
            String master = readSecret(scanner, "Crie uma senha mestra (mínimo 8 caracteres): ");

            if (master.length() < 8) {
                throw new IllegalArgumentException("Senha mestra muito curta.");
            }

            String data = email + "\n" + password;
            Files.createDirectories(DIR);
            Files.write(ENC_FILE, encrypt(data, master));
            System.out.println("Credenciais salvas com segurança!");
            return data;
        }

        String master = readSecret(scanner, "Digite sua senha mestra: ");
        return decrypt(Files.readAllBytes(ENC_FILE), master);
    }

    // sem eco no terminal; na IDE cai pro Scanner
    private static String readSecret(Scanner scanner, String prompt) {
        Console console = System.console();
        if (console != null) {
            return new String(console.readPassword("%s", prompt)).trim();
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // arquivo = salt (16) + iv (12) + dados cifrados (GCM)
    private static byte[] encrypt(String data, String master) throws Exception {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(master, salt), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return ByteBuffer.allocate(SALT_LEN + IV_LEN + encrypted.length)
                .put(salt).put(iv).put(encrypted)
                .array();
    }

    private static String decrypt(byte[] file, String master) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(file);
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        buf.get(salt);
        buf.get(iv);
        byte[] encrypted = new byte[buf.remaining()];
        buf.get(encrypted);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(master, salt), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String master, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(master.toCharArray(), salt, ITERATIONS, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
package clemerso.mailmanager;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

public class Config {

    private final Properties props = new Properties();
    private static final File ENC_FILE = new File("config/credentials.enc");
    private static final File PIN_FILE = new File("config/pin.enc");

    public Config() {
        this("auto");
    }

    public Config(String modo) {
        try {
            Scanner scanner = new Scanner(System.in);

            if (!ENC_FILE.exists()) {
                // primeira vez - pede tudo
                System.out.print("Email: ");
                String email = scanner.nextLine().trim();

                System.out.print("Senha de app: ");
                String password = scanner.nextLine().trim();

                System.out.print("Crie um PIN (mínimo 4 dígitos): ");
                String pin = scanner.nextLine().trim();

                String data = "email=" + email + "\npassword=" + password;

                ENC_FILE.getParentFile().mkdirs();
                Files.write(ENC_FILE.toPath(), encrypt(data, pin));
                Files.write(PIN_FILE.toPath(), encrypt(pin, getMachineKey()));

                props.load(new ByteArrayInputStream(data.getBytes()));
                System.out.println("Credenciais salvas com segurança!");

            } else if (modo.equals("auto")) {
                byte[] encPin = Files.readAllBytes(PIN_FILE.toPath());
                String pin = decrypt(encPin, getMachineKey());

                byte[] encrypted = Files.readAllBytes(ENC_FILE.toPath());
                props.load(new ByteArrayInputStream(decrypt(encrypted, pin).getBytes()));

            } else {
                System.out.print("Digite seu PIN: ");
                String pin = scanner.nextLine().trim();

                byte[] encrypted = Files.readAllBytes(ENC_FILE.toPath());
                props.load(new ByteArrayInputStream(decrypt(encrypted, pin).getBytes()));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar credenciais: " + e.getMessage());
        }
    }

    private byte[] encrypt(String data, String pin) throws Exception {
        SecretKey key = deriveKey(pin);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(data.getBytes());
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    private String decrypt(byte[] data, String pin) throws Exception {
        SecretKey key = deriveKey(pin);
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] encrypted = Arrays.copyOfRange(data, 16, data.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted));
    }

    private SecretKey deriveKey(String pin) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(pin.toCharArray(), "mailmanager_salt".getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private String getMachineKey() {
        return System.getProperty("user.name") + System.getenv("COMPUTERNAME");
    }

    public String getEmail() {
        return props.getProperty("email");
    }

    public String getPassword() {
        return props.getProperty("password");
    }
}
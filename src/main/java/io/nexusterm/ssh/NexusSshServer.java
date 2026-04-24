package io.nexusterm.ssh;

import io.nexusterm.shell.SessionManager;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

public class NexusSshServer {
    private static final Logger logger = LoggerFactory.getLogger(NexusSshServer.class);
    private final int port;
    private SshServer sshd;
    private final SessionManager sessionManager = new SessionManager();

    public NexusSshServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        
        // Host key provider (generates a key if it doesn't exist)
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));

        // Simple Password Authentication (for demo purposes)
        // User: admin, Password: password
        sshd.setPasswordAuthenticator((username, password, session) -> 
            "admin".equals(username) && "password".equals(password)
        );

        // Set our custom Shell Factory
        sshd.setShellFactory(new NexusShellFactory(sessionManager));

        sshd.start();
        logger.info("NexusTerm SSH Server started on port {}", port);
    }

    public void stop() throws IOException {
        if (sshd != null) {
            sshd.stop();
        }
    }

    public static void main(String[] args) throws IOException {
        NexusSshServer server = new NexusSshServer(2222);
        server.start();
        
        // Keep main thread alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

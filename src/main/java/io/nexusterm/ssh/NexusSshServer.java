package io.nexusterm.ssh;

import io.nexusterm.shell.SessionManager;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.security.Security;

public class NexusSshServer {
    private static final Logger logger = LoggerFactory.getLogger(NexusSshServer.class);
    private static final List<String> PREFERRED_KEX = List.of(
            "mlkem768x25519-sha256",
            "sntrup761x25519-sha512",
            "sntrup761x25519-sha512@openssh.com",
            "curve25519-sha256",
            "curve25519-sha256@libssh.org"
    );
    private final int port;
    private final String username;
    private final String password;
    private SshServer sshd;
    private final SessionManager sessionManager = new SessionManager();

    public NexusSshServer(int port) {
        this(port, "admin", "password");
    }

    public NexusSshServer(int port, String username, String password) {
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void start() throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        registerSecurityProviders();
        
        // Host key provider (generates a key if it doesn't exist)
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));

        // Keep auth focused on passwords so OpenSSH does not try public-key flows
        // that this demo server is not configured to support.
        sshd.setUserAuthFactories(List.of(UserAuthPasswordFactory.INSTANCE));

        sshd.setPasswordAuthenticator((candidateUsername, candidatePassword, session) ->
            username.equals(candidateUsername) && password.equals(candidatePassword)
        );
        configureKeyExchangeFactories();

        // Set our custom Shell Factory
        sshd.setShellFactory(new NexusShellFactory(sessionManager));

        sshd.start();
        logger.info("NexusTerm SSH Server started on port {} for user {}", port, username);
    }

    public void stop() throws IOException {
        if (sshd != null) {
            sshd.stop();
        }
    }

    private void registerSecurityProviders() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private void configureKeyExchangeFactories() {
        List<String> existing = sshd.getKeyExchangeFactories().stream()
                .map(KeyExchangeFactory::getName)
                .collect(Collectors.toCollection(ArrayList::new));

        LinkedHashSet<String> orderedNames = new LinkedHashSet<>();
        orderedNames.addAll(PREFERRED_KEX);
        orderedNames.addAll(existing);

        BuiltinDHFactories.ParseResult parsed = BuiltinDHFactories.parseDHFactoriesList(orderedNames);
        if (!parsed.getParsedFactories().isEmpty()) {
            List<KeyExchangeFactory> factories = NamedFactory.setUpTransformedFactories(
                    true,
                    parsed.getParsedFactories(),
                    ServerBuilder.DH2KEX
            );
            sshd.setKeyExchangeFactories(factories);
        }
        if (!parsed.getUnsupportedFactories().isEmpty()) {
            logger.info("Unavailable KEX factories skipped: {}", parsed.getUnsupportedFactories());
        }
    }

    public static void main(String[] args) throws IOException {
        NexusServerConfig config = NexusServerConfig.load();
        NexusSshServer server = new NexusSshServer(config.port(), config.username(), config.password());
        server.start();
        
        // Keep main thread alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

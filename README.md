# NexusTerm: Custom SSH Server & Structured Shell

NexusTerm is a modern, opinionated SSH server and shell environment built for the 2025 era. It deviates from traditional POSIX shells by providing a typed, asynchronous, and collaborative experience directly over the SSH protocol.

## Key Features

### 1. Structured & Typed Shell
NexusTerm commands don't just return strings; they return **typed Java objects**. This allows for "PowerShell-style" pipelines where data can be filtered and manipulated semantically.
*   `ls | where size > 1024`: Filters file objects based on their properties using reflection.
*   The shell understands the structure of the data passing through the pipeline, eliminating the need for `awk`, `sed`, or complex `grep` regexes.

### 2. First-Class Asynchronicity
Every pipeline execution is treated as a first-class asynchronous job.
*   **Non-blocking:** Run long-running tasks in the background using the `&` operator (e.g., `command &`).
*   **Job Management:** Monitor and control tasks with `jobs` and `await @<id>` commands.

### 3. Live JVM Instrumentation (`spy`)
The most powerful tool in NexusTerm is the `spy` command.
*   **Dynamic Attachment:** Attach to any running JVM process by PID.
*   **Bytecode Injection:** Injects a custom Java Agent (using **ASM**) that instrument classes on the fly to stream live method calls directly into your shell session.

### 4. Time-Travel History
NexusTerm keeps a complete history of the shell's **environment state** for every command.
*   `rewind <steps>`: Reverts the entire session state (Environment Variables, CWD) to a previous point in history.
*   It's like `git` for your terminal session, allowing you to undo environment changes instantly.

### 5. Multi-User Collaboration
Built-in session sharing inspired by `tmux`, but integrated directly into the shell protocol.
*   `collab list`: See all active SSH sessions.
*   `collab join <sessionId>`: "Follow" another user's session and see their command output live with distinct user coloring.

## Technology Stack
*   **Java 25**
*   **Apache MINA SSHD:** High-performance SSH protocol implementation.
*   **JLine 3:** Advanced terminal handling and PTY support.
*   **ASM 9:** Low-level bytecode manipulation for the `spy` agent.
*   **Jackson:** Structured data serialization.

## How to Run
```bash
# Build the project
mvn clean compile

# Run the SSH server (default port 2222)
# Login with: admin / password
mvn exec:java -Dexec.mainClass="io.nexusterm.ssh.NexusSshServer"
```

You can override the demo login and port without editing code:

```bash
mvn exec:java \
  -Dexec.mainClass="io.nexusterm.ssh.NexusSshServer" \
  -Dnexus.term.user=admin \
  -Dnexus.term.password=password \
  -Dnexus.term.port=2222
```

Then connect with:

```bash
ssh -p 2222 admin@localhost
```

Notes:
* The shell now falls back to a stream-safe dumb terminal when native JLine providers are unavailable, so SSH login should no longer disconnect immediately after successful authentication.
* NexusTerm intentionally advertises password authentication only, which avoids the noisy public-key fallback warnings from clients offering Ed25519 keys.
* If OpenSSH prints a post-quantum key-exchange warning, that is about the negotiated SSH KEX algorithm, not the password login path or shell startup.

## Security Note
This project is a demonstration of advanced Java concepts. The provided password authentication and agent attachment capabilities are intended for educational and local diagnostic purposes.

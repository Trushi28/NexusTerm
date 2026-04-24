# NexusTerm: Modern SSH Server with Typed Shell

NexusTerm is a high-performance SSH server and interactive shell environment built for 2025. It breaks from traditional POSIX shells by delivering a **typed, asynchronous, collaborative** terminal directly over SSH.

Imagine PowerShell's object pipelines, tmux's session sharing, and a JVM debugger built directly into your shell—that's NexusTerm.

## What Makes It Different?

### 1. **Typed Object Pipelines** (Not Just Text)

Traditional shells pipe text. NexusTerm pipes **Java objects**:

```bash
# Old way (POSIX)
ls -l | grep "*.txt" | awk '{print $9}' | xargs wc -l

# NexusTerm way (Typed)
ls | where size > 1024 | map name
```

- The shell **understands** that `ls` returns `FileObject[]` with `.size`, `.name`, `.modified` properties
- You filter and transform semantically using reflection, not string parsing
- No more `awk`, `sed`, `cut` for every operation—just property access

### 2. **First-Class Async Jobs**

Every command is an async job:

```bash
# Run a slow task in the background
slow-database-query &

# Check on it later
jobs

# Wait for a specific job to complete
await @1

# All without blocking your shell
```

Non-blocking execution means you can spawn 100 jobs and monitor them all in real-time.

### 3. **Live JVM Instrumentation (`spy` Command)**

The most powerful feature: attach to any running Java process and see its method calls in real-time:

```bash
# List running JVM processes
jps

# Attach to PID 12345 and spy on MyClass.process()
spy attach 12345
spy trace MyClass.process

# Watch every invocation stream into your terminal
```

Under the hood, NexusTerm injects a Java Agent using **ASM bytecode rewriting**—no restart required, zero overhead until you enable tracing.

### 4. **Time-Travel Session History**

Every shell command gets a snapshot of your environment:

```bash
# You've run 50 commands and messed up your environment variables
export DATABASE_URL=wrong_value

# Oops! Rewind to 5 commands ago
rewind 5

# Your DATABASE_URL is restored, your CWD is restored, everything is restored
# It's like git for your shell session
```

Great for debugging, experimentation, and undo functionality most shells don't have.

### 5. **Multi-User Collaboration**

Share your session with teammates:

```bash
# Start a session
collab list

# Teammate joins your session with distinct coloring
collab join session-abc

# Now you both see the same output, typed from either terminal
# Like tmux, but integrated into the protocol
```

Perfect for pair debugging, onboarding, and live troubleshooting.

## Technology Stack

- **Java 25**: Modern language features, virtual threads, etc.
- **Apache MINA SSHD 2.17+**: Battle-tested SSH server library
- **JLine 3**: Advanced terminal handling, TAB completion, line editing
- **Jackson**: Structured data serialization
- **ASM 9**: Bytecode manipulation for the `spy` agent
- **SLF4J + Logback**: Production logging

## Getting Started

### Prerequisites
- Java 25+
- Maven 3.8+

### Build the Project

```bash
mvn clean compile
```

### Run the SSH Server

```bash
# Start with defaults: admin/password on port 2222
mvn exec:java -Dexec.mainClass="io.nexusterm.ssh.NexusSshServer"
```

### Connect from Client

```bash
# Default credentials
ssh -p 2222 admin@localhost

# Or with custom credentials/port:
mvn exec:java \
  -Dexec.mainClass="io.nexusterm.ssh.NexusSshServer" \
  -Dnexus.term.user=myuser \
  -Dnexus.term.password=mypassword \
  -Dnexus.term.port=2222

ssh -p 2222 myuser@localhost
```

### Interactive Shell Features

Once connected:

```bash
# List files with typed filtering
ls | where size > 1024

# Run commands async
sleep 100 &

# Check async jobs
jobs

# Start monitoring a JVM
spy attach <pid>

# Rewind session state
rewind 3

# Start collaboration
collab join <session-id>
```

## Architecture

```
┌─────────────────────────────────────────┐
│  SSH Client (OpenSSH, Putty, etc.)     │
└──────────────────┬──────────────────────┘
                   │ SSH Protocol
                   v
┌──────────────────────────────────────────┐
│  Apache MINA SSHD                        │
│  (Key Exchange, Auth, Channel Mgmt)      │
└──────────────────┬───────────────────────┘
                   │
       ┌───────────┼────────────┐
       v           v            v
    ┌────────┐ ┌────────┐ ┌──────────┐
    │ Shell  │ │ PTY    │ │ Collab   │
    │ REPL   │ │Handler │ │ Manager  │
    └────┬───┘ └────────┘ └──────────┘
         │
         v
    ┌──────────────────────┐
    │ NexusShell           │
    │ - CommandRegistry    │
    │ - PipelineExecutor   │
    │ - TypedObjectSystem  │
    └──────────┬───────────┘
               │
       ┌───────┴────────┬──────────────┐
       v                v              v
   ┌─────────┐   ┌─────────────┐  ┌────────┐
   │ JobMgr  │   │ SessionState│  │ SpyAgent│
   │ (Async) │   │ (History)   │  │(Bytecode)
   └─────────┘   └─────────────┘  └────────┘
```

## Key Commands

| Command | Purpose |
|---------|---------|
| `ls` | List files (returns typed objects, not strings) |
| `where <condition>` | Filter objects by property |
| `map <property>` | Extract a property from objects |
| `&` | Run command in background |
| `jobs` | List running async jobs |
| `await @<id>` | Wait for a specific job |
| `spy attach <pid>` | Attach to a running JVM |
| `spy trace <class>` | Instrument a class and show method calls |
| `rewind <steps>` | Undo environment changes |
| `collab list` | List active sessions |
| `collab join <id>` | Join another user's session |

## Advanced Usage

### Example: Debugging a Slow Java Service

```bash
# Start monitoring the service
spy attach 1234

# Trace the main bottleneck
spy trace com.example.UserService.fetchUser

# Watch invocations stream in (timestamps, args, return values, exceptions)

# Filter to slow calls
spy filter "duration > 100ms"
```

### Example: Interactive Data Transformation

```bash
# Get all large files modified in the last day
ls \
  | where size > 1000000 \
  | where modified > "24h ago" \
  | map name
```

### Example: Pair Debugging Session

```bash
# User A: Share your session
collab list
# Output: session-abc (active)

# User B: Join the session
collab join session-abc

# Now both see the same terminal, can type independently
# User A and User B colored separately
```

## Design Decisions

### Why Typed Objects Over Strings?

Text pipelines (`ls | grep | awk`) work but are brittle:
- Column alignment changes break parsing
- Encoding issues cause silent failures
- No type safety or IDE support

Typed objects are:
- Semantic (you filter on actual properties, not regex)
- Composable (chain operations without string conversion)
- Debuggable (objects have structure, IDEs can help)

### Why Async Jobs Are Built-In?

Most shells (bash, zsh) treat backgrounding as an afterthought:
- Jobs exit mysteriously if you close the terminal
- No unified interface to track many jobs
- Pipelines block by default

NexusTerm treats async as first-class:
- Every command is a `Job<T>` internally
- Background jobs are persistent (survive SSH reconnect)
- Unified `jobs` / `await` / `kill` interface

### Why the `spy` Command?

Traditional debugging requires:
1. Kill the process
2. Restart with a debugger attached
3. Re-run the failing scenario

The `spy` command:
- Attaches without restart
- Zero overhead until you enable tracing
- Works with any JVM process
- Perfect for production troubleshooting

## Limitations & Scope

This is an **educational showcase** of advanced Java and distributed systems concepts:

- Single-machine SSH server (not a load-balanced fleet)
- Limited to Java introspection (can't spy on native code)
- Simplified SQL parsing (no complex WHERE clauses yet)
- Not intended for production use without hardening

That said, the architecture patterns here are battle-tested in real systems like JetBrains IDEs and production monitoring tools.

## Security Note

⚠️ **This is a demonstration project for educational and diagnostic purposes.**

- Password authentication is intentionally basic (no PKI support yet)
- Agent attachment can execute arbitrary code (obviously risky)
- Do **not** expose this on a public network
- Use only in trusted, local environments

## Project Structure

```
src/main/java/io/nexusterm/
├── ssh/
│   ├── NexusSshServer.java     # Main entry point
│   ├── NexusServerConfig.java  # SSH configuration
│   ├── NexusShellFactory.java  # Shell session factory
│   └── ...
├── shell/
│   ├── NexusShell.java         # Main REPL loop
│   ├── CommandRegistry.java    # Built-in commands
│   ├── PipelineExecutor.java   # Typed data pipeline
│   ├── JobManager.java         # Async job tracking
│   ├── SessionState.java       # Environment/history
│   └── NexusCommand.java       # Command interface
├── agent/
│   ├── SpyAgent.java           # JVM agent entry point
│   ├── BytecodeInstrumentor.java
│   └── MethodTracer.java
└── Main.java
```

## Further Reading

- [Apache MINA SSHD](https://mina.apache.org/sshd-project/): The SSH library powering NexusTerm
- [JLine 3](https://github.com/jline/jline3): Terminal handling and readline
- [ASM Bytecode Engineering](https://asm.ow2.io/): Bytecode instrumentation
- [Java Instrumentation API](https://docs.oracle.com/javase/8/docs/technotes/guides/instrumentation/): Agent attachments

## License

Educational and demonstration purposes.

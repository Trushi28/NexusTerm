package io.nexusterm.ssh;

import io.nexusterm.shell.NexusShell;
import io.nexusterm.shell.SessionManager;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

public class NexusShellFactory implements ShellFactory {
    private final SessionManager sessionManager;

    public NexusShellFactory(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Command createShell(ChannelSession channel) {
        return new NexusShell(sessionManager, channel.getSession().getIoSession().getRemoteAddress().toString());
    }
}

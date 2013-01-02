
In Mina, req-pty server-side handling is in ChannelSession.handlePtyReq.
This parses the command and updates Environment, but doesn't actually allocate a terminal.

Similarly, ChannelSession.handleWindowChange is needed to handle the client window size change.

also, handleSignal.

then handleShell or handleExec.

allocatign tty means all the output comes to stdout and not on stderr
(with ssh)

ChannelSession doesn't notify in case of handleWindowChange, so we need to add a subtype
and insert it like this:

        sshd.setChannelFactories(Arrays.<NamedFactory<Channel>>asList(
                new ChannelSession.Factory(),
                new ChannelDirectTcpip.Factory()));

Command can implement SessionAware to receive a Session object

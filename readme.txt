
In Mina, req-pty server-side handling is in ChannelSession.handlePtyReq.
This parses the command and updates Environment, but doesn't actually allocate a terminal.

Similarly, ChannelSession.handleWindowChange is needed to handle the client window size change.

also, handleSignal.

then handleShell or handleExec.


thus if we use python wrapper, we need a mechanism to communicate commands.

allocatign tty means all the output comes to stdout and not on stderr
(with ssh)
package io.github.hylexus.jt808.dispatcher.impl;

import io.github.hylexus.jt.command.CommandWaitingPool;
import io.github.hylexus.jt.command.Jt808CommandKey;
import io.github.hylexus.jt.data.msg.MsgType;
import io.github.hylexus.jt.exception.JtSessionNotFoundException;
import io.github.hylexus.jt808.dispatcher.CommandSender;
import io.github.hylexus.jt808.msg.resp.CommandMsg;
import io.github.hylexus.jt808.session.Session;
import io.github.hylexus.jt808.session.SessionManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author hylexus
 * Created At 2020-03-11 7:30 下午
 */
public abstract class AbstractCommandSender implements CommandSender {

    private final CommandWaitingPool commandWaitingPool = CommandWaitingPool.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public Object sendCommandAndWaitingForReply(CommandMsg commandMsg, boolean withFlowId, Long timeout, TimeUnit unit)
            throws IOException, InterruptedException {

        final String terminalId = commandMsg.getTerminalId();
        final MsgType msgType = commandMsg.getExpectedReplyMsgType();

        final Session session = getSession(terminalId);
        final int flowId = session.getCurrentFlowId();

        final byte[] bytes = this.encode(commandMsg, terminalId, flowId);
        final Jt808CommandKey commandKey = withFlowId
                ? Jt808CommandKey.of(msgType, terminalId, flowId)
                : Jt808CommandKey.of(msgType, terminalId);

        this.sendCommand(terminalId, bytes, timeout, unit);
        return commandWaitingPool.waitingForKey(commandKey, timeout, unit);
    }

    @Override
    public Object sendCommandAndWaitingForReply(Jt808CommandKey key, byte[] data, long time, TimeUnit unit) throws InterruptedException {
        this.sendCommand(key.getTerminalId(), data, time, unit);
        return commandWaitingPool.waitingForKey(key, time, unit);
    }

    @Override
    public void sendCommand(String terminalId, byte[] data, long time, TimeUnit unit) throws InterruptedException {
        final Session session = getSession(terminalId);
        session.sendMsgToClient(data);
    }

    @Override
    public void sendCommand(CommandMsg commandMsg, long time, TimeUnit unit) throws InterruptedException, IOException {
        final String terminalId = commandMsg.getTerminalId();
        final Session session = getSession(terminalId);
        final byte[] bytes = this.encode(commandMsg, terminalId, session.getCurrentFlowId());
        session.sendMsgToClient(bytes);
    }

    private Session getSession(String terminalId) {
        return sessionManager.findByTerminalId(terminalId).orElseThrow(() -> new JtSessionNotFoundException(terminalId));
    }

    protected abstract byte[] encode(Object object, String terminalId, int flowId) throws IOException;
}

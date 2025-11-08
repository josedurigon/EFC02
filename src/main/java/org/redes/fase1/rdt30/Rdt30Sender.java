package org.redes.fase1.rdt30;

import org.redes.Utils.ChannelUnreliable;
import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Rdt30Sender {
    private final DatagramSocket socket;
    private final InetAddress receiverAddr;
    private final int receiverPort;
    private final ChannelUnreliable channel;

    private int seq = 0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> timer;
    private final long timeoutMs; // ex.: 2000ms

    private volatile Packet lastPacket;

    public Rdt30Sender(String host, int port, ChannelUnreliable channel, long timeoutMs) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddr = InetAddress.getByName(host);
        this.receiverPort = port;
        this.channel = channel;
        this.timeoutMs = timeoutMs;
    }

    public void send(String message) throws Exception {
        lastPacket = new Packet(Packet.PacketType.DATA, seq, message.getBytes(StandardCharsets.UTF_8));
        Logger.log("[RDT3.0/SENDER] Enviando seq=" + seq + " → " + message);
        channel.send(lastPacket, socket, receiverAddr, receiverPort);
        startTimer();

        while (true) {
            byte[] buf = new byte[4096];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            socket.receive(dp);

            byte[] exact = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, exact, 0, dp.getLength());

            Packet ack = Packet.fromBytes(exact);

            if (ack.getType() == Packet.PacketType.ACK && !ack.isCorrupted() && ack.getSeqNum() == seq) {
                stopTimer();
                Logger.log("[RDT3.0/SENDER] ACK seq=" + seq + " recebido → confirmado");
                seq = 1 - seq;
                return;
            } else {
                Logger.log("[RDT3.0/SENDER] ACK inválido/corrompido (esperava seq=" + seq + ")");
                // continua aguardando até timeout tratar retransmissão
            }
        }
    }

    private void startTimer() {
        stopTimer();
        timer = scheduler.schedule(this::onTimeout, timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void stopTimer() {
        if (timer != null) timer.cancel(false);
    }

    private void onTimeout() {
        try {
            Logger.log("[RDT3.0/SENDER] TIMEOUT → retransmitindo seq=" + lastPacket.getSeqNum());
            channel.send(lastPacket, socket, receiverAddr, receiverPort);
            startTimer();
        } catch (Exception e) {
            Logger.logError("Falha ao retransmitir no timeout", e);
        }
    }

    public void close() {
        stopTimer();
        scheduler.shutdownNow();
        socket.close();
    }
}

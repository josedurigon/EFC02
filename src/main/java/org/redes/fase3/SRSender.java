package org.redes.fase3;

import org.redes.Utils.ChannelUnreliable;
import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

public class SRSender {
    private final DatagramSocket socket;
    private final InetAddress receiverAddr;
    private final int receiverPort;
    private final ChannelUnreliable channel;

    private final int windowSize;
    private final long timeoutMs;

    private int base = 0;
    private int nextSeqNum = 0;

    private final Map<Integer, Packet> window = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private volatile boolean running = true;

    public SRSender(String host, int port, ChannelUnreliable channel, int windowSize, long timeoutMs) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddr = InetAddress.getByName(host);
        this.receiverPort = port;
        this.channel = channel;
        this.windowSize = windowSize;
        this.timeoutMs = timeoutMs;
    }

    public void startAckListener() {
        new Thread(() -> {
            try {
                while (running) {
                    byte[] buf = new byte[4096];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socket.receive(dp);
                    Packet ack = Packet.fromBytes(Arrays.copyOf(dp.getData(), dp.getLength()));
                    if (ack.getType() != Packet.PacketType.ACK || ack.isCorrupted()) continue;

                    int ackNum = ack.getSeqNum();
                    Logger.log("[SR/SENDER] ACK recebido: " + ackNum);

                    if (window.containsKey(ackNum)) {
                        stopTimer(ackNum);
                        window.remove(ackNum);
                        if (ackNum == base) {
                            while (!window.containsKey(base) && nextSeqNum > base)
                                base++;
                        }
                    }
                }
            } catch (Exception e) {
                if (running) Logger.logError("Erro no listener de ACKs", e);
            }
        }).start();
    }

    public void send(String message) throws Exception {
        while (nextSeqNum >= base + windowSize) Thread.sleep(10);

        Packet pkt = new Packet(Packet.PacketType.DATA, nextSeqNum, message.getBytes(StandardCharsets.UTF_8));
        window.put(nextSeqNum, pkt);
        sendPacket(pkt);
        startTimer(pkt.getSeqNum());
        nextSeqNum++;
    }

    private void sendPacket(Packet pkt) throws Exception {
        channel.send(pkt, socket, receiverAddr, receiverPort);
        Logger.log("[SR/SENDER] Enviado seq=" + pkt.getSeqNum());
    }

    private void startTimer(int seqNum) {
        stopTimer(seqNum);
        timers.put(seqNum, scheduler.schedule(() -> timeout(seqNum), timeoutMs, TimeUnit.MILLISECONDS));
    }

    private void stopTimer(int seqNum) {
        ScheduledFuture<?> t = timers.remove(seqNum);
        if (t != null) t.cancel(false);
    }

    private void timeout(int seqNum) {
        try {
            Packet pkt = window.get(seqNum);
            if (pkt != null) {
                Logger.log("[SR/SENDER] TIMEOUT → reenvio seq=" + seqNum);
                sendPacket(pkt);
                startTimer(seqNum);
            }
        } catch (Exception e) {
            Logger.logError("Falha ao retransmitir SR pacote", e);
        }
    }

    public void close() {
        running = false;
        timers.values().forEach(t -> t.cancel(false));
        scheduler.shutdownNow();
        socket.close();
    }
}

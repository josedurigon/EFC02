package org.redes.fase2;

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

public class GBNSender {
    private final DatagramSocket socket;
    private final InetAddress receiverAddr;
    private final int receiverPort;
    private final ChannelUnreliable channel;

    private final int windowSize;
    private final long timeoutMs;

    private int base = 0;
    private int nextSeqNum = 0;
    private final Map<Integer, Packet> window = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> timer;

    public GBNSender(String host, int port, ChannelUnreliable channel, int windowSize, long timeoutMs) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddr = InetAddress.getByName(host);
        this.receiverPort = port;
        this.channel = channel;
        this.windowSize = windowSize;
        this.timeoutMs = timeoutMs;
        this.base = 0;
        this.nextSeqNum = 0;
        this.timer = null;
    }

    public GBNSender(DatagramSocket socket, InetAddress receiverAddr, int receiverPort,
                     ChannelUnreliable channel, int windowSize,
                     long timeoutMs, int base,
                     int nextSeqNum,
                     ScheduledFuture<?> timer) {
        this.socket = socket;
        this.receiverAddr = receiverAddr;
        this.receiverPort = receiverPort;
        this.channel = channel;
        this.windowSize = windowSize;
        this.timeoutMs = timeoutMs;
        this.base = base;
        this.nextSeqNum = nextSeqNum;
        this.timer = timer;
    }

    public void send(String message) throws Exception {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        int seq = nextSeqNum;

        // Se a janela está cheia, bloqueia até liberar espaço
        while (nextSeqNum >= base + windowSize) {
            Thread.sleep(10);
        }

        Packet pkt = new Packet(Packet.PacketType.DATA, seq, data);
        window.put(seq, pkt);
        channel.send(pkt, socket, receiverAddr, receiverPort);
        Logger.log("[GBN/SENDER] Enviado seq=" + seq + " → " + message);

        if (base == nextSeqNum) startTimer();

        nextSeqNum++;
    }

    public void startAckListener() {
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buf = new byte[4096];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socket.receive(dp);

                    byte[] exact = Arrays.copyOf(dp.getData(), dp.getLength());
                    Packet ack = Packet.fromBytes(exact);

                    if (ack.getType() == Packet.PacketType.ACK && !ack.isCorrupted()) {
                        int ackNum = ack.getSeqNum();
                        Logger.log("[GBN/SENDER] ACK recebido: " + ackNum);

                        // move a base
                        if (ackNum >= base) {
                            base = ackNum + 1;
                            Logger.log("[GBN/SENDER] Base atualizada → " + base);
                            if (base == nextSeqNum) stopTimer();
                            else restartTimer();
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logError("Erro no listener de ACKs", e);
            }
        }).start();
    }

    private void startTimer() {
        stopTimer();
        timer = scheduler.schedule(this::timeout, timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void restartTimer() {
        stopTimer();
        startTimer();
    }

    private void stopTimer() {
        if (timer != null) timer.cancel(false);
    }

    private void timeout() {
        try {
            Logger.log("[GBN/SENDER] TIMEOUT → retransmitindo janela " + base + " até " + (nextSeqNum - 1));
            for (int i = base; i < nextSeqNum; i++) {
                Packet pkt = window.get(i);
                if (pkt != null) {
                    channel.send(pkt, socket, receiverAddr, receiverPort);
                    Logger.log("[GBN/SENDER] Reenvio seq=" + i);
                }
            }
            startTimer();
        } catch (Exception e) {
            Logger.logError("Falha ao retransmitir janela", e);
        }
    }

    public void close() {
        stopTimer();
        scheduler.shutdownNow();
        socket.close();
    }
}

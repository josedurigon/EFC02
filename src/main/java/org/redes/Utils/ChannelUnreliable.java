package org.redes.Utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChannelUnreliable {
    private final double lossRate;
    private final double corruptRate;
    private final double minDelay;
    private final double maxDelay;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ChannelUnreliable(double lossRate, double corruptRate, double minDelay, double maxDelay) {
        this.lossRate = lossRate;
        this.corruptRate = corruptRate;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
    }

    public void send(Packet packet, DatagramSocket socket, InetAddress dest, int port) {
        if (random.nextDouble() < lossRate) {
            Logger.log("[SIMULADOR] Pacote perdido: " + packet);
            return;
        }

        byte[] raw = packet.toBytes();

        if (random.nextDouble() < corruptRate) {
            raw = corrupt(raw);
            Logger.log("[SIMULADOR] Pacote corrompido: " + packet);
        }

        double delay = minDelay + (maxDelay - minDelay) * random.nextDouble();

        final byte[] rawFinal = raw;
        final InetAddress destFinal = dest;
        final int portFinal = port;
        final DatagramSocket socketFinal = socket;
        final double delayFinal = delay;

        scheduler.schedule(() -> {
            try {
                DatagramPacket dp = new DatagramPacket(rawFinal, rawFinal.length, destFinal, portFinal);
                socket.send(dp);
                Logger.log("[CANAL] Pacote enviado com atraso de " + String.format("%.3f", delay) + "s → " + dest.getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, (long) (delay * 1000), TimeUnit.MILLISECONDS);
    }

    private byte[] corrupt(byte[] data) {
        byte[] copy = data.clone();
        int idx = random.nextInt(copy.length);
        copy[idx] ^= 0xFF; // inverte bits
        return copy;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}

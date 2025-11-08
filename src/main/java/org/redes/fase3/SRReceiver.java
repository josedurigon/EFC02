package org.redes.fase3;

import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SRReceiver {
    private final DatagramSocket socket;
    private final int windowSize;
    private int expectedBase = 0;

    private final Map<Integer, byte[]> buffer = new TreeMap<>();

    public SRReceiver(int port, int windowSize) throws Exception {
        this.socket = new DatagramSocket(port);
        this.windowSize = windowSize;
        Logger.log("[SR/RECEIVER] Aguardando na porta " + port);
    }

    public void start() throws Exception {
        while (true) {
            byte[] buf = new byte[8192];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            socket.receive(dp);

            Packet pkt = Packet.fromBytes(Arrays.copyOf(dp.getData(), dp.getLength()));

            if (pkt.isCorrupted()) {
                Logger.log("[SR/RECEIVER] Pacote corrompido → ignorado");
                continue;
            }

            int seq = pkt.getSeqNum();
            if (seq < expectedBase || seq >= expectedBase + windowSize) {
                Logger.log("[SR/RECEIVER] Pacote fora da janela (" + seq + ") → ignorado");
                continue;
            }

            sendAck(dp.getAddress(), dp.getPort(), seq);
            if (!buffer.containsKey(seq)) buffer.put(seq, pkt.getData());

            while (buffer.containsKey(expectedBase)) {
                String data = new String(buffer.remove(expectedBase), StandardCharsets.UTF_8);
                Logger.log("[SR/RECEIVER] Entregue à aplicação: seq=" + expectedBase + " → " + data);
                expectedBase++;
            }
        }
    }

    private void sendAck(InetAddress addr, int port, int seq) throws Exception {
        Packet ack = new Packet(Packet.PacketType.ACK, seq, new byte[0]);
        DatagramPacket dp = new DatagramPacket(ack.toBytes(), ack.toBytes().length, addr, port);
        socket.send(dp);
        Logger.log("[SR/RECEIVER] ACK enviado: " + seq);
    }

    public void close() {
        socket.close();
    }
}

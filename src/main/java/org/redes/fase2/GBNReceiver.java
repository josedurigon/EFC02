package org.redes.fase2;

import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GBNReceiver {
    private final DatagramSocket socket;
    private int expectedSeqNum = 0;
    private int lastAck = -1;

    public GBNReceiver(DatagramSocket socket, int expectedSeqNum, int lastAck) {
        this.socket = socket;
        this.expectedSeqNum = expectedSeqNum;
        this.lastAck = lastAck;
    }

    public GBNReceiver(int port) throws Exception {
        this.socket = new DatagramSocket(port);
        this.expectedSeqNum = 0;
        this.lastAck = -1;
        Logger.log("[GBN/RECEIVER] Aguardando na porta " + port);
    }




    public void start() throws Exception {
        while (true) {
            byte[] buf = new byte[8192];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            socket.receive(dp);

            byte[] exact = Arrays.copyOf(dp.getData(), dp.getLength());
            Packet pkt = Packet.fromBytes(exact);

            if (pkt.isCorrupted()) {
                Logger.log("[GBN/RECEIVER] Pacote corrompido → reenviando ACK último=" + lastAck);
                sendAck(dp.getAddress(), dp.getPort(), lastAck);
                continue;
            }

            int seq = pkt.getSeqNum();
            if (seq == expectedSeqNum) {
                String data = new String(pkt.getData(), StandardCharsets.UTF_8);
                Logger.log("[GBN/RECEIVER] Recebido seq=" + seq + " → " + data);
                sendAck(dp.getAddress(), dp.getPort(), seq);
                lastAck = seq;
                expectedSeqNum++;
            } else {
                Logger.log("[GBN/RECEIVER] Fora de ordem (esperava " + expectedSeqNum + ", recebeu " + seq + ") → reenviando ACK último=" + lastAck);
                sendAck(dp.getAddress(), dp.getPort(), lastAck);
            }
        }
    }

    private void sendAck(InetAddress addr, int port, int seq) throws Exception {
        Packet ack = new Packet(Packet.PacketType.ACK, seq, new byte[0]);
        DatagramPacket dp = new DatagramPacket(ack.toBytes(), ack.toBytes().length, addr, port);
        socket.send(dp);
    }

    public void close() {
        socket.close();
    }
}

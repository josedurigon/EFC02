package org.redes.fase1.rdt21;

import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class Rdt21Receiver {


    private final DatagramSocket socket;
    private int expected = 0; // espera 0/1 alternante
    private int lastDelivered = 1; // para reenviar ACK do último válido (começa como oposto do esperado)

    public Rdt21Receiver(int port) throws Exception {
        this.socket = new DatagramSocket(port);
        Logger.log("[RDT2.1/RECEIVER] Aguardando na porta " + port);
    }

    public String receiveOnce() throws Exception {
        byte[] buf = new byte[8192];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        socket.receive(dp);

        byte[] exact = new byte[dp.getLength()];
        System.arraycopy(dp.getData(), 0, exact, 0, dp.getLength());

        Packet pkt = Packet.fromBytes(exact);

        if (pkt.isCorrupted()) {
            Logger.log("[RDT2.1/RECEIVER] Corrompido → reenviando ACK do último seq=" + lastDelivered);
            Packet ack = new Packet(Packet.PacketType.ACK, lastDelivered, new byte[0]);
            socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, dp.getAddress(), dp.getPort()));
            return null;
        }

        int seq = pkt.getSeqNum();
        if (seq == expected) {
            String data = new String(pkt.getData(), StandardCharsets.UTF_8);
            Logger.log("[RDT2.1/RECEIVER] Válido seq=" + seq + " → " + data);

            // entrega à aplicação (aqui apenas retorna)
            Packet ack = new Packet(Packet.PacketType.ACK, seq, new byte[0]);
            socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, dp.getAddress(), dp.getPort()));

            lastDelivered = seq;
            expected = 1 - expected;
            return data;
        } else {
            Logger.log("[RDT2.1/RECEIVER] Duplicado seq=" + seq + " (esperava " + expected + ") → reenviando ACK " + lastDelivered);
            Packet ack = new Packet(Packet.PacketType.ACK, lastDelivered, new byte[0]);
            socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, dp.getAddress(), dp.getPort()));
            return null;
        }
    }

    public void close() { socket.close(); }

}

package org.redes.fase1.rdt20;

import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Rdt20Receiver {
    private final DatagramSocket socket;

    public Rdt20Receiver(int port) throws Exception {
        this.socket = new DatagramSocket(port);
        Logger.log("[RDT2.0/RECEIVER] Aguardando na porta " + port);
    }

    public String receiveOnce() throws Exception {
        byte[] buf = new byte[8192];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        socket.receive(dp);

        byte[] exact = new byte[dp.getLength()];
        System.arraycopy(dp.getData(), 0, exact, 0, dp.getLength());

        Packet pkt = Packet.fromBytes(exact);

        if (pkt.isCorrupted()) {
            Logger.log("[RDT2.0/RECEIVER] Pacote corrompido → NAK");
            Packet nak = new Packet(Packet.PacketType.NAK, 0, new byte[0]);
            socket.send(new DatagramPacket(nak.toBytes(), nak.toBytes().length, dp.getAddress(), dp.getPort()));
            return null;
        }

        String data = new String(pkt.getData(), StandardCharsets.UTF_8);
        Logger.log("[RDT2.0/RECEIVER] Recebido válido: " + data);

        Packet ack = new Packet(Packet.PacketType.ACK, 0, new byte[0]);
        socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, dp.getAddress(), dp.getPort()));

        return data;
    }

    public void close() { socket.close(); }
}

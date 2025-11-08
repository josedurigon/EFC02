package org.redes.fase1.rdt21;

import org.redes.Utils.ChannelUnreliable;
import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Rdt21Sender {

    private final DatagramSocket socket;
    private final InetAddress receiverAddr;
    private final int receiverPort;
    private final ChannelUnreliable channel;
    private int seq = 0; // alternante: 0/1

    public Rdt21Sender(String host, int port, ChannelUnreliable channel) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddr = InetAddress.getByName(host);
        this.receiverPort = port;
        this.channel = channel;
    }

    public void send(String message) throws Exception {
        Packet pkt = new Packet(Packet.PacketType.DATA, seq, message.getBytes(StandardCharsets.UTF_8));
        Logger.log("[RDT2.1/SENDER] Enviando seq=" + seq + " → " + message);
        channel.send(pkt, socket, receiverAddr, receiverPort);

        while (true) {
            byte[] buf = new byte[4096];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            socket.receive(dp);

            byte[] exact = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, exact, 0, dp.getLength());

            Packet ack = Packet.fromBytes(exact);

            if (ack.getType() == Packet.PacketType.ACK && !ack.isCorrupted() && ack.getSeqNum() == seq) {
                Logger.log("[RDT2.1/SENDER] ACK seq=" + seq + " recebido");
                seq = 1 - seq; // alterna
                return;
            } else {
                Logger.log("[RDT2.1/SENDER] ACK inválido/corrompido (esperava seq=" + seq + ") → retransmitindo");
                channel.send(pkt, socket, receiverAddr, receiverPort);
            }
        }
    }

    public void close() { socket.close(); }
}

package org.redes.fase1.rdt20;

import org.redes.Utils.ChannelUnreliable;
import org.redes.Utils.Logger;
import org.redes.Utils.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Rdt20Sender {
    private final DatagramSocket socket;
    private final InetAddress receiverAddr;
    private final int receiverPort;
    private final ChannelUnreliable channel;

    public Rdt20Sender(String host, int port, ChannelUnreliable channel) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddr = InetAddress.getByName(host);
        this.receiverPort = port;
        this.channel = channel;
    }


    public void send(String message) throws Exception {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        Packet pkt = new Packet(Packet.PacketType.DATA, 0, data);

        Logger.log("[SENDER] Enviando: " + message);
        channel.send(pkt, socket, receiverAddr, receiverPort);

        byte[] buf = new byte[2048];
        DatagramPacket response = new DatagramPacket(buf, buf.length);
        socket.receive(response);

        Packet ack = Packet.fromBytes(response.getData());
        if (ack.getType() == Packet.PacketType.NAK) {
            Logger.log("[SENDER] Recebido NAK, retransmitindo...");
            send(message); // retransmissão
        } else {
            Logger.log("[SENDER] Recebido ACK, mensagem confirmada.");
        }
    }

    public void close() {
        socket.close();
    }
}

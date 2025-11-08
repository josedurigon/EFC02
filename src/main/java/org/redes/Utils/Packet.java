package org.redes.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class Packet implements Serializable {

    public enum PacketType {
        DATA((byte) 0),
        ACK((byte) 1),
        NAK((byte) 2);

        private final byte code;
        PacketType(byte code) { this.code = code; }
        public byte getCode() { return code; }

        public static PacketType fromCode(byte code) {
            for (PacketType t : values())
                if (t.code == code) return t;
            return null;
        }
    }

    private PacketType type;
    private int seqNum;
    private byte[] data;
    private String checksum;

    public Packet(PacketType type, int seqNum, byte[] data) {
        this.type = type;
        this.seqNum = seqNum;
        this.data = data != null ? data : new byte[0];
        this.checksum = calculateChecksum();
    }

    private String calculateChecksum() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular checksum", e);
        }
    }

    public boolean isCorrupted() {
        return !calculateChecksum().equals(this.checksum);
    }

    public byte[] toBytes() {
        byte[] checksumBytes = checksum.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + checksumBytes.length + data.length);
        buffer.put(type.getCode());
        buffer.putInt(seqNum);
        buffer.putInt(checksumBytes.length);
        buffer.put(checksumBytes);
        buffer.put(data);
        return buffer.array();
    }

    public static Packet fromBytes(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        PacketType type = PacketType.fromCode(buffer.get());
        int seqNum = buffer.getInt();
        int checksumLength = buffer.getInt();

        byte[] checksumBytes = new byte[checksumLength];
        buffer.get(checksumBytes);

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        Packet p = new Packet(type, seqNum, data);
        p.checksum = new String(checksumBytes);
        return p;
    }

    // Getters
    public PacketType getType() { return type; }
    public int getSeqNum() { return seqNum; }
    public byte[] getData() { return data; }
    public String getChecksum() { return checksum; }

    @Override
    public String toString() {
        return String.format("[Packet type=%s seq=%d len=%d checksum=%s]",
                type, seqNum, data.length, checksum.substring(0, 6) + "...");
    }
}

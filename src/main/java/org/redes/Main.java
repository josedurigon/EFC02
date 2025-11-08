package org.redes;

import org.redes.Utils.ChannelUnreliable;
import org.redes.Utils.Logger;
import org.redes.fase1.rdt20.Rdt20Receiver;
import org.redes.fase1.rdt20.Rdt20Sender;
import org.redes.fase1.rdt21.Rdt21Receiver;
import org.redes.fase1.rdt21.Rdt21Sender;
import org.redes.fase1.rdt30.Rdt30Receiver;
import org.redes.fase1.rdt30.Rdt30Sender;
import org.redes.fase2.GBNReceiver;
import org.redes.fase2.GBNSender;
import org.redes.fase3.SRReceiver;
import org.redes.fase3.SRSender;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso:");
            System.out.println("  java -cp out org.redes.Main rdt20");
            System.out.println("  java -cp out org.redes.Main rdt21");
            System.out.println("  java -cp out org.redes.Main rdt30");
            return;
        }

        String fase = args[0].toLowerCase();

        switch (fase) {
            case "rdt20":
                executarRdt20();
                break;
            case "rdt21":
                executarRdt21();

                break;
            case "rdt30":
                executarRdt30();
                break;

            case "gbn":
                executarGoBackN();
                break;
            case "sr":
                executarSelectiveRepeat();
                break;

            default:
                System.out.println("Fase desconhecida: " + fase);
        }
    }


    private static void executarRdt20() throws Exception {
        Logger.log("[MAIN] Iniciando teste da Fase 1A (rdt2.0)");
        ChannelUnreliable canal = new ChannelUnreliable(0.0, 0.3, 0.05, 0.3);

        Thread receptor = new Thread(() -> {
            try {
                Rdt20Receiver recv = new Rdt20Receiver(9000);
                for (int i = 0; i < 10; i++) recv.receiveOnce();
                recv.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receptor.start();
        Thread.sleep(500);

        Rdt20Sender sender = new Rdt20Sender("localhost", 9000, canal);
        for (int i = 1; i <= 10; i++) sender.send("Mensagem " + i);
        sender.close();
        canal.shutdown();
        receptor.join();
    }

    private static void executarRdt21() throws Exception {
        Logger.log("[MAIN] Iniciando teste da Fase 1B (rdt2.1)");
        ChannelUnreliable canal = new ChannelUnreliable(0.0, 0.2, 0.05, 0.2);

        Thread receptor = new Thread(() -> {
            try {
                Rdt21Receiver recv = new Rdt21Receiver(9001);
                for (int i = 0; i < 10; i++) recv.receiveOnce();
                recv.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receptor.start();
        Thread.sleep(500);

        Rdt21Sender sender = new Rdt21Sender("localhost", 9001, canal);
        for (int i = 1; i <= 10; i++) sender.send("Mensagem " + i);
        sender.close();
        canal.shutdown();
        receptor.join();
    }

    private static void executarRdt30() throws Exception {
        Logger.log("[MAIN] Iniciando teste da Fase 1C (rdt3.0)");
        ChannelUnreliable canal = new ChannelUnreliable(0.15, 0.15, 0.05, 0.3);

        Thread receptor = new Thread(() -> {
            try {
                Rdt30Receiver recv = new Rdt30Receiver(9002);
                for (int i = 0; i < 10; i++) recv.receiveOnce();
                recv.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receptor.start();
        Thread.sleep(500);

        Rdt30Sender sender = new Rdt30Sender("localhost", 9002, canal, 2000);
        for (int i = 1; i <= 10; i++) sender.send("Mensagem " + i);
        sender.close();
        canal.shutdown();
        receptor.join();
    }

    private static void executarGoBackN() throws Exception {
        Logger.log("[MAIN] Iniciando teste da Fase 2 (Go-Back-N)");
        ChannelUnreliable canal = new ChannelUnreliable(0.1, 0.05, 0.05, 0.2);

        Thread receptor = new Thread(() -> {
            try {
                GBNReceiver recv = new GBNReceiver(9010);
                recv.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receptor.start();
        Thread.sleep(500);

        GBNSender sender = new GBNSender("localhost", 9010, canal, 5, 2000);
        sender.startAckListener();

        for (int i = 1; i <= 20; i++) {
            sender.send("Mensagem " + i);
            Thread.sleep(100);
        }

        sender.close();
        canal.shutdown();
        receptor.join();
    }

    private static void executarSelectiveRepeat() throws Exception {
        Logger.log("[MAIN] Iniciando teste da Fase 3 (Selective Repeat)");
        ChannelUnreliable canal = new ChannelUnreliable(0.1, 0.05, 0.05, 0.2);

        Thread receptor = new Thread(() -> {
            try {
                SRReceiver recv = new SRReceiver(9020, 5);
                recv.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receptor.start();
        Thread.sleep(500);

        SRSender sender = new SRSender("localhost", 9020, canal, 5, 2000);
        sender.startAckListener();

        for (int i = 1; i <= 20; i++) {
            sender.send("Mensagem " + i);
            Thread.sleep(100);
        }

        sender.close();
        canal.shutdown();
        receptor.join();
    }




}

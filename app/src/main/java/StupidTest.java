import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class StupidTest {

    public static boolean isSecondaryLine(String line) {
        return line.length() != 0 && line.charAt(0) == ' ';
    }

    public static boolean isTertiaryLine(String line) {
        return line.length() != 0 && line.charAt(0) == '\t';
    }

    public static void main(String[] args) throws IP6Exception, BadInputException, IOException {

        BufferedWriter output = null;
        BufferedWriter failOutput = null;

        String fileName = "asdffff.txt";
        String[] lines = {
                "13:45:47.101339 IP (tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575)",
                "    41.249.197.208.63145 > 192.168.0.20.4852: Flags [P.], cksum 0xacdf (correct), seq 246:284, ack 276, win 229, options [nop,nop,TS val 1454965 ecr 6631861], length 38",
                "13:45:47.101339 IP6 (tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575)",
                "17:51:38.580697 IP (tos 0x0, ttl 64, id 7600, offset 0, flags [DF], proto TCP (6), length 1119)",
                "    192.168.0.20.53887 > 91.234.200.114.80: Flags [P.], cksum 0xc015 (correct), seq 1:1080, ack 1, win 229, length 1079: HTTP, length: 1079",
                "\tGET / HTTP/1.1",
                "\tHost: www.delfi.lt",
                "\tConnection: keep-alive",
                "\tAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                "\tUpgrade-Insecure-Requests: 1",
                "\tUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36",
                "\tAccept-Encoding: gzip, deflate, sdch",
                "\tAccept-Language: en-GB,en;q=0.8,lt;q=0.6,fr;q=0.4",
                "\tCookie: __gfp_64b=QRwq6FrQRDenrRB1XyjZCmasiqR8iws2yufccsSnYhn.i7; dcid=1172438160,1,1479593027,1448057027,80e6e36978764420dcd85afeb62482b2; _cb_ls=1; roost-isopen=false; roost-show-prompt=true; cookiebar=1; cacheID=20151121X3ea1b1c6fa62468198fefbcc4a3fef3d; __utma=109566352.1258204060.1448057026.1448275179.1448275179.1; __utmc=109566352; __utmz=109566352.1448275179.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _v__chartbeat3=s2a6lmr_mAPaW-I; _gat=1; _ga=GA1.2.1258204060.1448057026; _chartbeat2=sB9-pcEpw_DWBgms.1448057030255.1448300735785.1111; roost-notes-read=%7B%22data%22%3A%5B%5D%7D; roost-flyout=true; _chartbeat4=t=DmmQSWCZxv62DJuGYiufDExkppei&E=3&EE=3&x=0&c=0.68&y=32085&w=659"
        };
        Packet packet = new Packet();

        int lineNumber = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            output = new BufferedWriter(new FileWriter("output.txt"));
            failOutput = new BufferedWriter(new FileWriter("fail.txt"));
            for (String line; (line = br.readLine()) != null; ) {

                if (isSecondaryLine(line)) {
                    if (packet.isInitialized()) {
                        packet.parseSecondaryLine(line);
                    } else {
                        // TODO
                    }
                } else if (isTertiaryLine(line)) {
                    packet.parseTertiaryLine(line);
                } else {
                    if (packet.isInitialized()) {
                        finishPacket(packet, output, failOutput);
                    }
                    packet = new Packet(line);
                }
                lineNumber++;
            }
            // line is not visible here.
        } catch (BadInputException | IP6Exception e) {
            System.out.println("Line " + Integer.toString(lineNumber));
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            output.close();
            failOutput.close();
        }
    }

    private static void finishPacket(Packet packet, BufferedWriter output, BufferedWriter failOutput) throws IOException {
        if (packet.isIgnore()) {
            failOutput.write(packet.toString() + "\n");
        } else {
            output.write(packet.toString() + "\n");
        }
    }
}

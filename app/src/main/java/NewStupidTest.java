import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewStupidTest {
    public static void main(String[] args) {
        String fileName = "shit.txt";
//      e.process.gapps  1856    10019  112u  IPv6 195624       TCP 192.168.0.17:57037->216.58.210.46:443 (ESTABLISHED)
//                                      app    smth       smth     smth    ip      smth   prot    srcip   srcport  dstip    dstport
        Pattern tcp = Pattern.compile("(.+?) +([0-9]+) +([0-9]+) +(.+?) +(.+?) +([0-9]+) +(.+?) +([^:]+):([0-9]+)->([^:]+):([0-9]+).*");
        String[] asdf = {"", "a", "", ""};
        int i = 0;
        while (true) {
            asdf[i++] = "asdf";
            if (i == 4) break;
        }
        for (String a: asdf) {
            System.out.println(a);
        }
//        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//            for (String line; (line = br.readLine()) != null; ) {
//                Matcher m = tcp.matcher(line);
//                System.out.println(m.matches());
//                System.out.print(m.group(8));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//
//        }
    }
}

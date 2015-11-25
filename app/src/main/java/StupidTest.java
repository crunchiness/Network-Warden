public class StupidTest {

    public static boolean isSecondaryLine(String line) {
        return line.length() != 0 && line.charAt(0) == ' ';
    }

    public static boolean isTertiaryLine(String line) {
        return line.length() != 0 && line.charAt(0) == '\t';
    }

    public static void main(String[] args) throws IP6Exception, BadInputException, Exception {
        String line1 = "13:45:47.101339 IP (tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575)";
        String line2 = "    192.168.0.17.49973 > 173.241.240.219.80: Flags [S], cksum 0x5fb5 (incorrect -> 0xb3b0), seq 4263616793, win 14600, options [mss";
        String line3 = "13:45:47.101339 IP6 (tos 0x0, ttl 64, id 5999, offset 0, flags [DF], proto TCP (6), length 575)";

        String[] lines = {line1, line2, line3};

        Packet packet = new Packet();
        for (String line : lines) {
            if (isSecondaryLine(line)) {
                if (!packet.isInitialized()) {
                    throw new Exception("Encountered secondary line before valid IP line"); // should probably skip these lines? or save them separately
                }
                packet.parseSecondaryLine(line);
            } else if (isTertiaryLine(line)) {
                System.out.println("Tertiary line");
            } else {
                if (packet.isInitialized()) {
                    // TODO do something with old packet, has to be not initialized after
                }
                try {
                    packet = new Packet(line);
                } catch (BadInputException | IP6Exception e) {
                    // save the ip line
                    System.out.println(e.getMessage());
                }

            }
        }
    }
}

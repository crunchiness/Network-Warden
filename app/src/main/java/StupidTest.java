import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import warden.Packet;

public class StupidTest {
    private enum Line {
        PRIMARY, SECONDARY, TERTIARY, EMPTY
    }

    public static void main(String[] args) throws IOException {

        BufferedWriter output = null;
        BufferedWriter failOutput = null;
        BufferedWriter errorOutput = null;

        String fileName = "asdffff.txt";

        Packet packet = new Packet();

        int lineNumber = 1;
        int ip6Count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            output = new BufferedWriter(new FileWriter("output.txt"));
            failOutput = new BufferedWriter(new FileWriter("fail.txt"));
            errorOutput = new BufferedWriter(new FileWriter("error.txt"));
            for (String line; (line = br.readLine()) != null; ) {
                switch (lineIs(line)) {
                    case PRIMARY:
                        if (packet.isInitialized()) {
                            finishPacket(packet, output, failOutput);
                        }
                        packet = new Packet(line);
                        break;
                    case SECONDARY:
                        if (packet.isInitialized()) {
                            packet.parseSecondaryLine(line);
                        } else {
                            errorOutput.write("ERR1 " + line + "\n");
                        }
                        break;
                    case TERTIARY:
                        packet.parseTertiaryLine(line);
                        break;
                    default:
                        errorOutput.write("ERR2 " + line + "\n");
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                output.close();
            }
            if (failOutput != null) {
                failOutput.close();
            }
            if (errorOutput != null) {
                errorOutput.close();
            }
        }
    }

    /**
     * Determines what kind of line this is
     * @param line line
     * @return enum with the kind
     */
    private static Line lineIs(String line) {
        if (line.length() == 0) {
            return Line.EMPTY;
        } else if (line.charAt(0) == ' ') {
            return Line.SECONDARY;
        } else if (line.charAt(0) == '\t') {
            return Line.TERTIARY;
        }
        return Line.PRIMARY;
    }

    private static void finishPacket(Packet packet, BufferedWriter output, BufferedWriter failOutput) throws IOException {
        if (packet.isIgnore()) {
            failOutput.write(packet.toString());
        } else {
            output.write(packet.toString());
        }
    }
}

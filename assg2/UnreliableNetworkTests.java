import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static org.junit.Assert.*;

public class UnreliableNetworkTests {
    private static final Integer PORT_RECEIVE = 9001;
    private static final Integer PORT_UNRELINET = 9000;
    private static final float DATA_CORRUPT_RATE = 0.1f;
    private static final float ACK_CORRUPT_RATE = 0.1f;
    private static final float DATA_LOSS_RATE = 0.1f;
    private static final float ACK_LOSS_RATE = 0.1f;

    FileReceiver fileReceiver = null;

    @BeforeClass
    public static void setupUnreliableNetwork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new UnreliNET(
                        DATA_CORRUPT_RATE,
                        ACK_CORRUPT_RATE,
                        DATA_LOSS_RATE,
                        ACK_LOSS_RATE,
                        PORT_UNRELINET,
                        PORT_RECEIVE
                );
            }
        }).start();
    }

    @Before
    public void setupFileReceiver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fileReceiver = new FileReceiver(PORT_RECEIVE);
                    fileReceiver.serve_until_end_of_file();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Test
    public void testTransferSmallImage() throws Exception {
        String[] sender_args = { "data/doge.jpg", "localhost", String.valueOf(PORT_UNRELINET) , "data/doge-copy.jpg" };
        FileSender.main(sender_args);

        
        Checksum checksum = new CRC32();
        Path p = FileSystems.getDefault().getPath("data", "doge.jpg");
        byte [] fileData = Files.readAllBytes(p);
        checksum.update(fileData, 0, fileData.length);
        long originalChecksum = checksum.getValue();

        Checksum checksum2 = new CRC32();
        Path p2 = FileSystems.getDefault().getPath("data", "doge-cpy.jpg");
        byte [] fileData2 = Files.readAllBytes(p);
        checksum2.update(fileData2, 0, fileData2.length);
        long cloneChecksum = checksum2.getValue();

        assertEquals(originalChecksum, cloneChecksum);
    }
    
    @Test
    public void testTransferLargeImage() throws Exception {
        String[] sender_args = { "data/cny.mp3", "localhost", String.valueOf(PORT_UNRELINET) , "data/cny-copy.mp3" };
        FileSender.main(sender_args);

        
        Checksum checksum = new CRC32();
        Path p = FileSystems.getDefault().getPath("data", "cny.mp3");
        byte [] fileData = Files.readAllBytes(p);
        checksum.update(fileData, 0, fileData.length);
        long originalChecksum = checksum.getValue();

        Checksum checksum2 = new CRC32();
        Path p2 = FileSystems.getDefault().getPath("data", "cny-copy.mp3");
        byte [] fileData2 = Files.readAllBytes(p);
        checksum2.update(fileData2, 0, fileData2.length);
        long cloneChecksum = checksum2.getValue();

        assertEquals(originalChecksum, cloneChecksum);
    }
}
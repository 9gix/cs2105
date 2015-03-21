// Author: Eugene

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

class FileReceiver {
    // LOGGING
    private static final Logger log = Logger.getLogger(FileReceiver.class.getName());
    private static final Level LOG_LEVEL = Level.SEVERE;
    
    

    public DatagramSocket socket;
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    public static final int PACKET_BUFFER_SIZE = 1000;
    
    byte[] packet_data;
    
    
    public static final byte SYN_MASK = 0b00000001;
    public static final byte ACK_MASK = 0b00000010;
    public static final byte NAK_MASK = 0b00000100;
    public static final byte FIN_MASK = 0b00001000;

    

    // Packet Data Structure 
    private short data_length;
    private int data_checksum;
    private int seq_no;
    private byte flags;
    private byte[] data;
    // -------------------------
    
    // Receiver Data Structure
    private File file;
    private int receive_base; // Current Window Starting Sequence
    
    int current_offset = 0;
    // -------------------------
    private boolean is_corrupted;
    private String filename;
    private int current_seq;
    private FileChannel fc;
    private ByteBuffer dataBuffer;
    private ByteBuffer packetBuffer;
    
    public static class Packet {
        public static final int SEQ_BYTE_OFFSET = 0; // integer
        public static final int SEQ_BYTE_LENGTH = 4;
        public static final int CHECKSUM_BYTE_OFFSET = 4; // integer
        public static final int CHECKSUM_BYTE_LENGTH= 4;
        public static final int FLAG_BYTE_OFFSET = 8; // short
        public static final int FLAG_BYTE_LENGTH = 1;
        public static final int DATA_BYTE_OFFSET = 9; // byte[]
        public static final int DATA_BYTE_LENGTH = 991;
        
        public static final int HEADER_LENGTH = 9;
    }
    
    

    public static void main(String[] args) throws IOException {
        log.setLevel(LOG_LEVEL);

        // check if the number of command line argument is 1
        if (args.length != 1) {
            System.out.println("Usage: java FileReceiver port");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        FileReceiver server = new FileReceiver(port);
        server.serve_until_end_of_file();
    }


    public FileReceiver(int localPort) throws SocketException {
        socket = new DatagramSocket(localPort);
        packet_buffer = new byte[PACKET_BUFFER_SIZE];
        packet = new DatagramPacket(packet_buffer, packet_buffer.length);
    }

    public void serve_until_end_of_file() throws IOException{
        receivePacket();
        prepareFile();
        log.warning("Receiving File: " + this.filename);
        long filePosition;
        current_seq = 1;
        
        while (true) {
            try{
                receivePacket();
            } catch (SocketTimeoutException e){
                log.info("File Received: " + this.filename);
                log.info("Shutting Down Server");
                return;
            }
            if (!isEOF()){
                filePosition = (this.seq_no - 1)* Packet.DATA_BYTE_LENGTH;
                if (filePosition >= 0){
                    updateFile(dataBuffer, filePosition);
                    current_seq = seq_no + 1;
                }
            } else {
                socket.setSoTimeout(2000);
            }
        }
    }

    private ByteBuffer receivePacket() throws IOException{
        this.is_corrupted = true;
        ByteBuffer buffer = null;
        while (this.is_corrupted){
            socket.receive(packet);
            processPacket();
            log.warning("Received Packet Seq: " + this.seq_no);
            if (this.is_corrupted){
                log.warning("Packet Corrupted");
                sendNak();
            }
            if (current_seq > seq_no){
                log.warning(
                        "Packet Ignored due to Out of Sequence\n" +
                        "Current Sequence: " + current_seq + "\n" + 
                        "Received Sequence: " + this.seq_no + "\n"
                        );
            }
        }
        acknowledgePacket();
        return buffer;
    }
    

    private void sendNak() throws IOException {

        byte[] nak_buff = new byte[Packet.HEADER_LENGTH];
        DatagramPacket nak_packet = new DatagramPacket(
                nak_buff, nak_buff.length, packet.getSocketAddress());
        
        ByteBuffer buffer = ByteBuffer.wrap(nak_buff);
        buffer.putInt(this.seq_no); // Sequence No
        buffer.putInt(0); // Checksum
        buffer.put(NAK_MASK); // Flag
        nak_packet.setLength(Packet.HEADER_LENGTH);
        
        int checksum = calculateChecksum(buffer.array());
        buffer.putInt(Packet.CHECKSUM_BYTE_OFFSET, checksum);
        socket.send(nak_packet);
        log.warning("Sending NAK: " + this.seq_no);
    }


    private void acknowledgePacket() throws IOException {
        sendAck();
    }


    /***
     * 
     * @param packet_buffer
     * @return true if data packet is correct
     */
    public static boolean verifyChecksum(ByteBuffer packetBuffer) {
        int sent_checksum = packetBuffer.getInt(Packet.CHECKSUM_BYTE_OFFSET);
        byte[] b = packetBuffer.array();
        Arrays.fill(b, Packet.CHECKSUM_BYTE_OFFSET, Packet.CHECKSUM_BYTE_OFFSET + Packet.CHECKSUM_BYTE_LENGTH, (byte) 0);
        int received_checksum = calculateChecksum(b);
        return sent_checksum == received_checksum;
    }
    
    public static int calculateChecksum(byte[] data){
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }

    private void sendAck() throws IOException {

        byte[] ack_buff = new byte[Packet.HEADER_LENGTH];
        DatagramPacket ack_packet = new DatagramPacket(
                ack_buff, ack_buff.length, packet.getSocketAddress());
        
        ByteBuffer buffer = ByteBuffer.wrap(ack_buff);
        buffer.putInt(this.seq_no + 1); // Sequence No
        buffer.putInt(0); // Checksum
        buffer.put(ACK_MASK); // Flag
        ack_packet.setLength(Packet.HEADER_LENGTH);
        
        int checksum = calculateChecksum(buffer.array());
        buffer.putInt(Packet.CHECKSUM_BYTE_OFFSET, checksum);

        socket.send(ack_packet);
        log.warning("Sending Ack: " + this.seq_no);
    }
    
    

    private void updateFile(ByteBuffer buffer, long filePosition) throws IOException {
        fc.write(buffer, filePosition);
        log.warning("File updated");
    }

    private void prepareFile() throws IOException {
        filename = new String(data).trim();
        file = new File(filename);
        if (file.exists()){
            file.delete();
        }
        file.createNewFile();
        
        fc = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
    }
    
    private void processPacket() {
        packet_data = packet.getData();
        packetBuffer = ByteBuffer.wrap(packet_data, 0, packet.getLength());
        seq_no = packetBuffer.getInt();
        data_checksum = packetBuffer.getInt();
        flags = packetBuffer.get();
        data = new byte[packetBuffer.remaining()];
        packetBuffer.get(data);
        dataBuffer = ByteBuffer.wrap(this.data);
        is_corrupted = !verifyChecksum(packetBuffer);
        packetBuffer.rewind();
    }

    private boolean isEOF() {
        return (flags & FIN_MASK) == FIN_MASK;
    }
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}

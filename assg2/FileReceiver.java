// Author: Eugene

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

class FileReceiver {

    public DatagramSocket socket;
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    public static final int PACKET_BUFFER_SIZE = 1000;
    
    byte[] packet_data;
    
    
    public static final short SYN_MASK = 0b00000001;
    public static final short ACK_MASK = 0b00000010;
    public static final short FIN_MASK = 0b00000100;
    

    // Packet Data Structure 
    private short data_length;
    private int data_checksum;
    private int seq_no;
    private int ack_no;
    private short flags;
    private byte[] data;
    // -------------------------
    
    // Receiver Data Structure
    private File file;
    private int receive_base; // Current Window Starting Sequence
    
    int current_offset = 0;
    // -------------------------
    private boolean is_corrupted;
    private String filename;
    private RandomAccessFile file_access;
    private int current_seq;
    
    public static class Packet {
        public static final int SEQ_BYTE_OFFSET = 0; // integer
        public static final int SEQ_BYTE_LENGTH = 4;
        public static final int ACK_BYTE_OFFSET = 4; // integer
        public static final int ACK_BYTE_LENGTH = 4;
        public static final int CHECKSUM_BYTE_OFFSET = 8; // integer
        public static final int CHECKSUM_BYTE_LENGTH= 4;
        public static final int FLAG_BYTE_OFFSET = 12; // short
        public static final int FLAG_BYTE_LENGTH = 2;
        public static final int DATA_BYTE_OFFSET = 14; // byte[]
        public static final int DATA_BYTE_LENGTH = 986;
        
        public static final int HEADER_LENGTH = 14;
    }
    
    

    public static void main(String[] args) throws IOException {

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
        
        while (true) {
            try{
                receivePacket();
            } catch (SocketTimeoutException e){
                return;
            }
            if (!isEOF()){
                if (current_seq + 1 == seq_no){
                    updateFile();
                    current_seq = seq_no;
                }
            } else {
                socket.setSoTimeout(3000);
            }
        }
    }

    private void receivePacket() throws IOException{
        this.is_corrupted = true;
        while (this.is_corrupted){
            socket.receive(packet);
            processPacket();
            System.out.println("Received Packet Seq: " + this.seq_no);
            if (this.is_corrupted){
                System.out.println("Packet Corrupted");
            }
            if (current_seq > seq_no){
                System.out.println("Out of Sequence");
            }
        }
        acknowledgePacket();
    }
    

    private void acknowledgePacket() throws IOException {
        this.ack_no = this.seq_no;
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

        byte[] ack_buff = new byte[14];
        DatagramPacket ack_packet = new DatagramPacket(
                ack_buff, ack_buff.length, packet.getSocketAddress());
        
        ByteBuffer buffer = ByteBuffer.wrap(ack_buff);
        buffer.putInt(0); // Sequence No
        buffer.putInt(ack_no); // Ack No
        buffer.putInt(0); // Checksum
        buffer.putShort(ACK_MASK); // Flag
        ack_packet.setLength(14);
        
        int checksum = calculateChecksum(buffer.array());
        buffer.putInt(Packet.CHECKSUM_BYTE_OFFSET, checksum);
        socket.send(ack_packet);
        System.out.println("Sending Ack: " + this.ack_no);
    }
    
    

    private void updateFile() throws IOException {
        file_access.write(this.data);
        System.out.println("File updated");
    }

    private void prepareFile() throws IOException {
        filename = new String(data).trim();
        file = new File(filename);
        if (file.exists()){
            file.delete();
        }
        file.createNewFile();
        file_access = new RandomAccessFile(file, "rw");
    }
    
    private void processPacket() {
        packet_data = packet.getData();
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet_data, 0, packet.getLength());
        seq_no = packetBuffer.getInt();
        ack_no = packetBuffer.getInt();
        data_checksum = packetBuffer.getInt();
        flags = packetBuffer.getShort();
        data = new byte[packetBuffer.remaining()];
        packetBuffer.get(data);
        is_corrupted = !verifyChecksum(packetBuffer);
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

// Author: Eugene

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;

class FileReceiver {

    public DatagramSocket socket;
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    public static final int PACKET_BUFFER_SIZE = 1000;
    
    byte[] packet_data;
    
    /***
     * Protocol:
     * 
     * 
     * +-----------------------------+
     * |            PACKET           |
     * +=============================+
     * | 0         : filename length | -> 1 bytes (length of the filename, up to 8 bit to represent 255 character)
     * | 1 - 255   : filename        | -> 255 bytes (maximum 255 character)
     * | 256 - 263 : file offset     | -> 8 bytes (file offset position)
     * | 264 - 267 : data length     | -> 4 bytes (data size) 
     * | 268 - 779 : data            | -> 512 bytes
     * +-----------------------------+ 
     * 
     */
    
    public static final int FILENAME_LENGTH_OFFSET = 0;
    public static final int FILENAME_LENGTH_SIZE = 1; // 1 byte
    public static final int FILENAME_BUFFER_OFFSET = 1;
    public static final int FILENAME_BUFFER_SIZE = 255; // String
    public static final int FILE_OFFSET_OFFSET = 256;
    public static final int FILE_OFFSET_SIZE = Long.SIZE / 8; // 8 bytes
    public static final int DATA_LENGTH_OFFSET = 264;
    public static final int DATA_LENGTH_SIZE = Integer.SIZE / 8; // 4 bytes
    public static final int DATA_BUFFER_OFFSET = 268;
    public static final int DATA_BUFFER_SIZE = 512; // array of bytes
    
    
    ByteArrayInputStream in_stream;

    public static void main(String[] args) throws IOException {

        // check if the number of command line argument is 1
        if (args.length != 1) {
            System.out.println("Usage: java FileReceiver port");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        FileReceiver server = new FileReceiver(port);
        server.serve_forever();
    }

    public FileReceiver(int localPort) throws SocketException {
        socket = new DatagramSocket(localPort);
        packet_buffer = new byte[PACKET_BUFFER_SIZE];
    }
    
    public void serve_forever() throws IOException{
        while (true) {
            packet = new DatagramPacket(packet_buffer, PACKET_BUFFER_SIZE);
            socket.receive(packet);
            packet_data = packet.getData();
            this.handle_packet();
            
        }
    }
    
    public String get_packet_filename(){
        byte filename_length = ByteBuffer.wrap(packet_data).get(FILENAME_LENGTH_OFFSET);
        return new String(packet_data, FILENAME_BUFFER_OFFSET, filename_length);
    }
    
    public int get_chunk_size() throws IOException{
        return ByteBuffer.wrap(packet_data).getInt(DATA_LENGTH_OFFSET);
    }
    
    public int get_chunk_offset(){
        return (int)ByteBuffer.wrap(packet_data).getLong(FILE_OFFSET_OFFSET);
    }
    
    public byte[] get_packet_content(int chunk_size) throws IOException{
        byte[] chunk_data = new byte[chunk_size];
        ((ByteBuffer)ByteBuffer.wrap(packet_data).position(DATA_BUFFER_OFFSET)).get(chunk_data);
        return chunk_data;
    }
    
    int current_offset = 0;

    public void handle_packet() throws IOException {
        in_stream = new ByteArrayInputStream(packet_data);
        File file = new File(this.get_packet_filename());
        if (!file.exists()){
            file.createNewFile();
        }
        int chunk_size = this.get_chunk_size();
        try (RandomAccessFile file_access = new RandomAccessFile(file, "rw")){
            current_offset = this.get_chunk_offset();
            file_access.seek(current_offset);
            file_access.write(this.get_packet_content(chunk_size));            
        }
        
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

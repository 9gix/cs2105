// Author: Eugene

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

class FileSender {
    
    
    
    public DatagramSocket socket; 
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        // check if the number of command line argument is 4
        if (args.length != 4) {
            System.out.println("Usage: java FileSender <path/filename> "
                                   + "<rcvHostName> <rcvPort> <rcvFileName>");
            System.exit(1);
        }
        
        String source_filename = args[0];
        String hostname = args[1];
        int port = Integer.parseInt(args[2]);
        SocketAddress socket_address = new InetSocketAddress(hostname, port);
        String destination_filename = args[3];
        new FileSender(source_filename, socket_address, destination_filename);
    }
    
    public FileSender(String source_filename, SocketAddress socket_address, String destination_filename) throws IOException, InterruptedException {
        
        packet_buffer = new byte[FileReceiver.PACKET_BUFFER_SIZE];
        byte[] data_buffer = new byte[FileReceiver.DATA_BUFFER_SIZE];
        // Filename Length
        ByteBuffer.wrap(
                packet_buffer, 
                FileReceiver.FILENAME_LENGTH_OFFSET,
                FileReceiver.FILENAME_LENGTH_SIZE).put(
                        (byte) destination_filename.length());
        
        // Filename
        ByteBuffer.wrap(
                packet_buffer, 
                FileReceiver.FILENAME_BUFFER_OFFSET, 
                FileReceiver.FILENAME_BUFFER_SIZE ).put(
                        destination_filename.getBytes());
        
    
        socket = new DatagramSocket();
        
        try (InputStream input_stream = new BufferedInputStream(new FileInputStream(source_filename))){
            int data_length;
            long file_offset = 0;
            while((data_length = input_stream.read(data_buffer, 0, FileReceiver.DATA_BUFFER_SIZE)) != -1){

                // File Offset
                ByteBuffer.wrap(
                        packet_buffer,
                        FileReceiver.FILE_OFFSET_OFFSET,
                        FileReceiver.FILE_OFFSET_SIZE).putLong(
                                file_offset);
                file_offset += data_length;
                
                // Data Length
                ByteBuffer.wrap(
                        packet_buffer,
                        FileReceiver.DATA_LENGTH_OFFSET,
                        FileReceiver.DATA_LENGTH_SIZE).putInt(
                                data_length);
                //System.out.println("SENDER: " + data_length);
                
                
                // Data 
                ByteBuffer.wrap(
                        packet_buffer, 
                        FileReceiver.DATA_BUFFER_OFFSET,
                        FileReceiver.DATA_BUFFER_SIZE).put(
                                data_buffer);
                
                
                packet = new DatagramPacket(packet_buffer, packet_buffer.length, socket_address);
                socket.send(packet);
                
                // UDP unreliable. Server didn't handle all packet if data send too fast.
                Thread.sleep(4);
            }
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
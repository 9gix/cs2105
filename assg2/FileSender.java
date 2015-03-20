// Author: Eugene

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

class FileSender {
    
    
    
    public DatagramSocket socket; 
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    private int seq_no;
    
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        // check if the number of command line argument is 4
        if (args.length != 4) {
            System.out.println("Usage: java FileSender <path/filename> "
                                   + "<rcvHostName> <rcvPort> <rcvFileName>");
            System.exit(1);
        }
        
        String sourceFilename = args[0];
        String hostname = args[1];
        int port = Integer.parseInt(args[2]);
        SocketAddress socketAddress = new InetSocketAddress(hostname, port);
        String destinationFilename = args[3];
        new FileSender(sourceFilename, socketAddress, destinationFilename);
    }
    
    public FileSender(String sourceFilename, SocketAddress socket_address, String destinationFilename) throws IOException, InterruptedException {
        
        packet_buffer = new byte[FileReceiver.PACKET_BUFFER_SIZE];
        socket = new DatagramSocket();
        socket.setSoTimeout(3);
        packet = new DatagramPacket(packet_buffer, packet_buffer.length, socket_address);
        
        sendFile(sourceFilename, destinationFilename);
    }
    
    private void sendFile(String sourceFilename, String destinationFilename) throws IOException {
        byte[] fileMetaData = new byte[FileReceiver.Packet.DATA_BYTE_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(fileMetaData);
        buffer.put(destinationFilename.getBytes());
        send(buffer.array(), false);
        
        try (InputStream input_stream = new BufferedInputStream(new FileInputStream(sourceFilename))){
            int dataLength;
            byte[] data_reader_buffer = new byte[FileReceiver.Packet.DATA_BYTE_LENGTH];
            while((dataLength = input_stream.read(data_reader_buffer)) > 0){
                seq_no++;
                ByteArrayOutputStream out_stream = new ByteArrayOutputStream();
                out_stream.write(data_reader_buffer, 0, dataLength);
                byte[] dataBuffer = out_stream.toByteArray();
                send(dataBuffer, false);
            }
            
        }
        send(new byte[0], true);
    }

    private void send(byte[] data, boolean isEOF) throws IOException {
        buildPacket(data, isEOF);
        boolean waitForAck = true;
        byte[] ackBuffArr = new byte[14];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffArr, ackBuffArr.length);
        while (waitForAck){
            if (isEOF){
                System.out.println("Sending Packet FIN");
            } else {
                System.out.println("Sending Packet Seq :" + seq_no);
            }
            socket.send(packet);
            try {
                ByteBuffer buffer = receiveAcknowledgement(ackPacket);
                int ack_no = buffer.getInt(FileReceiver.Packet.ACK_BYTE_OFFSET);
                short flags = buffer.getShort(FileReceiver.Packet.FLAG_BYTE_OFFSET);
                boolean is_ack = (flags & FileReceiver.ACK_MASK) == FileReceiver.ACK_MASK;
                boolean is_nak = (flags & FileReceiver.NAK_MASK) == FileReceiver.NAK_MASK;
                System.out.println("Received Ack:" + ack_no);
                
                // Correct ACK No & Correct ACK Packet
                if (FileReceiver.verifyChecksum(buffer) && !is_nak && is_ack){
                    waitForAck = false;
                } else {
                    waitForAck = true;
                }
            } catch (SocketTimeoutException e){
                waitForAck = true;
                System.out.println("Packet Lost");
            }
        }
    }

    private ByteBuffer receiveAcknowledgement(DatagramPacket ackPacket) throws IOException {
        socket.receive(ackPacket);
        byte[] dataBuffArr = ackPacket.getData();
        ByteBuffer ackBuffer = ByteBuffer.wrap(dataBuffArr);
        return ackBuffer;
    }

    private void buildPacket(byte[] dataBuffer, boolean isEOF) {
        ByteBuffer buffer = ByteBuffer.wrap(packet_buffer);
        buffer.putInt(seq_no);
        buffer.putInt(0);
        buffer.putInt(0);
        short flags = FileReceiver.SYN_MASK;
        if (isEOF){
            flags |= FileReceiver.FIN_MASK;
        }
        buffer.putShort(flags);
        buffer.put(dataBuffer);
        int checksum = FileReceiver.calculateChecksum(buffer.array());
        buffer.putInt(FileReceiver.Packet.CHECKSUM_BYTE_OFFSET, checksum);
        packet.setLength(FileReceiver.Packet.HEADER_LENGTH + dataBuffer.length);
    }

}
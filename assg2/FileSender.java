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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

class FileSender {
    // LOGGING
    private static final Logger log = Logger.getLogger(FileReceiver.class.getName());
    private static final Level LOG_LEVEL = Level.SEVERE;
    
    
    public DatagramSocket socket; 
    public DatagramPacket packet;
    
    byte[] packet_buffer;
    
    
    public static void main(String[] args) throws IOException, InterruptedException {
        log.setLevel(LOG_LEVEL);
        
        // check if the number of command line argument is 4
        if (args.length != 3) {
            System.out.println("Usage: java FileSender <path/filename> "
                                   + "<rcvPort> <rcvFileName>");
            System.exit(1);
        }
        
        String sourceFilename = args[0];
        String hostname = "localhost";
        int port = Integer.parseInt(args[1]);
        SocketAddress socketAddress = new InetSocketAddress(hostname, port);
        String destinationFilename = args[2];
        new FileSender(sourceFilename, socketAddress, destinationFilename);
    }
    
    public FileSender(String sourceFilename, SocketAddress socket_address, String destinationFilename) throws IOException, InterruptedException {
        
        packet_buffer = new byte[FileReceiver.PACKET_BUFFER_SIZE];
        socket = new DatagramSocket();
        socket.setSoTimeout(1);
        packet = new DatagramPacket(packet_buffer, packet_buffer.length, socket_address);
        
        sendFile(sourceFilename, destinationFilename);
    }
    
    private void sendFile(String sourceFilename, String destinationFilename) throws IOException {
        byte[] fileMetaData = new byte[FileReceiver.Packet.DATA_BYTE_LENGTH];
        
        // Sending File metadata
        ByteBuffer buffer = ByteBuffer.wrap(fileMetaData);
        buffer.put(destinationFilename.getBytes());
        send(buffer.array(), 0, false);
        log.info("Sending File: " + sourceFilename);

        // Sending File data
        int sequence = 1;
        try (InputStream input_stream = new BufferedInputStream(new FileInputStream(sourceFilename))){
            int dataLength;
            byte[] data_reader_buffer = new byte[FileReceiver.Packet.DATA_BYTE_LENGTH];
            while((dataLength = input_stream.read(data_reader_buffer)) > 0){
                ByteArrayOutputStream out_stream = new ByteArrayOutputStream();
                out_stream.write(data_reader_buffer, 0, dataLength);
                byte[] dataBuffer = out_stream.toByteArray();
                send(dataBuffer, sequence, false);
                sequence++;
            }
        }
        
        // Sending FIN flags
        send(new byte[0], sequence, true);
        log.info("File Sent as: " + destinationFilename);
    }

    private void send(byte[] data, int packetNo, boolean isEOF) throws IOException {
        buildPacket(data, packetNo, isEOF);
        boolean waitForAck = true;
        byte[] ackBuffArr = new byte[FileReceiver.Packet.HEADER_LENGTH];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffArr, ackBuffArr.length);
        while (waitForAck){
            if (isEOF){
                log.warning("Sending Packet FIN");
            } else {
                log.warning("Sending Packet Seq :" + packetNo);
            }
            socket.send(packet);
            try {
                ByteBuffer buffer = receiveAcknowledgement(ackPacket);
                int ack_no = buffer.getInt(FileReceiver.Packet.SEQ_BYTE_OFFSET);
                byte flags = buffer.get(FileReceiver.Packet.FLAG_BYTE_OFFSET);

                boolean is_verified = FileReceiver.verifyChecksum(buffer);
                boolean is_ack = (flags & FileReceiver.ACK_MASK) == FileReceiver.ACK_MASK;
                boolean is_nak = (flags & FileReceiver.NAK_MASK) == FileReceiver.NAK_MASK;
                boolean is_requesting_next_packet = (ack_no == packetNo + 1);
                // Correct ACK No & Correct ACK Packet
                if (is_verified && !is_nak && is_ack && is_requesting_next_packet){
                    log.warning("Received Ack:" + ack_no); 
                    waitForAck = false;
                } else {
                    waitForAck = true;
                    if (is_nak){
                        log.warning("Negative Acknowledgement");
                    } else if (!is_requesting_next_packet){
                        log.warning("Invalid Acknowledgement");
                    } else if (!is_verified){
                        log.warning("Acknowledgement Corrupted");
                    }
                }
            } catch (SocketTimeoutException e){
                waitForAck = true;
                log.warning("Packet Lost");
            }
        }
    }

    private ByteBuffer receiveAcknowledgement(DatagramPacket ackPacket) throws IOException {
        socket.receive(ackPacket);
        byte[] dataBuffArr = ackPacket.getData();
        ByteBuffer ackBuffer = ByteBuffer.wrap(dataBuffArr);
        return ackBuffer;
    }

    private void buildPacket(byte[] dataBuffer, int packetNo, boolean isEOF) {
        ByteBuffer buffer = ByteBuffer.wrap(packet_buffer);
        buffer.putInt(packetNo);
        buffer.putInt(0);
        byte flags = FileReceiver.SYN_MASK;
        if (isEOF){
            flags |= FileReceiver.FIN_MASK;
        }
        buffer.put(flags);
        buffer.put(dataBuffer);
        int checksum = FileReceiver.calculateChecksum(buffer.array());
        buffer.putInt(FileReceiver.Packet.CHECKSUM_BYTE_OFFSET, checksum);
        packet.setLength(FileReceiver.Packet.HEADER_LENGTH + dataBuffer.length);
    }

}
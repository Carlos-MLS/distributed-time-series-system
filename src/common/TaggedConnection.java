package common;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TaggedConnection implements AutoCloseable {

    public static class Frame {
        public final int tag; //tag , id da mensagem
        public final byte[] data; //dados da mensagem

        public Frame(int tag, byte[] data) { 
            this.tag = tag; this.data = data; }
    }

    private FramedConnection framedConnection;



    public TaggedConnection(Socket socket) throws IOException {
        this.framedConnection = new FramedConnection(socket);
    }

    public void send (Frame frame) throws IOException {
        // O dataOutputstream está a escrever no um ByteArrayOutputStream e esse array é enviado no send
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        dataOutputStream.writeInt(frame.tag); //escreve a tag
        dataOutputStream.write(frame.data); //escreve os dados
        dataOutputStream.flush();
        framedConnection.send(byteArrayOutputStream.toByteArray()); //envia tudo junto
    }

    public void send(int tag, byte[] data) throws IOException {
        send(new Frame(tag, data));
    }

    public Frame receive() throws IOException {
        byte[] receivedData = framedConnection.receive();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivedData);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        int tag = dataInputStream.readInt();
        byte[] data = new byte[receivedData.length - 4]; // 4 bytes para o int
        dataInputStream.readFully(data);

        return new Frame(tag, data);
    }

    @Override
    public void close() throws IOException {
        framedConnection.close();
    }

}

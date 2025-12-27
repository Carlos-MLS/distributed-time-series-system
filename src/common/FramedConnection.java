package common;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.*;

public class FramedConnection implements AutoCloseable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ReentrantLock sendLock = new ReentrantLock();
    private ReentrantLock receiveLock = new ReentrantLock();

    public FramedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.sendLock = new ReentrantLock();
        this.receiveLock = new ReentrantLock();
    }

    public void send(byte[] data) throws IOException {
        sendLock.lock();
        try {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } finally {
            sendLock.unlock();
        }
    }

    public byte[] receive() throws IOException {
        receiveLock.lock();
        try {
        int tamanhoDados = in.readInt();
        byte[] dados = new byte[tamanhoDados];
        in.readFully(dados);
        return dados;
        } finally {
            receiveLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
    

}

package common;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import common.TaggedConnection.Frame;




public class Demultiplexer implements AutoCloseable {
    private TaggedConnection taggedConnection; 

    Lock lock = new ReentrantLock();
    
    private IOException exception = null;
    private Map<Integer, Entry> entries = new HashMap<Integer, Entry>(); //mapa de tags para filas de mensagens

    private class Entry {
        Deque<byte[]> queue = new ArrayDeque<>();
        Condition condition = lock.newCondition();
        int waiters = 0; // número de threads à espera 
    }

    private Entry getEntry(int tag) {
        Entry entry = entries.get(tag);
        if (entry == null) {
            entry = new Entry();
            entries.put(tag, entry);
        }
        return entry;
    }

    public Demultiplexer(TaggedConnection taggedConnection) {
        this.taggedConnection = taggedConnection;
    }

    //Thread que fica a receber mensagens e a colocá-las na fila correta
    public void start() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Frame frame = taggedConnection.receive();
                    lock.lock();
                    try {
                        Entry entry = getEntry(frame.tag);
                        entry.queue.add(frame.data);
                        entry.condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (IOException e) {
                lock.lock();
                try {
                    exception = e;
                    entries.forEach((tag, entry) -> {
                    entry.condition.signalAll();
                 });    
                } finally {
                    lock.unlock();
                }
            }
        });
        t.start();
    }

    public void send (Frame frame) throws IOException {
        taggedConnection.send(frame);
    }

    //envia mensagem com tag e dados
    public void send(int tag, byte[] data) throws IOException {
        taggedConnection.send(tag, data);
    }

    //recebe mensagem com a tag especificada, se n houver mensagens com essa tag bloqueia até chegar uma
    public byte[] receive(int tag) throws IOException, InterruptedException {
        lock.lock(); 
        try {
            Entry entry = getEntry(tag);  // cria ou obtém a entry para a tag
            entry.waiters++;               // incrementa o número de waiters

            while (true) {                 
                if(!entry.queue.isEmpty()) {  // tem mensagens na fila?
                    entry.waiters--;              // decrementa o número de waiters
                    byte[] resposta = entry.queue.poll();  // remove primeira msg da fila

                    if(entry.waiters == 0 && entry.queue.isEmpty()) {  // nenhuma thread à espera e fila vazia?
                        entries.remove(tag);   //se não tiver, remove a entry do mapa
                    }

                    return resposta;
                }

                if (exception != null) 
                    throw exception;       
                entry.condition.await();  // espera por uma mensagem
            }
        } finally {
            lock.unlock();  
        }
    }

    @Override
    public void close() throws IOException {
        taggedConnection.close();
    }

}

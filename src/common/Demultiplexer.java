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
    // Mapa de tags para filas de mensagens, e permite múltiplas threads concorrentes
    // Cada tag representa um pedido/resposta independente, podemos ter várias threads à espera da mesma tag
    private Map<Integer, Entry> entries = new HashMap<Integer, Entry>();

    // Entry representa a  fila de mensagenss e a sincronização para uma tag específica
    // Implementa padrão produtor-consumidor: thread leitora produz, threads cliente consomem
    private class Entry {
        Deque<byte[]> queue = new ArrayDeque<>();  // Fila FIFO de respostas recebidas para esta tag
        Condition condition = lock.newCondition();  // Condition para acordar threads à espera desta tag
        int waiters = 0; // Número de threads cliente bloqueadas à espera de resposta com esta tag
                         // Usado para saber quando podemos remover a Entry do mapa (quando waiters==0 e queue vazia)
    }

    // Obtém ou cria Entry para uma tag
    // Se múltiplas threads pedirem a mesma tag, partilham a mesma Entry e fila
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
    // Recebe frame do socket, extrai tag, coloca dados na fila correta (Entry)
    // signalAll() acorda todas as threads à espera desta tag, mas só a primeira consome a mensagem
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
                // Socket fechou, propagar erro para todas as threads bloqueadas
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

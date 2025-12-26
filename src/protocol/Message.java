package protocol;

public class Message {
    // Definir aqui os tipos de mensagens para não ter de definir em cada lado no ServerWorker e ClientLib
    // Usar o static já que faz sentido querer saber estes valores sem instanciar a classe Message
    public static final byte REGISTAR = 1;
    public static final byte LOGIN = 2;

    public static final byte STATUS_SUCCESSO = 0;
    public static final byte STATUS_FALHOU = 1;
    public static final byte STATUS_NAO_AUTENTICADO = 2; 

    //fase de registo de eventos
    public static final byte ADICIONAR_EVENTO = 3;
    public static final byte NOVO_DIA = 4;  // Admin only (depois controlas)

    //para a fase de de agregação e cache
    public static final byte QUANTIDADE_VENDAS = 5;
    public static final byte VOLUME_VENDAS = 6;
    public static final byte PRECO_MEDIO = 7;
    public static final byte PRECO_MAXIMO = 8;

    //fase de filtrar eventos de series temporais
    public static final byte FILTRAR_EVENTOS = 9;


    //fase de notificação ocorrências, a que temos barreira
    public static final byte VENDAS_SIMULTANEAS = 10;
    public static final byte VENDAS_CONSECUTIVAS = 11;


    
}

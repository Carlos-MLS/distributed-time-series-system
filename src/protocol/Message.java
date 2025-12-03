package protocol;

public class Message {
    // Definir aqui os tipos de mensagens para não ter de definir em cada lado no ServerWorker e ClientLib
    // Usar o static já que faz sentido querer saber estes valores sem instanciar a classe Message
    public static final byte REGISTAR = 1;
    public static final byte LOGIN = 2;

    public static final byte STATUS_SUCCESSO = 0;
    public static final byte STATUS_FALHOU = 1;
    public static final byte STATUS_NAO_AUTENTICADO = 2; 

    public static final byte ADICIONAR_EVENTO = 3;
    public static final byte NOVO_DIA = 4;  // Admin only (depois controlas)

    
}

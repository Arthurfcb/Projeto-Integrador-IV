package com.petcare;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bson.Document;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;

/**
 * Servidor WebSocket para gerenciamento de agendamentos de uma petshop
 * Responsável por:
 * 1. Manter conexões WebSocket com clientes
 * 2. Verificar agendamentos periodicamente no MongoDB
 * 3. Enviar notificações para agendamentos próximos (24h e 1h antes)
 */
public class AppointmentServer extends WebSocketServer {
    // Configurações do servidor
    private static final int PORT = 12345;  // Porta do servidor WebSocket
    private Set<WebSocket> connections = new HashSet<>();  // Conjunto de conexões ativas
    private MongoClient mongoClient;  // Cliente MongoDB
    private MongoDatabase database;  // Referência ao banco de dados
    private Timer timer;  // Timer para verificação periódica

    /**
     * Configuração inicial do logging
     * Desativa logs desnecessários do MongoDB e configura nível global
     */
    static {
        LogManager.getLogManager().reset();
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.WARNING);
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
    }

    /**
     * Construtor do servidor
     * Inicializa o WebSocket e estabelece conexão com MongoDB
     */
    public AppointmentServer() {
        super(new InetSocketAddress(PORT));
        initializeMongoConnection();
    }

    /**
     * Inicializa conexão com MongoDB
     * Configura parâmetros de conexão, SSL, timeouts e retry policies
     * @throws RuntimeException se falhar ao conectar
     */
    private void initializeMongoConnection() {
        // String de conexão do MongoDB Atlas
        String connectionString = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority";
        
        // Configurações do cliente MongoDB
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .applyToSslSettings(builder -> builder.enabled(true).invalidHostNameAllowed(true))
            .applyToSocketSettings(builder -> builder.connectTimeout(30000, TimeUnit.MILLISECONDS).readTimeout(30000, TimeUnit.MILLISECONDS))
            .applyToClusterSettings(builder -> builder.serverSelectionTimeout(30000, TimeUnit.MILLISECONDS).mode(ClusterConnectionMode.MULTIPLE))
            .retryWrites(true).retryReads(true).build();

        try {
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("Registros");
            database.runCommand(new Document("ping", 1));  // Testa conexão
            System.out.println("Conexão MongoDB estabelecida!");
        } catch (Exception e) {
            throw new RuntimeException("Falha ao conectar ao MongoDB", e);
        }
    }

    /**
     * Verifica agendamentos no banco de dados
     * Executado periodicamente pelo Timer
     * Processa cada agendamento e envia notificações quando necessário
     */
    private void checkAppointments() {
        try {
            MongoCollection<Document> appointments = database.getCollection("Agendamentos");
            LocalDateTime now = LocalDateTime.now();
            System.out.println("\n=== Verificando agendamentos em: " + now + " ===");
            
            // Query para buscar todos os agendamentos
            Document query = new Document("data", new Document("$exists", true));
            long count = appointments.countDocuments(query);
            System.out.println("Total de agendamentos encontrados: " + count);
            
            // Processa cada agendamento
            appointments.find(query).forEach(doc -> {
                try {
                    System.out.println("\nProcessando agendamento: " + doc.toJson());
                    
                    // Extrai informações do agendamento
                    String dataStr = doc.getString("data");
                    String horaStr = doc.getString("horario");
                    LocalDateTime appointmentDateTime = LocalDateTime.parse(dataStr + "T" + horaStr);
                    
                    System.out.println("Data: " + dataStr + ", Hora: " + horaStr);
                    
                    // Calcula tempo até o agendamento
                    long horasAte = ChronoUnit.HOURS.between(now, appointmentDateTime);
                    double minutosAte = ChronoUnit.MINUTES.between(now, appointmentDateTime) / 60.0;
                    
                    System.out.println(String.format("Horas até o agendamento: %d (%.2f horas)", horasAte, minutosAte));
                    
                    // Verifica se está no momento de notificar (24h ou 1h antes)
                    if ((minutosAte >= 23.0 && minutosAte <= 24.5) || (horasAte >= 0 && horasAte <= 1)) {
                        System.out.println("Dentro da janela de notificação: " + 
                            (minutosAte >= 23.0 ? "24 horas" : "1 hora"));
                            
                        // Cria mensagem de notificação
                        String message = String.format("%s: %s para o agendamento!\nCliente: %s\nPet: %s\nData: %s às %s\nDescrição: %s",
                            horasAte <= 1 ? "ATENÇÃO" : "Lembrete",
                            horasAte <= 1 ? "menos de 1 hora" : "24 horas",
                            doc.getString("cliente"),
                            doc.getString("nome_pet"),
                            dataStr.replace("-", "/"),
                            horaStr,
                            doc.getString("descricao"));
                            
                        System.out.println("Enviando notificação: " + message);
                        broadcast(message);
                        
                        // Atualiza registro da última notificação
                        String tipoNotificacao = minutosAte >= 23.0 ? "24horas" : "1hora";
                        appointments.updateOne(
                            new Document("_id", doc.getObjectId("_id")),
                            new Document("$set", new Document("ultima_notificacao", 
                                new Document("data_hora", now.toString())
                                    .append("tipo", tipoNotificacao)))
                        );
                        System.out.println("Notificação registrada: " + tipoNotificacao);
                    } else {
                        System.out.println("Fora do período de notificação (nem 24h nem 1h antes)");
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar agendamento: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método chamado quando o servidor é iniciado
     * Inicia o timer para verificação periódica dos agendamentos
     */
    @Override
    public void onStart() {
        System.out.println("Servidor iniciado na porta " + getPort());
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAppointments();
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));  // Verifica a cada minuto
    }

    /**
     * Envia mensagem para todos os clientes conectados
     * @param message Mensagem a ser enviada
     */
    public void broadcast(String message) {
        int totalConexoes = connections.size();
        System.out.println("Tentando enviar broadcast para " + totalConexoes + " conexões");
        
        connections.removeIf(conn -> conn == null || !conn.isOpen());  // Remove conexões inválidas
        connections.forEach(conn -> conn.send(message));  // Envia para todas conexões
        
        System.out.println("Broadcast enviado para " + connections.size() + " conexões");
    }

    // Métodos de callback do WebSocket

    /**
     * Chamado quando um novo cliente se conecta
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Nova conexão de " + conn.getRemoteSocketAddress());
        System.out.println("Total de conexões ativas: " + connections.size());
    }

    /**
     * Chamado quando um cliente se desconecta
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Conexão fechada de " + conn.getRemoteSocketAddress());
        System.out.println("Total de conexões ativas: " + connections.size());
    }

    /**
     * Chamado quando uma mensagem é recebida de um cliente
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensagem recebida de " + conn.getRemoteSocketAddress() + ": " + message);
    }

    /**
     * Chamado quando ocorre um erro em uma conexão
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erro na conexão com " + (conn != null ? conn.getRemoteSocketAddress() : "desconhecido"));
        if (conn != null) {
            connections.remove(conn);
            System.out.println("Total de conexões ativas: " + connections.size());
        }
        ex.printStackTrace();
    }

    /**
     * Método principal que inicia o servidor
     */
    public static void main(String[] args) {
        new AppointmentServer().start();
    }
}
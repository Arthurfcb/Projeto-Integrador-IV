package com.petcare;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Base64;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Servidor de agendamentos para petshop que gerencia conexões de clientes e monitora agendamentos
 * Implementa o padrão accept -> socket -> thread para manipulação de conexões TCP/IP
 */
public class AppointmentServer {
    // Configurações do servidor
    private static final int PORT = 12345;                              // Porta de escuta do servidor
    private final ServerSocket serverSocket;                            // Socket servidor para aceitar conexões
    private final ExecutorService threadPool;                          // Pool de threads para gerenciar conexões
    private final Set<WebSocketClientHandler> clients;                  // Conjunto de clientes conectados
    private MongoClient mongoClient;                                   // Cliente MongoDB
    private MongoDatabase database;                                    // Banco de dados MongoDB
    private volatile boolean running;                                  // Flag para controle do servidor
    private Timer appointmentChecker;                                  // Timer para verificação de agendamentos
    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * Configuração inicial do logging
     * Desativa logs desnecessários e configura nível de log global
     */
    static {
        LogManager.getLogManager().reset();
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.WARNING);
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
    }

    /**
     * Construtor do servidor
     * Inicializa componentes básicos e estabelece conexão com MongoDB
     * @throws IOException se houver erro na criação do ServerSocket
     */
    public AppointmentServer() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.threadPool = Executors.newCachedThreadPool();
        this.clients = Collections.synchronizedSet(new HashSet<>());
        initializeMongoConnection();
    }

    /**
     * Inicializa a conexão com o MongoDB
     * Configura parâmetros de conexão, SSL, timeouts e políticas de retry
     * @throws RuntimeException se falhar ao conectar
     */
    private void initializeMongoConnection() {
        String connectionString = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority";

        // Configurações do cliente MongoDB
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .applyToSslSettings(builder -> builder.enabled(true).invalidHostNameAllowed(true))
            .applyToSocketSettings(builder -> builder.connectTimeout(30000, TimeUnit.MILLISECONDS))
            .applyToClusterSettings(builder -> builder.serverSelectionTimeout(30000, TimeUnit.MILLISECONDS))
            .retryWrites(true)
            .retryReads(true)
            .build();

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
     * Inicia o servidor e começa a aceitar conexões
     * Implementa o loop principal de accept -> socket -> thread
     */
    public void start() {
        running = true;
        startAppointmentChecker();

        System.out.println("Servidor iniciado na porta " + PORT);

        // Loop principal de aceitação de conexões
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();  // Bloqueia até receber conexão
                WebSocketClientHandler clientHandler = new WebSocketClientHandler(clientSocket);
                clients.add(clientHandler);
                threadPool.execute(clientHandler);  // Delega conexão para uma thread do pool
                System.out.println("Nova conexão aceita: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Inicia o timer que verifica periodicamente os agendamentos
     * Executa a cada minuto para identificar agendamentos próximos
     */
    private void startAppointmentChecker() {
        appointmentChecker = new Timer();
        appointmentChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAppointments();
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));  // Executa a cada minuto
    }

    /**
     * Verifica agendamentos no MongoDB
     * Identifica agendamentos próximos e envia notificações quando necessário
     */
    private void checkAppointments() {
        try {
            MongoCollection<Document> appointments = database.getCollection("Agendamentos");
            LocalDateTime now = LocalDateTime.now();

            // Busca todos os agendamentos
            Document query = new Document("data", new Document("$exists", true));
            appointments.find(query).forEach(doc -> {
                try {
                    // Extrai e processa dados do agendamento
                    String dataStr = doc.getString("data");
                    String horaStr = doc.getString("horario");
                    LocalDateTime appointmentDateTime = LocalDateTime.parse(dataStr + "T" + horaStr);

                    // Calcula tempo até o agendamento
                    long horasAte = ChronoUnit.HOURS.between(now, appointmentDateTime);
                    double minutosAte = ChronoUnit.MINUTES.between(now, appointmentDateTime) / 60.0;

                    // Verifica se está no momento de notificar (24h ou 1h antes)
                    if ((minutosAte >= 23.0 && minutosAte <= 24.5) || (horasAte >= 0 && horasAte <= 1)) {
                        String message = formatNotificationMessage(doc, horasAte, dataStr, horaStr);
                        broadcast(message);

                        updateNotificationStatus(appointments, doc, now, minutosAte);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar agendamento: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
        }
    }

    /**
     * Formata a mensagem de notificação com base nos dados do agendamento
     * @param doc Documento do MongoDB com dados do agendamento
     * @param horasAte Horas até o agendamento
     * @param dataStr Data do agendamento
     * @param horaStr Hora do agendamento
     * @return Mensagem formatada
     */
    private String formatNotificationMessage(Document doc, long horasAte, String dataStr, String horaStr) {
        return String.format("%s: %s para o agendamento!\nCliente: %s\nPet: %s\nData: %s às %s\nDescrição: %s",
            horasAte <= 1 ? "ATENÇÃO" : "Lembrete",
            horasAte <= 1 ? "menos de 1 hora" : "24 horas",
            doc.getString("cliente"),
            doc.getString("nome_pet"),
            dataStr.replace("-", "/"),
            horaStr,
            doc.getString("descricao"));
    }

    /**
     * Atualiza o status da notificação no MongoDB
     * @param appointments Coleção de agendamentos
     * @param doc Documento do agendamento
     * @param now Data/hora atual
     * @param minutosAte Minutos até o agendamento
     */
    private void updateNotificationStatus(MongoCollection<Document> appointments, Document doc, 
                                       LocalDateTime now, double minutosAte) {
        String tipoNotificacao = minutosAte >= 23.0 ? "24horas" : "1hora";
        appointments.updateOne(
            new Document("_id", doc.getObjectId("_id")),
            new Document("$set", new Document("ultima_notificacao", 
                new Document("data_hora", now.toString())
                    .append("tipo", tipoNotificacao)))
        );
    }

    /**
     * Envia mensagem para todos os clientes conectados
     * Remove clientes desconectados durante o processo
     * @param message Mensagem a ser enviada
     */
    private void broadcast(String message) {
        synchronized(clients) {
            clients.removeIf(client -> !client.isConnected());
            for (WebSocketClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Para o servidor e libera recursos
     * Fecha conexões, cancela timer e finaliza thread pool
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (appointmentChecker != null) {
                appointmentChecker.cancel();
            }
            if (threadPool != null) {
                threadPool.shutdown();
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        } catch (Exception e) {
            System.err.println("Erro ao parar servidor: " + e.getMessage());
        }
    }

    /**
     * Classe interna que gerencia uma conexão individual com cliente
     * Cada instância roda em uma thread separada do pool
     */
    private class WebSocketClientHandler implements Runnable {
        private final Socket socket;                    // Socket do cliente
        private InputStream in;                         // Stream de entrada
        private OutputStream out;                       // Stream de saída
        private volatile boolean connected;             // Status da conexão
        private boolean isWebSocketConnected;

        /**
         * Construtor do handler de cliente
         * @param socket Socket do cliente conectado
         * @throws IOException se erro ao criar streams
         */
        public WebSocketClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.connected = true;
            this.isWebSocketConnected = false;
        }

        /**
         * Método principal da thread
         * Loop de leitura de mensagens do cliente
         */
        @Override
        public void run() {
            try {
                if (handleWebSocketHandshake()) {
                    handleWebSocketCommunication();
                }
            } catch (IOException e) {
                System.err.println("Erro na comunicação com cliente: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        /**
         * Trata o handshake do WebSocket
         * @return true se handshake bem-sucedido, false caso contrário
         * @throws IOException se erro na leitura ou escrita
         */
        private boolean handleWebSocketHandshake() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            String key = null;
            boolean isWebSocketRequest = false;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.contains("Upgrade: websocket")) {
                    isWebSocketRequest = true;
                }
                if (line.startsWith("Sec-WebSocket-Key: ")) {
                    key = line.substring(19).trim();
                }
            }

            if (isWebSocketRequest && key != null) {
                String acceptKey = generateAcceptKey(key);
                String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                                "Upgrade: websocket\r\n" +
                                "Connection: Upgrade\r\n" +
                                "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

                out.write(response.getBytes());
                out.flush();
                isWebSocketConnected = true;
                return true;
            }
            return false;
        }

        /**
         * Trata a comunicação do WebSocket
         * @throws IOException se erro na leitura ou escrita
         */
        private void handleWebSocketCommunication() throws IOException {
            byte[] buffer = new byte[8192];
            int len;

            while (connected && (len = in.read(buffer)) != -1) {
                if (len < 2) continue;

                boolean fin = (buffer[0] & 0x80) != 0;
                int opcode = buffer[0] & 0x0F;
                boolean masked = (buffer[1] & 0x80) != 0;
                int payloadLength = buffer[1] & 0x7F;
                int offset = 2;

                if (payloadLength == 126) {
                    payloadLength = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                    offset = 4;
                } else if (payloadLength == 127) {
                    // Não implementado para mensagens muito grandes
                    continue;
                }

                byte[] mask = new byte[4];
                if (masked) {
                    System.arraycopy(buffer, offset, mask, 0, 4);
                    offset += 4;
                }

                byte[] payload = new byte[payloadLength];
                System.arraycopy(buffer, offset, payload, 0, payloadLength);

                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ mask[i % 4]);
                    }
                }

                if (opcode == 8) { // Close frame
                    disconnect();
                    break;
                } else if (opcode == 1) { // Text frame
                    String message = new String(payload, StandardCharsets.UTF_8);
                    System.out.println("Mensagem recebida: " + message);
                }
            }
        }

        /**
         * Gera a chave de aceite do WebSocket
         * @param key Chave do cliente
         * @return Chave de aceite
         */
        private String generateAcceptKey(String key) {
            try {
                String concatenated = key + WEBSOCKET_MAGIC_STRING;
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Envia mensagem para o cliente
         * @param message Mensagem a ser enviada
         */
        public void sendMessage(String message) {
            if (!connected || !isWebSocketConnected) return;

            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                int length = payload.length;

                ByteArrayOutputStream frame = new ByteArrayOutputStream();
                frame.write(0x81); // FIN + Text frame

                if (length <= 125) {
                    frame.write(length);
                } else if (length <= 65535) {
                    frame.write(126);
                    frame.write((length >> 8) & 0xFF);
                    frame.write(length & 0xFF);
                } else {
                    frame.write(127);
                    for (int i = 7; i >= 0; i--) {
                        frame.write((int)((length >> (8 * i)) & 0xFF));
                    }
                }

                frame.write(payload);
                out.write(frame.toByteArray());
                out.flush();
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem: " + e.getMessage());
                disconnect();
            }
        }

        /**
         * Verifica se cliente ainda está conectado
         * @return true se conectado, false caso contrário
         */
        public boolean isConnected() {
            return connected && !socket.isClosed() && isWebSocketConnected;
        }

        /**
         * Desconecta o cliente e libera recursos
         * Remove cliente da lista de conexões ativas
         */
        private void disconnect() {
            connected = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            } finally {
                clients.remove(this);
            }
        }
    }

    /**
     * Método principal que inicia o servidor
     * @param args argumentos da linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        try {
            AppointmentServer server = new AppointmentServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }
}
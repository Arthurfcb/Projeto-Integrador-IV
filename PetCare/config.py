import os
from datetime import timedelta

class Config:
    SECRET_KEY = os.urandom(24)
    MONGO_URI = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority&appName=Registros"
    
    # Configurações de timeout do MongoDB
    MONGO_CONNECT_TIMEOUT_MS = 5000
    MONGO_SOCKET_TIMEOUT_MS = 5000
    MONGO_SERVER_SELECTION_TIMEOUT_MS = 5000
    
    # Configurações de sessão
    PERMANENT_SESSION_LIFETIME = timedelta(hours=1)
    SESSION_COOKIE_SECURE = True
    SESSION_COOKIE_HTTPONLY = True
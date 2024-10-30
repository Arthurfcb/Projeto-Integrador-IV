import os

class Config:
    SECRET_KEY = os.urandom(24)  # Necessário para proteção dos formulários
    MONGO_URI = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority&appName=Registros"

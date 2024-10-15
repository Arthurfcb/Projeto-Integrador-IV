from pymongo import MongoClient

# Substitua a URI abaixo pela sua URI de conexão
MONGO_URI = "mongodb+srv://LuigiTarga:30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority&appName=Registros"

try:

    client = MongoClient(MONGO_URI)
    db = client['Registros']  # Nome do banco
    print("Conexão estabelecida com sucesso!")
except Exception as e:
    print(f"Erro ao conectar: {e}")

from flask import Flask, render_template, request, redirect, url_for
from flask_pymongo import PyMongo
from config import Config

app = Flask(__name__)
app.config.from_object(Config)

# Conectar ao MongoDB
mongo = PyMongo(app)
client = mongo.cx  # Usa o cliente do PyMongo
db = client['Registros']  # Nome do cluster
users_collection = db['RegistrosCliente']  # Nome da coleção


@app.route('/')
def index():
    return "Bem-vindo à aplicação de Cadastro!"


# Rota de Cadastro
@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        # Inserir o novo usuário na coleção 'RegistrosCliente'
        users_collection.insert_one({'username': username, 'password': password})
        return redirect(url_for('login'))
    return render_template('register.html')


# Rota de Login
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        # Verificar usuário na coleção 'RegistrosCliente'
        user = users_collection.find_one({'username': username, 'password': password})
        if user:
            return f"Bem-vindo, {username}!"
        else:
            return "Login inválido!"
    return render_template('login.html')


# Rota de Teste de Conexão
@app.route('/test_connection')
def test_connection():

    # Tente inserir um usuário de teste
    test_user = {'username': 'teste', 'password': 'teste123'}
    users_collection.insert_one(test_user)

    # Listar todos os usuários da coleção 'RegistrosCliente'
    users = users_collection.find()
    user_list = [user['username'] for user in users]  # Cria uma lista com os nomes de usuário

    return f"Usuários cadastrados: {', '.join(user_list)}"


# Rota para limpar dados de teste (opcional)
@app.route('/clear_test_data')
def clear_test_data():
    users_collection.delete_one({'username': 'teste'})  # Remove o usuário de teste
    return "Dados de teste limpos!"


if __name__ == '__main__':
    app.run(debug=True)

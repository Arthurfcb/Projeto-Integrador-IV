from flask import Flask, render_template, request, redirect, url_for, session
from flask_pymongo import PyMongo
from config import Config

app = Flask(__name__)
app.config.from_object(Config)

# Configurar uma chave secreta para as sessões
app.secret_key = 'chave_super_secreta'

# Conectar ao MongoDB
mongo = PyMongo(app)
client = mongo.cx  # Usa o cliente do PyMongo
db = client['Registros']  # Nome do cluster
users_collection = db['RegistrosCliente']  # Nome da coleção

# Rota de Login
@app.route('/login', methods=['GET', 'POST'])
def login():
    error_message = None  # Variável para armazenar a mensagem de erro
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        # Verificar usuário na coleção 'RegistrosCliente'
        user = users_collection.find_one({'username': username, 'password': password})
        if user:
            session['username'] = username  # Armazena o usuário na sessão
            return redirect(url_for('main'))  # Redireciona para a página principal
        else:
            error_message = "Nome de usuário ou senha incorretos. Por favor, tente novamente."  # Mensagem de erro

    # Renderiza a página de login com a mensagem de erro, se houver
    return render_template('login.html', error_message=error_message)

# Rota para a tela principal
@app.route('/')
def main():
    # Verificar se o usuário está logado
    if 'username' in session:
        return render_template('main.html')
    else:
        return redirect(url_for('login'))  # Se não estiver logado, redirecionar para o login

# Rota para o prontuário
@app.route('/prontuario')
def prontuario():
    if 'username' in session:  # Verificar se o usuário está logado
        return "Aqui será exibido o prontuário"
    else:
        return redirect(url_for('login'))  # Redireciona se não estiver logado

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

# Rota de logout para encerrar a sessão
@app.route('/logout')
def logout():
    session.pop('username', None)  # Remove o usuário da sessão
    return redirect(url_for('login'))  # Redireciona para o login

if __name__ == '__main__':
    app.run(debug=True)

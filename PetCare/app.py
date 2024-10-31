from flask import Flask, render_template, request, redirect, url_for, session, flash
from flask_pymongo import PyMongo
from config import Config
from bson.objectid import ObjectId  # Importar ObjectId

app = Flask(__name__)
app.config.from_object(Config)

# Configurar uma chave secreta para as sessões
app.secret_key = 'chave_super_secreta'

# Conectar ao MongoDB
mongo = PyMongo(app)
client = mongo.cx
db = client['Registros']
users_collection = db['RegistrosCliente']

# Rota de Login
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        # Verificar usuário na coleção 'RegistrosCliente'
        user = users_collection.find_one({'username': username, 'password': password})
        if user:
            session['username'] = username  # Armazena o usuário na sessão
            return redirect(url_for('main'))  # Redireciona para a página principal
        else:
            return render_template('login.html', error="Login inválido!")
    return render_template('login.html')

# Rota para a tela principal
@app.route('/')
def main():
    if 'username' in session:
        return render_template('main.html')  # Tela principal que dá acesso às outras
    else:
        return redirect(url_for('login'))

# Rota para cadastrar prontuário
@app.route('/prontuario', methods=['GET', 'POST'])
def prontuario():
    if 'username' in session:
        if request.method == 'POST':
            nome_cliente = request.form['nome_cliente']
            nome_pet = request.form['nome_pet']
            alergias = request.form['alergias']
            medicamentos = request.form['medicamentos']
            condicoes = request.form['condicoes']
            veterinario = request.form['veterinario']
            data = request.form['data']

            prontuario_data = {
                'nome_cliente': nome_cliente,
                'nome_pet': nome_pet,
                'alergias': alergias,
                'medicamentos': medicamentos,
                'condicoes': condicoes,
                'veterinario': veterinario,
                'data': data
            }

            try:
                # Insere o prontuário no MongoDB
                db['Prontuarios'].insert_one(prontuario_data)
                mensagem = "Prontuário cadastrado com sucesso!"  # Mensagem de sucesso
                mensagem_tipo = "success"
            except Exception as e:
                mensagem = f"Erro ao cadastrar prontuário: {str(e)}"  # Mensagem de erro
                mensagem_tipo = "error"

            return render_template('prontuario.html', mensagem=mensagem, mensagem_tipo=mensagem_tipo)

        return render_template('prontuario.html')
    else:
        return redirect(url_for('login'))

# Rota para procurar prontuário por cliente e pet
@app.route('/procurar_prontuario', methods=['GET', 'POST'])
def procurar_prontuario():
    if 'username' in session:
        if request.method == 'POST':
            nome_cliente = request.form['nome_cliente']
            nome_pet = request.form['nome_pet']

            # Procura o prontuário no MongoDB
            prontuario = db['Prontuarios'].find_one({'nome_cliente': nome_cliente, 'nome_pet': nome_pet})

            if prontuario:
                return render_template('procurar_prontuario.html', prontuario=prontuario)
            else:
                mensagem = "Prontuário não encontrado para este cliente e pet."
                return render_template('procurar_prontuario.html', mensagem=mensagem)

        return render_template('procurar_prontuario.html')
    else:
        return redirect(url_for('login'))

# Rota para deletar prontuário
@app.route('/deletar_prontuario/<prontuario_id>', methods=['POST'])
def deletar_prontuario(prontuario_id):
    if 'username' in session:
        try:
            db['Prontuarios'].delete_one({'_id': ObjectId(prontuario_id)})
            flash("Prontuário deletado com sucesso!", "success")  # Mensagem de sucesso
        except Exception as e:
            flash(f"Erro ao deletar prontuário: {str(e)}", "error")  # Mensagem de erro
        return redirect(url_for('procurar_prontuario'))  # Redireciona de volta para a página de procura de prontuário
    else:
        return redirect(url_for('login'))

# Rota para cadastrar cliente
@app.route('/cadastrar_cliente')
def cadastrar_cliente():
    if 'username' in session:
        return render_template('cadastrar_cliente.html')  # Renderiza a página de cadastro de cliente
    else:
        return redirect(url_for('login'))


# Rota para agendamentos
@app.route('/agendamentos')
def agendamentos():
    if 'username' in session:
        return render_template('agendamento.html')  # Rota para a página de agendamento
    else:
        return redirect(url_for('login'))

# Rota para agendamentos antigos
@app.route('/agendamentos_antigos')
def agendamentos_antigos():
    if 'username' in session:
        # Recupera todos os agendamentos da coleção 'Agendamentos'
        agendamentos = list(db['Agendamentos'].find())  # Busca todos os agendamentos
        for agendamento in agendamentos:
            print(agendamento['_id'])  # Para verificar os IDs
        return render_template('agendamento_antigo.html', agendamentos=agendamentos)  # Passa os agendamentos para o template
    else:
        return redirect(url_for('login'))

@app.route('/deletar_agendamento/<agendamento_id>', methods=['POST'])
def deletar_agendamento(agendamento_id):
    print(f"Tentando deletar agendamento com ID: {agendamento_id}")  # Para depuração
    if 'username' in session:
        try:
            db['Agendamentos'].delete_one({'_id': ObjectId(agendamento_id)})
            flash("Agendamento deletado com sucesso!", "success")  # Mensagem de sucesso
        except Exception as e:
            flash(f"Erro ao deletar agendamento: {str(e)}", "error")  # Mensagem de erro
        return redirect(url_for('agendamentos_antigos'))  # Redireciona de volta para a página de agendamentos antigos
    else:
        return redirect(url_for('login'))

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

# Rota para agendar uma consulta
@app.route('/agendar', methods=['POST'])
def agendar():
    if 'username' in session:
        data = request.form['date']
        horario = request.form['time']
        cliente = request.form['cliente']  # Nome do dono do pet
        nome_pet = request.form['nome_pet']  # Nome do pet
        descricao = request.form['description']

        agendamento_data = {
            'username': session['username'],
            'data': data,
            'horario': horario,
            'cliente': cliente,  # Nome do dono do pet
            'nome_pet': nome_pet,  # Adiciona o nome do pet
            'descricao': descricao
        }

        try:
            db['Agendamentos'].insert_one(agendamento_data)
            mensagem = "Agendamento realizado com sucesso!"
        except Exception as e:
            mensagem = f"Erro ao agendar: {str(e)}"

        return render_template('agendamento.html', mensagem=mensagem)
    else:
        return redirect(url_for('login'))

# Rota para cadastrar cliente
@app.route('/register_cliente', methods=['POST'])
def register_cliente():
    if 'username' in session:  # Verifica se o usuário está logado
        nome = request.form['nome']  # Obtém o nome do cliente
        nome_pet = request.form['nome_pet']  # Obtém o nome do pet
        email = request.form['email']  # Obtém o email do cliente

        # Dados a serem inseridos no MongoDB
        cliente_data = {
            'nome': nome,
            'nome_pet': nome_pet,
            'email': email
        }

        try:
            # Inserir os dados na coleção 'RegistrosClientela'
            db['RegistrosClientela'].insert_one(cliente_data)
            flash("Cliente cadastrado com sucesso!", "success")  # Mensagem de sucesso
        except Exception as e:
            flash(f"Erro ao cadastrar cliente: {str(e)}", "error")  # Mensagem de erro

        return redirect(url_for('cadastrar_cliente'))  # Redireciona de volta para a página de cadastro
    else:
        return redirect(url_for('login'))

# Rota para consultar a clientela
@app.route('/consultar_clientela')
def consultar_clientela():
    if 'username' in session:
        # Recupera todos os clientes da coleção 'RegistrosClientela'
        clientela = list(db['RegistrosClientela'].find())  # Busca todos os clientes
        return render_template('clientela.html', clientela=clientela)  # Passa os clientes para o template
    else:
        return redirect(url_for('login'))

# Rota para deletar cliente
@app.route('/deletar_cliente/<cliente_id>', methods=['POST'])
def deletar_cliente(cliente_id):
    if 'username' in session:
        try:
            db['RegistrosClientela'].delete_one({'_id': ObjectId(cliente_id)})
            flash("Cliente deletado com sucesso!", "success")  # Mensagem de sucesso
        except Exception as e:
            flash(f"Erro ao deletar cliente: {str(e)}", "error")  # Mensagem de erro
        return redirect(url_for('consultar_clientela'))  # Redireciona de volta para a página de consulta de clientela
    else:
        return redirect(url_for('login'))



# Rota de logout para encerrar a sessão
@app.route('/logout')
def logout():
    session.pop('username', None)  # Remove o usuário da sessão
    return redirect(url_for('login'))  # Redireciona para o login

if __name__ == '__main__':
    app.run(debug=True)

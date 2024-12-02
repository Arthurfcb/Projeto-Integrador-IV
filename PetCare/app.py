"""
PetCare - Sistema de Gerenciamento de Clínica Veterinária
Este aplicativo Flask fornece funcionalidades para gerenciar uma clínica veterinária,
incluindo cadastro de clientes, prontuários, agendamentos e autenticação de usuários.
"""

from flask import Flask, render_template, request, redirect, url_for, session, flash
from flask_pymongo import PyMongo
from config import Config
from bson.objectid import ObjectId  # Importar ObjectId
from datetime import datetime, timedelta
import logging
import sys

app = Flask(__name__)
app.config.from_object(Config)

# Configurar uma chave secreta para as sessões
app.secret_key = 'chave_super_secreta'

# Conectar ao MongoDB
mongo = PyMongo(app)
client = mongo.cx
db = client['Registros']
users_collection = db['RegistrosCliente']

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

# Rota de Login
@app.route('/login', methods=['GET', 'POST'])
def login():
    """
    Rota de autenticação de usuários.
    GET: Exibe o formulário de login
    POST: Processa a tentativa de login
    Verifica as credenciais no banco de dados e cria uma sessão para o usuário.
    """
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
    """
    Rota principal do aplicativo.
    Requer autenticação.
    Renderiza a página principal que dá acesso a todas as funcionalidades do sistema.
    """
    if 'username' in session:
        return render_template('main.html')  # Tela principal que dá acesso às outras
    else:
        return redirect(url_for('login'))

# Rota para cadastrar prontuário
@app.route('/prontuario', methods=['GET', 'POST'])
def prontuario():
    """
    Gerenciamento de prontuários médicos.
    GET: Exibe o formulário de cadastro de prontuário
    POST: Registra um novo prontuário com informações do pet e do cliente
    Campos: nome do cliente, nome do pet, alergias, medicamentos, condições e veterinário responsável
    """
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
    """
    Sistema de busca de prontuários.
    GET: Exibe o formulário de busca
    POST: Busca prontuário específico por nome do cliente e do pet
    Exibe todas as informações médicas registradas para o pet
    """
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
    """
    Remove um prontuário específico do sistema.
    Requer ID do prontuário.
    Apenas usuários autenticados podem deletar prontuários.
    """
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
    """
    Interface para cadastro de novos clientes.
    Requer autenticação.
    Renderiza o formulário de cadastro de cliente.
    """
    if 'username' in session:
        return render_template('cadastrar_cliente.html')  # Renderiza a página de cadastro de cliente
    else:
        return redirect(url_for('login'))

# Rota para agendamentos
@app.route('/agendamentos')
def agendamentos():
    """
    Gerenciamento de agendamentos.
    Exibe interface para criar novos agendamentos.
    Requer autenticação.
    """
    if 'username' in session:
        return render_template('agendamento.html')
    else:
        return redirect(url_for('login'))

# Rota para agendamentos antigos
@app.route('/agendamentos_antigos')
def agendamentos_antigos():
    """
    Visualização de histórico de agendamentos.
    Lista todos os agendamentos registrados no sistema.
    Permite gerenciar agendamentos existentes.
    """
    if 'username' in session:
        print("\n[INFO] Tentando acessar agendamentos antigos...")
        try:
            agendamentos = list(db['Agendamentos'].find())
            print("Erro ao verificar agendamentos:")
            return render_template('agendamento_antigo.html', agendamentos=agendamentos)
        except Exception as e:
            print(f"\n[ERRO] Não foi possível conectar ao banco de dados: {str(e)}\n")
            return render_template('agendamento_antigo.html', agendamentos=None)
    return redirect(url_for('login'))

@app.route('/deletar_agendamento/<agendamento_id>', methods=['POST'])
def deletar_agendamento(agendamento_id):
    """
    Remove um agendamento específico.
    Requer ID do agendamento.
    Apenas usuários autenticados podem deletar agendamentos.
    """
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
    """
    Registro de novos usuários no sistema.
    GET: Exibe formulário de registro
    POST: Cria novo usuário no banco de dados
    """
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
    """
    Criação de novos agendamentos.
    Valida data e hora do agendamento.
    Registra informações do cliente, pet e descrição da consulta.
    Impede agendamentos retroativos e em horários já ocupados.
    """
    if 'username' in session:
        data = request.form['date']
        horario = request.form['time']
        cliente = request.form['cliente']
        nome_pet = request.form['nome_pet']
        descricao = request.form['description']

        try:
            # Converte a data e hora do agendamento para um objeto datetime
            data_hora_agendamento = datetime.strptime(f"{data} {horario}", "%Y-%m-%d %H:%M")
            
            # Obtém a data/hora atual
            agora = datetime.now()

            # Verifica se a data é retroativa
            if data_hora_agendamento < agora:
                return render_template('agendamento.html', 
                    mensagem="Erro: Não é possível agendar para uma data/hora que já passou!")

            # Aqui você pode adicionar outras validações se necessário
            # Por exemplo, verificar se o horário está dentro do horário de funcionamento
            hora = int(horario.split(':')[0])
            if hora < 8 or hora >= 18:
                return render_template('agendamento.html', 
                    mensagem="Erro: Os agendamentos só podem ser feitos entre 8h e 18h!")

            agendamento_data = {
                'username': session['username'],
                'data': data,
                'horario': horario,
                'cliente': cliente,
                'nome_pet': nome_pet,
                'descricao': descricao,
                'data_criacao': agora
            }

            db['Agendamentos'].insert_one(agendamento_data)
            mensagem = "Agendamento realizado com sucesso!"

        except ValueError:
            mensagem = "Erro: Data ou hora em formato inválido!"
        except Exception as e:
            mensagem = f"Erro ao agendar: {str(e)}"

        return render_template('agendamento.html', mensagem=mensagem)
    else:
        return redirect(url_for('login'))

# Rota para cadastrar cliente
@app.route('/register_cliente', methods=['POST'])
def register_cliente():
    """
    Registra um novo cliente no sistema.
    Armazena informações como nome, contato e dados do pet.
    Requer autenticação.
    """
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
    """
    Lista todos os clientes cadastrados.
    Exibe informações básicas de cada cliente.
    Requer autenticação.
    """
    if 'username' in session:
        # Recupera todos os clientes da coleção 'RegistrosClientela'
        clientela = list(db['RegistrosClientela'].find())  # Busca todos os clientes
        return render_template('clientela.html', clientela=clientela)  # Passa os clientes para o template
    else:
        return redirect(url_for('login'))

# Rota para deletar cliente
@app.route('/deletar_cliente/<cliente_id>', methods=['POST'])
def deletar_cliente(cliente_id):
    """
    Remove um cliente do sistema.
    Requer ID do cliente.
    Apenas usuários autenticados podem deletar clientes.
    """
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
    """
    Encerra a sessão do usuário.
    Remove as informações de autenticação.
    Redireciona para a página de login.
    """
    session.pop('username', None)  # Remove o usuário da sessão
    return redirect(url_for('login'))  # Redireciona para o login

if __name__ == '__main__':
    app.run(debug=True)

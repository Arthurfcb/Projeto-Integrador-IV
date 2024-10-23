from flask import Flask, render_template

app = Flask(__name__)

# Rota para a tela principal
@app.route('/')
def main():
    return render_template('main.html')

# Adicione outras rotas e funcionalidades do seu app abaixo
# Por exemplo, a rota para o prontuário:
@app.route('/prontuario')
def prontuario():
    return "Aqui será exibido o prontuário"

if __name__ == '__main__':
    app.run(debug=True)

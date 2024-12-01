# PetCare - Sistema de Gerenciamento Veterinário

Sistema desenvolvido para gerenciamento de clínica veterinária, incluindo agendamentos e notificações automáticas.

## Funcionalidades Principais

- Cadastro de clientes e pets
- Agendamento de consultas  
- Sistema de notificações automáticas (24h e 1h antes da consulta)
- Gerenciamento de prontuários
- Interface intuitiva e responsiva

## Pré-requisitos

- Python 3.8+
- Java 11+
- Maven
- MongoDB Atlas (conta configurada)
- Navegador moderno com suporte a notificações

## Configuração do Ambiente

1. Clone o repositório
git clone https://github.com/seu-usuario/Projeto-Integrador-IV.git
cd Projeto-Integrador-IV/PetCare

2. Configure o ambiente Python
# Criar ambiente virtual
python -m venv venv

# Ativar ambiente virtual
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Instalar dependências
pip install -r requirements.txt

3. Configure o MongoDB
- Acesse MongoDB Atlas (https://www.mongodb.com/cloud/atlas)
- Crie um novo cluster (ou use existente)
- Obtenha a string de conexão
- Atualize o arquivo config.py com suas credenciais

4. Compile o servidor de notificações
mvn clean && mvn package

## Executando o Sistema

1. Inicie o servidor de notificações
java -jar target/appointment-notifier-1.0-SNAPSHOT.jar

2. Inicie o servidor Flask (em outro terminal)
python app.py

3. Acesse o sistema
- Abra o navegador
- Acesse: http://localhost:5000
- Faça login com suas credenciais

## Sistema de Notificações

O sistema enviará notificações automáticas:
- 24 horas antes da consulta
- 1 hora antes da consulta

Para receber as notificações:
1. Permita notificações quando solicitado pelo navegador
2. Mantenha o navegador aberto
3. Certifique-se que o servidor de notificações está rodando

## Estrutura do Projeto
PetCare/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── petcare/
│                   └── AppointmentServer.java
├── templates/
│   ├── main.html
│   ├── agendamento.html
│   └── ...
├── app.py
├── config.py
├── pom.xml
└── requirements.txt

## Solução de Problemas

### Servidor Java não inicia
1. Verifique a instalação do Java:
java -version

2. Certifique-se que a porta 8887 está disponível

3. Recompile o projeto:
mvn clean && mvn package

### Notificações não aparecem
1. Verifique permissões do navegador
2. Abra o console do navegador (F12)
3. Confirme que o servidor Java está rodando
4. Verifique os logs no terminal

### Erro de conexão MongoDB
1. Verifique a string de conexão em config.py
2. Confirme se o IP está liberado no MongoDB Atlas
3. Teste as credenciais no MongoDB Compass

## Desenvolvimento

Para contribuir com o projeto:
1. Crie um fork
2. Desenvolva sua feature
3. Faça commit das alterações
4. Crie um Pull Request

## Suporte

Em caso de dúvidas ou problemas:
1. Verifique a seção de Solução de Problemas
2. Abra uma issue no GitHub
3. Entre em contato com a equipe de desenvolvimento

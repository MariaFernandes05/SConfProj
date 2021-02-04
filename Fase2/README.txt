
COMO EXECUTAR O PROJETO:

Primeiramente, compilamos com o comando make.
Para correr com os ficheiros de permissões:
MequieServer
java -Djava.security.manager -Djava.security.policy=MequieServer.policy MequieServer <port> <keystore> <keystore-password>

Mequie
java -Djava.security.manager -Djava.security.policy=Mequie.policy Mequie <ip:porto> <truststore> <keystore> <keystore-password> <localUserID>


LIMITACOES:

Photo:
    A funcao photo nao funciona, visto que nos nao conseguimos enviar o ficheiro de imagem sem dar erro 'Broken Pipe'.

Collect e History:
    Nao conseguimos arranjar maneira de guardar estado das mensagens ja lidas por cada user. Assim, consequentemente,
o comando collect mostra as mensagens todas, e por sua vez, o history, que so deveria mostrar as lidas por todos os
elementos, aparece vazia. 

    Na funcao colect, existe codigo em comentario relevante ah foto.






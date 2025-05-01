Este projeto se trata ca criação de um serviço de mensagens criptografadas usando a criptografia RSA (Rivest-Shamir-Adleman), que usa de chaves públicas para a encriptação e chaves privadas para a decriptação de mensagens.

O programa consiste de um servidor e dois (ou mais) clientes, onde os clientes podem selecionar um usuário (e sua chave pública correspondente) para enviar uma mensagem secreta, a qual apenas aquele usuário conseguirá ver o conteúdo. 

![print1](https://github.com/user-attachments/assets/f6d1784f-ce28-455e-b4e4-988103289f86)
![print2](https://github.com/user-attachments/assets/2b3b943e-9515-4079-8d3a-48a8e5a35d2a)
As imagens acima retratam o funcionamento do algoritmo, onde o cliente "Alice" envia uma mensagem criptografada com a chave pública de "Bob", e apenas Bob consegue ver o conteúdo através de sua chave privada, impossibilitando que até a própria Alice, ou um terceiro cliente "Eve" consiga ler a mensagem enviada.


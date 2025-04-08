/*
Grade: 19.6 out of 20 (SOLO PROJECT)
Sorting the board by adding one tent on the possibles spots and 
redoing that until one position works.
*/


% ========== START OF CODE ==========

 :- use_module(library(clpfd)). % para poder usar transpose/2
 :- set_prolog_flag(answer_write_options,[max_depth(0)]). % ver listas completas
 :- ['puzzlesAcampar.pl']. % Ficheiro dado. No Mooshak tera mais puzzles.

% junta num tuplo um numero com um membro da lista
juntaNumeros_aux(_,[],[]):- !.
juntaNumeros_aux(X,[H|T],[(X,H)|Lista]):- 
    juntaNumeros_aux(X,T,Lista).

 % numa linha encontra as posicoes da Letra
posicao_aux([], _, Posicoes, Posicoes, _):- !.
posicao_aux([H|T],Xatual,Listafinal,Posicoes,Letra):-
    
    (Letra==H -> append(Listafinal,[Xatual],Listafinal1), % verifica se o elemento da linha e igual a letra e se sim devolve poem a posicao na lista final
    Xnovo is Xatual +1,
    posicao_aux(T,Xnovo,Listafinal1,Posicoes,Letra); %continua ate a linha esta vazia
    Xnovo is Xatual+1,
    posicao_aux(T,Xnovo,Listafinal,Posicoes,Letra)).

 % o numero de uma lista menos outra e por numa nova lista
subtrairListas_aux([], [], []):- !.
subtrairListas_aux([H1|T1], [H2|T2], [Diferenca|Lista]) :-
    Diferenca is H1 - H2,
    subtrairListas_aux(T1, T2, Lista).

% devolve um lista de largura do tamanho com o tamanho em todas as posicoes
totalListas_aux(_,0,[]):- !.
totalListas_aux(TamanhodaLinha,LarguradaLista,[TamanhodaLinha|Lista]):- 
    LarguradaListaNovo is LarguradaLista -1,
    totalListas_aux(TamanhodaLinha,LarguradaListaNovo,Lista).

 % devolve as linhas que tem o x tendas e igual ao x espacos disponiveis
tendascheias_aux([],[],_,Listafinal,Listafinal):- !.
tendascheias_aux([H1|T1],[H2|T2],Xatual,Lista,Listafinal):-
    Xnovo is Xatual+1,
    (H1==H2 -> %verifica se os espacos disponiveis para tenda e igual o x tendas necessario
    append(Lista,[Xatual],Listanova), % adicionar a linha em que isso acontece
    tendascheias_aux(T1,T2,Xnovo,Listanova,Listafinal); % continua mas agora com a linha adicionada
    tendascheias_aux(T1,T2,Xnovo,Lista,Listafinal) % continua sem a linha adicionada
    ).

%encher as linhas com relva ou tendas, a diferenca entre este e o outro e que este adiciona a multiplas linhas
encherlinhas_aux(TabuleiroFinal,TabuleiroFinal,[],_,_):- !. 
encherlinhas_aux(Tabuleiro,TabuleiroFinal,[H|T],TendaOuRelva,Largura):-
    insereObjectoEntrePosicoes(Tabuleiro,TabuleiroFinal1, TendaOuRelva, (H, 1), (H, Largura)), % encher a linha
    encherlinhas_aux(TabuleiroFinal1,TabuleiroFinal,T,TendaOuRelva,Largura). % repetir com a nova linha

 % devolve um lista com a vizinhanca da lista inicial (utilizado para as arvores)
vizinhancaLista_aux([],Lista,Lista):- !.
vizinhancaLista_aux([H|T],Lista,Listafinal):-
    vizinhanca(H,Lista1), % pedir a vizinhanca
    append(Lista,Lista1,Listacheia), % adicionar a lista
    vizinhancaLista_aux(T,Listacheia,Listanova), % repetir com coordenadas diferentes
    list_to_set(Listanova,Listafinal). % remover as repeticoes de coordenadas

% devolve um lista com a vizinhacaAlargada da lista inicial (utilizado para as tendas)
vizinhancaAlargadaLista_aux([],Lista,Lista):- !.
vizinhancaAlargadaLista_aux([H|T],Lista,Listafinal):- 
    vizinhancaAlargada(H,Lista1), % pedir a vizinhancaAlargada da posicao
    append(Lista,Lista1,Listacheia), % adicionar a lista
    vizinhancaAlargadaLista_aux(T,Listacheia,Listafinal). % repetir com uma posicao diferente

% insere uma objeto em todos os elementos das listas
insereObjectoLista_aux(TabuleiroFinal,TabuleiroFinal,_,[]):- !.
insereObjectoLista_aux(Tabuleiro,TabuleiroFinal,TendaOuRelva,[H|T]):-  
    insereObjectoCelula(Tabuleiro,TabuleiroFinal1, TendaOuRelva,H), % inserir o objeto na posicao
    insereObjectoLista_aux(TabuleiroFinal1,TabuleiroFinal,TendaOuRelva,T). % repetir com uma posicao diferente

%posicoes possiveis para tendas
listaTendasdisponiveis_aux([],_,Lista,_,Lista) :- !.
listaTendasdisponiveis_aux([(A,B)|T],TodasCelulas,ListaVazia,Tabuleiro,Listafinal):- 
    celulaMesmoVazia_aux(Tabuleiro,Listafinal1), % receber as posicoes vazias
    todasCelulas(Tabuleiro,TodasCelulas1,t), % pedir as celulas com tendas
    append(Listafinal1,TodasCelulas1,Listafinal2), % juntar as duas listas
    (member((A,B),TodasCelulas),member((A,B),Listafinal2) -> %verifica que pertence ao tabuleiro e a celula esta vazia/ ou ocupada com uma tenda
    nth1(A, ListaVazia,Valor, Listamenos), %remover o Valor
    Valornovo is Valor + 1,
    nth1(A,ListaNova,Valornovo,Listamenos), %adicionar o novo Valor
    listaTendasdisponiveis_aux(T,TodasCelulas,ListaNova,Tabuleiro,Listafinal); % repetir com uma Lista nova e uma coordenada diferente
    listaTendasdisponiveis_aux(T,TodasCelulas,ListaVazia,Tabuleiro,Listafinal) % repetir com uma coordenada diferente
    ).

%posicoes para por tendas
listadePosicoesTendas_aux([],ArvoresListarecebedor,_,ArvoresListarecebedor):- !.
listadePosicoesTendas_aux([(A,B)|T],ArvoresListarecebedor,LinhasCheias,ArvoresListaFinal):- 
    (member(A,LinhasCheias)-> % ver se a coordenada A pertence a linha que vai se por as tendas
    append(ArvoresListarecebedor,[(A,B)],ArvoresListarecebedor1), %adicionar as coordenadas do ponto que se vai por uma tenda
    listadePosicoesTendas_aux(T,ArvoresListarecebedor1,LinhasCheias,ArvoresListaFinal); % repetir com coordenadas diferentes e com uma lista nova
    listadePosicoesTendas_aux(T,ArvoresListarecebedor,LinhasCheias,ArvoresListaFinal)). % repetir com coordenadas diferentes
    
%devolve a lista de unicaHipoteses que ha em todas as arvores
vizinhacalivreLista_aux(_,[],Lista,Lista):- !.
vizinhacalivreLista_aux(Tabuleiro,[H|T],ListaAcumuladora,Listafinal):- 
    vizinhanca(H,Vizinhanca), %pedir a vizinhanca
    todasCelulas(Tabuleiro,TodasCelulas), %pedir todas as celulas
    subtract(Vizinhanca,TodasCelulas,Vizinhancafora), %ver que parte da vizinhanca esta fora do tabuleiro
    subtract(Vizinhanca,Vizinhancafora,Vizinhancadentro), % remover a parte da vizinhanca que ta fora
    vizinhacalivre_aux(Tabuleiro,Vizinhancadentro,VizinhacaVazia), %  devolve todas as coordenadas que so tem um espaco livre/ e nao tem tendas
    append(ListaAcumuladora,VizinhacaVazia,ListaAcumuladoraNova), % adicionar a lista
    vizinhacalivreLista_aux(Tabuleiro,T,ListaAcumuladoraNova,Listafinal). % repetir com coordenadas diferentes
    
%ver se a vizinhanca de uma arvore so tem um espaco livre e nao tem nenhuma tenda
vizinhacalivre_aux(Tabuleiro,Vizinhanca,VizinhacaVazia):- 
    todasCelulas(Tabuleiro,Arvores,a), % pedir as celulas de todas as arvores
    todasCelulas(Tabuleiro,Relva,r), % pedir as celulas de todas as relvas
    todasCelulas(Tabuleiro,Tendas,t), % pedir as celulas de todas as tendas
    subtract(Vizinhanca,Tendas,VizinhancasemT), %remover as tendas da vizinhanca
    length(VizinhancasemT,C), % pedir a largura da lista sem tendas
    length(Vizinhanca,P), % pedir a largura da lista com tendas
    (C \== P -> VizinhacaVazia = []; %verificar que nao ha nenhuma tenda na vizinhanca
    subtract(Vizinhanca,Arvores,VizinhancasemA), %remover as arvores da vizinhanca
    subtract(VizinhancasemA,Relva,VizinhancasemR), %remover a relva da vizinhanca
    (length(VizinhancasemR,1) -> VizinhacaVazia =VizinhancasemR; % verificar que so ha uma opcao
    VizinhacaVazia = [])).

%em tendas em todas as linhas e colunas as quais faltavam colocar X tendas e que tinham exactamente X posicoes livres.
aproveita_aux(Puzzle):-
    aproveita_aux(Puzzle,TabuleiroFinal), Puzzle = (TabuleiroFinal, _, _).
aproveita_aux(([H|T],CLinhas,CColunas),TabuleiroFinal):-
    length(H,Largura), % ver a largura de uma linha
    todasCelulas([H|T],TodasCelulas), % pedir todas as celulas
    todasCelulas([H|T],ArvoresCelulas,a), %pedir todas as celulas com arvores
    totalListas_aux(0,Largura,ListaVazia), % criar um lista vazia 
    vizinhancaLista_aux(ArvoresCelulas,[],ListaArvores), % criar uma lista com a vizinhanca das arvores
    listaTendasdisponiveis_aux(ListaArvores,TodasCelulas,ListaVazia,[H|T],Listafinal), % posicoes possiveis para tendas
    tendascheias_aux(CLinhas,Listafinal,1,[],Linhascheias), % devolve as linhas que tem o x tendas e igual ao x espacos disponiveis
    listadePosicoesTendas_aux(ListaArvores,[],Linhascheias,Tendasposicoes), % posicoes para por tendas
    insereObjectoLista_aux([H|T],Tabuleirocheio,t,Tendasposicoes), % inserir a lista anterior com tendas
    transpose(Tabuleirocheio,TabuleiroVirado), % repetir o que fizemos antes mas agora para as colunas
    todasCelulas(TabuleiroVirado,ArvoresCelulas1,a), %pedir todas as celulas com arvores
    vizinhancaLista_aux(ArvoresCelulas1,[],ListaArvores1), % criar uma lista com a vizinhanca das arvores
    listaTendasdisponiveis_aux(ListaArvores1,TodasCelulas,ListaVazia,TabuleiroVirado,Listafinal1), % posicoes possiveis para tendas
    tendascheias_aux(CColunas,Listafinal1,1,[],Linhascheias1), % devolve as linhas que tem o x tendas e igual ao x espacos disponiveis
    listadePosicoesTendas_aux(ListaArvores1,[],Linhascheias1,Tendasposicoes1),  % posicoes para por tendas
    insereObjectoLista_aux(TabuleiroVirado,TabuleiroViradocheio,t,Tendasposicoes1),  % inserir a lista anterior com tendas
    transpose(TabuleiroViradocheio,TabuleiroFinal). %Virar o tabuleiro para voltar ao normal

% serve para ver qual linhas tem o mesmo X que espacos livres, devolvendo tudo numa lista com o numero da linha
linhastendas_aux([],[],_,Lista,Lista):- !.
linhastendas_aux([Linha|Tabuleiro],[Clinhas|Clinhas1],X,Lista,Listafinal):-
    findall(P,(member(P,Linha),var(P)),L), % descobrir os espacos lvres
    Xnovo is X+1,
    length(L,Num), % ver a quantos espacos livres ha
    (Num == Clinhas -> append(Lista,[X],Listanova), % ver se o num e igual ao Clinhas
    linhastendas_aux(Tabuleiro,Clinhas1,Xnovo,Listanova,Listafinal); % repetir com uma lista nova e coordenadas novas
    linhastendas_aux(Tabuleiro,Clinhas1,Xnovo,Lista,Listafinal)). % repetir com cooredenadas novas

% devolve todas as celulas sem nada
celulaMesmoVazia_aux(Tabuleiro,Listafinal):-
    todasCelulas(Tabuleiro,TodasCelulas), % pedir todas as Celulas
    todasCelulas(Tabuleiro,TodasCelulastendas,t), % pedir as Celulas das Tendas
    todasCelulas(Tabuleiro,TodasCelulasArvores,a), % pedir as Celulas das Arvores
    todasCelulas(Tabuleiro,TodasCelulasRelva,r), % pedir as Celulas das Relvas
    append(TodasCelulasArvores,TodasCelulastendas,Celulas1), % juntar as celulas
    append(Celulas1,TodasCelulasRelva,Celulas2), % junhtar as celulas
    subtract(TodasCelulas,Celulas2,Listafinal). % tirar as celulas juntadas do total

% Serve para remover a vizinhanca fora do tabuleiro
limpaextras_aux(_,[],Lista,Lista). 
limpaextras_aux(TodasCelulas,[H|T],Lista,Listafinal):-
    (member(H,TodasCelulas)-> append(Lista,[H],ListaNova),% ver se a coordenada pertence ao tabuleiro 
    limpaextras_aux(TodasCelulas,T,ListaNova,Listafinal); % se sim adiciona a lista
    limpaextras_aux(TodasCelulas,T,Lista,Listafinal)).% se nao continua sem adicionar a coordenada

% verificar que nenhuma tenda esta na vizinhancaAlarga de outra tenda
tendasseparadas_aux(TodasCelulas):-
    vizinhancaAlargadaLista_aux(TodasCelulas,[],VizinhancaAlargada), %cria uma lista com a vizinhancaAlargada de todas as tendas
    intersection(TodasCelulas,VizinhancaAlargada,Total), % ver se a alguma intersecao entre as duas listas
    length(Total,0). % se houve alguma intersecao devolve falso

% tenta resolver tabuleiro pondo uma tenda numa posicao possivel no tabuleiro, a Tentativa serve para ver qual posicao que vamos utilizar
arrisca_aux((Tabuleiro,CLinhas,CColunas),Tentativa,TabuleiroFinal):-
    todasCelulas(Tabuleiro,TodasCelulas), % pedir todas as celulas do tabuleiro
    todasCelulas(Tabuleiro,TodasCelulasArvores,a),% pedir todas as arvores do tabuleiro
    vizinhancaLista_aux(TodasCelulasArvores,[],Vizinhanca), % pedir uma lista com a vizinhanca das arvores
    limpaextras_aux(TodasCelulas,Vizinhanca,[],VizinhancaNova), % remove-mos as celulas que estao fora do tabuleiro
    nth1(Tentativa,VizinhancaNova,Celula), % encontrar a posicao para por a tenda
    insereObjectoCelula(Tabuleiro,TabuleiroNovo,t,Celula), % por a tenda nessa Celula
    resolve((TabuleiroNovo,CLinhas,CColunas),Tabuleiro,0,Tentativa,TabuleiroFinal). % tentar resolver o tabuleiro com a tenda

%----------------(Separacao das funcoes auxiliares das funcoes requeridas)--------------------------------------------

%ver as posicoes laterais do tuplo
vizinhanca((A,B),L) :-
    Amais is A + 1,
    Amenos is A - 1,
    Bmenos is B - 1,
    Bmais is B + 1,
    L = [(Amenos,B),(A,Bmenos),(A,Bmais),(Amais,B)].

%ver as posicoes laterais e diagonais do tuplo
vizinhancaAlargada((A,B),L):-
    Amais is A + 1,
    Amenos is A - 1,
    Bmenos is B - 1,
    Bmais is B + 1,
    L = [(Amenos,Bmenos),(Amenos,B),(Amenos,Bmais),(A,Bmenos),(A,Bmais),(Amais,Bmenos),(Amais,B),(Amais,Bmais)].

 %todas as celulas do tamanho posto
todasCelulas_pred2(Tamanho,ValorX,ValorY,Lista,TodasCelulas):-
    (ValorX>Tamanho -> TodasCelulas = Lista; %para a funcao continuar ate o valorX das coordenadas ser maior que o tamanho da linha
        (ValorY>Tamanho -> ValorXnovo is ValorX+1,todasCelulas_pred2(Tamanho,ValorXnovo,1,Lista,TodasCelulas); 
            append(Lista,[(ValorX,ValorY)],Listanova),%Caso o valorY for maior que o tamanho da coluna repetir com um novo ValorX 
            ValorYnovo is ValorY +1,
            todasCelulas_pred2(Tamanho,ValorX,ValorYnovo,Listanova,TodasCelulas))).

%todas as celulas do tabuleiro
todasCelulas(Tabuleiro,TodasCelulas):- 
    length(Tabuleiro, Tamanho_tabuleiro),
    todasCelulas_pred2(Tamanho_tabuleiro,1,1,[],TodasCelulas).

%todas as celulas que nao contem arvores relvas ou tendas
todasCelulas(Tabuleiro,TodasCelulas,Z):-
    Z \== a,
    Z \== t,
    Z \== r,
    todasCelulas_pred3(Tabuleiro,TodasCelulas1,1,[],a), % pedir todas as celulas que contem arvores
    todasCelulas_pred3(Tabuleiro,TodasCelulas2,1,[],t), % pedir todas as celulas que contem tenda
    todasCelulas_pred3(Tabuleiro,TodasCelulas3,1,[],r), % pedir todas as celulas que contem relva
    todasCelulas(Tabuleiro,TodasCelulas4), % pedir todas as celulas
    subtract(TodasCelulas4,TodasCelulas1,Total1), % remover todas celulas que contem arvores
    subtract(Total1,TodasCelulas2,Total2), % remover todas celulas que contem tenda
    subtract(Total2,TodasCelulas3,TodasCelulas). % remover todas celulas que contem relva

% todas as celulas do objeto pedido
todasCelulas(Tabuleiro,TodasCelulas,Objeto):- 
    todasCelulas_pred3(Tabuleiro,TodasCelulas,1,[],Objeto).

todasCelulas_pred3([], TodasCelulas, _, TodasCelulas, _).

% devolve todas as celulas do pedido objecto
todasCelulas_pred3([H|T],TodasCelulas,Xatual,Lista,A):- 
    findall(A,member(A, H),Listadeverificacao), % ver se a linha contem um objeto
    length(Listadeverificacao,Comprimento),
    Xnovo is Xatual +1,
    (length(H,0)-> TodasCelulas = Lista; % quando o tabuleiro estiver vazio acaba
        (Comprimento>0 -> posicao_aux(H,1,[],Posicao,A), % ver se a linha contem um objeto/ ver as posicoes do objeto na linha 
        juntaNumeros_aux(Xatual,Posicao,ListadeCelulas), % juntar o valorX da linha com a lista que contem as posicoes do objeto
        append(Lista,ListadeCelulas,Listanova),% acumular os valores das linhas anteriores
        todasCelulas_pred3(T,TodasCelulas,Xnovo,Listanova,A);
        todasCelulas_pred3(T,TodasCelulas,Xnovo,Lista,A))).


% devolve os espacos das linhas sem objectos
contagemlinhas([], ContagemLinhas, ContagemLinhas, _).
contagemlinhas([H|T], ContagemLinhas,Acumulador, Z) :- 
    Z \== a,
    Z \== t,
    Z \== r,
    length(H, Largura),
    totalListas_aux(Largura, Largura, Lista), % fazer as lista maxima possivel
    contagemlinhas([H|T], ContagemLinhas1,Acumulador, a), 
    subtrairListas_aux(Lista, ContagemLinhas1, ContagemLinhas). % remover as arvores

% devolve os espacos das linhas de um certo objecto
contagemlinhas([H|T],ContagemLinhas,Acumulador,Objeto):- 
    posicao_aux(H,1,[],Posicoes,Objeto),
    length(Posicoes,Num), % ver a lagura do objeto na linha
    append(Acumulador,[Num],AcumuladorNovo), % adicionar o valor passado
    contagemlinhas(T,ContagemLinhas,AcumuladorNovo,Objeto). % repetir ate o tabuleiro ser []

% devolve o Clinhas e o CColunas sem objectos
calculaObjectosTabuleiro([H|T], CLinhas, CColunas, X):-
    X\==a,
    contagemlinhas([H|T], ContagemLinhas, [], X), % contar as linhas do tabuleiro 
    transpose([H|T],TabuleiroVirado), % virar o tabuleiro
    contagemlinhas(TabuleiroVirado,Contagemcolunas,[],X), % contar as linhas agora viradas
    CLinhas = ContagemLinhas, % devolver o valor do Clinhas
    CColunas = Contagemcolunas. % devolver o valor do Ccolunas

%  devolve o Clinhas e o CColunas do objecto pedido
calculaObjectosTabuleiro([H|T], CLinhas, CColunas, a):-
    contagemlinhas([H|T], ContagemLinhas, [], a),  %contar as linhas do tabuleiro
    transpose([H|T],TabuleiroVirado), % virar o tabuleiro
    contagemlinhas(TabuleiroVirado,Contagemcolunas,[],a), % contar as linhas agora viradas
    CLinhas = ContagemLinhas, % devolver o valor do Clinhas
    CColunas = Contagemcolunas. % devolve o valor do Ccolunas

 % devolve verdade se a celula pedida tiver so relva ou vazio
celulaVazia(Tabuleiro, (L, C)):-
    todasCelulas(Tabuleiro,TodasCelulas,a), % pedir as celulas das arvores
    todasCelulas(Tabuleiro,TodasCelulastenda,t), % pedir as celulas das tendas
    append(TodasCelulas,TodasCelulastenda,TodasCelulasfinal), % adicionar as celulas das arvores com as celulas das arvores
    (member((L,C),TodasCelulasfinal) -> false; % ver se as coordenadas pertence as celulas anteriores
    true).

 % insere o objecto na celula
insereObjectoCelula(Tabuleiro,TendaOuRelva,(L,C)):-
insereObjectoCelula(Tabuleiro,TabuleiroFinal, TendaOuRelva, (L, C)), Tabuleiro = TabuleiroFinal.
insereObjectoCelula(Tabuleiro,TabuleiroFinal, TendaOuRelva, (L, C)):-
    celulaMesmoVazia_aux(Tabuleiro,Lista1), % devolver uma lista com as posicoes vazias
    (member((L,C),Lista1)-> %verificar que a posicao esta vazia e que pertence ao tabuleiro
    nth1(L,Tabuleiro,Lista,TabuleiroMenos), % remover a linha
    nth1(C,Lista,_,ListaMenos), % remover valor na posicao das coordenadas
    nth1(C,Listamais,TendaOuRelva,ListaMenos), % adicionar o objeto
    nth1(L,TabuleiroFinal,Listamais,TabuleiroMenos); %por a lista nova no tabuleiro
    TabuleiroFinal = Tabuleiro % caso nao seja verdade devolve o tabuleiro anterior
    ).
insereObjectoEntrePosicoes(Tabuleiro, TendaOuRelva, (L, C1), (L, C2)):- % insere o objecto entre as posicoes
insereObjectoEntrePosicoes(Tabuleiro,TabuleiroFinal,TendaOuRelva, (L, C1), (L,C2)), Tabuleiro =TabuleiroFinal.

insereObjectoEntrePosicoes(Tabuleiro,TabuleiroFinal, TendaOuRelva, (L, C1), (L, C2)):- 
    (C1 == C2 -> insereObjectoCelula(Tabuleiro,TabuleiroFinal, TendaOuRelva, (L, C2)); % se a coluna for igual fazemos o ultimo caso
    insereObjectoCelula(Tabuleiro,Tabuleiro1, TendaOuRelva, (L, C1)),
    C3 is C1 + 1 ,
    insereObjectoEntrePosicoes(Tabuleiro1,TabuleiroFinal,TendaOuRelva,(L,C3),(L,C2)) % se nao somamos +1 a primeira coordanada
    ).

 % Poem relva no tabuleiro quando relva e a unica opcao
relva(Puzzle):-
    relva(Puzzle,TabuleiroFinal), Puzzle = (TabuleiroFinal, _, _). % Poem relva no tabuleiro quando relva e a unica opcao
relva(([H|T],CLinhas,CColunas),TabuleiroFinal):-
    calculaObjectosTabuleiro([H|T], CLinhasTendas, CColunaTendas, t), % ver oas possiveis tendas 
    length(H,Largura),
    tendascheias_aux(CLinhas,CLinhasTendas,1,[],Linhascheias), % devolve as linhas que tem o x tendas e igual ao x espacos disponiveis
    encherlinhas_aux([H|T],Tabuleirocheio,Linhascheias,r,Largura), % em vez de utilizar insereObjectoEntrePosicoes fazemos uma funcao que vai a todas as linhas
    transpose(Tabuleirocheio, TabuleiroVirado), % viramos o tabuleiro e fazemos o mesmo que fizemos as linhas
    tendascheias_aux(CColunas,CColunaTendas,1,[],Linhascheias1),% devolve as counas que tem o x tendas e igual ao x espacos disponiveis
    encherlinhas_aux(TabuleiroVirado,TabuleiroCheioVirado,Linhascheias1,r,Largura), % repetir o que fizemos antes mas agora para as colunas
    transpose(TabuleiroCheioVirado,TabuleiroFinal). % virar o novo tabuleiro

% e posto relva onde as arvores nao conseguem chegar
inacessiveis(Tabuleiro):- 
    inacessiveis(Tabuleiro,TabuleiroFinal), Tabuleiro = TabuleiroFinal.
inacessiveis(Tabuleiro,TabuleiroFinal):-
    todasCelulas(Tabuleiro,TodasCelulas), % pedir todas as celulas
    todasCelulas(Tabuleiro,ArvoresCelulas,a), % pedir as celulas das arvores
    vizinhancaLista_aux(ArvoresCelulas,[],ListaArvores), % ListaArvores contem todas as vizinhacas das arvores
    subtract(TodasCelulas,ListaArvores,Listafinal), % tira-se a vizinhanca das arvores das celulas total
    insereObjectoLista_aux(Tabuleiro,TabuleiroFinal,r,Listafinal). % adiciona-se relva onde as arvores nao conseguem chegar

% adiciona tendas onde o seu x e igual aos espacos vazios
aproveita(Puzzle):-
    aproveita(Puzzle,TabuleiroFinal), Puzzle = (TabuleiroFinal, _, _).
aproveita(([H|T],Clinhas,CColunas),TabuleiroFinal):-
    length(H,Largura), % pedir a largura
    linhastendas_aux([H|T],Clinhas,1,[],Lista), % devolver uma lista com as linhas 
    encherlinhas_aux([H|T],TabuleiroNovo,Lista,t,Largura), % encher as linhas
    transpose(TabuleiroNovo,TabuleiroVirado), % virar o tabuleiro
    linhastendas_aux(TabuleiroVirado,CColunas,1,[],Lista1), % devolver uma lista com as colunas
    encherlinhas_aux(TabuleiroVirado,TabuleiroNovo1,Lista1,t,Largura), % encher as colunas
    transpose(TabuleiroNovo1,TabuleiroFinal). % virar mais uma vez


% poem relva na vizinhanca alargada das tendas
limpaVizinhancas(Puzzle):-
    limpaVizinhancas(Puzzle,TabuleiroFinal), Puzzle = (TabuleiroFinal, _, _).
limpaVizinhancas((Tabuleiro,_),TabuleiroFinal):-
    todasCelulas(Tabuleiro,PosicoesFinal,t), %pedir todas as celulas das arvores
    vizinhancaAlargadaLista_aux(PosicoesFinal,[],Vizinhancatenda), % pedir uma lista que tem a vizinhacaAlargada das tendas
    list_to_set(Vizinhancatenda,Vizinhancatendanova), % remover as repeticoes da lista anterior
    insereObjectoLista_aux(Tabuleiro,TabuleiroFinal,r,Vizinhancatendanova). % inserir relva na lista anterior

%todas as arvores que tinham apenas uma posicao livre e nao tem tenda, tem agora uma tenda nessa posicao.
unicaHipotese(Puzzle):-
    unicaHipotese(Puzzle,TabuleiroFinal), Puzzle = (TabuleiroFinal, _, _).
unicaHipotese((Tabuleiro,_,_),TabuleiroFinal):-
   todasCelulas(Tabuleiro,TodasCelulasArvores,a), % pedir todas as celulas das arvores
   vizinhacalivreLista_aux(Tabuleiro,TodasCelulasArvores,[],PosicaodeTenda), % pedir a vizinhanca livre de arvores sem tendas
   insereObjectoLista_aux(Tabuleiro,TabuleiroFinal,t,PosicaodeTenda). % inserir tendas na ultima lista

 %valida que cada arvore tem uma e uma so ligacao com uma tenda
valida([],_):- true.
valida([H|T],LTen):-
    vizinhanca(H,L), % pedir a vizinhanca de uma arvore
    intersection(L, LTen, Lista), % ver se existe uma ligacao entre a vizinhanca e uma tenda
    length(Lista,Num),
    (Num > 0 -> valida(T,LTen); % ver se existe uma ligacao
    false).
% resolve o puzzle
resolve((T,P1,P2)):-
    resolve((T,P1,P2),T,0,0,TabuleiroFinal), (T,P1,P2) = (TabuleiroFinal,P1,P2).

resolve((Tabuleiro,CLinhas,CColunas),TabuleiroInicial,Preso,Tentativa,TabuleiroFinal):-
    todasCelulas(Tabuleiro,TodasCelulastendas,t), % pedir todas as celulas com tendas
    todasCelulas(Tabuleiro,TodasCelulasArvores,a), % pedir todas as celulas com arvores
    relva((Tabuleiro,CLinhas,CColunas),Tabuleiro1), %adicionar a relva
    aproveita_aux((Tabuleiro1,CLinhas,CColunas),Tabuleiro2), % utiliza-se uma auxiliar pois devolve valores certos
    unicaHipotese((Tabuleiro2,CLinhas,CColunas),Tabuleiro3), % ver se existe uma arvore com uma uncaHipotese
    limpaVizinhancas((Tabuleiro3,CLinhas,CColunas),Tabuleiro4), % das tendas adicionadas adicionar relva na vizinhacaAlargada
    celulaMesmoVazia_aux(Tabuleiro,Listafinal), % pedir todas as celulas vazias
    length(Listafinal,Vazio), % ver a largura da lista vazia
    calculaObjectosTabuleiro(Tabuleiro,Clinhas1,Ccolunas1,t), % ver o Clinhas e o Ccolunas das tendas
    % ver se o Tabuleiro esta cheio, verificar que nenhuma tenda esta junta, verificar que as Clinhas e as Ccolunas sao iguais ao pedido
    (Vazio==0,valida(TodasCelulasArvores,TodasCelulastendas),tendasseparadas_aux(TodasCelulastendas),Clinhas1==CLinhas,Ccolunas1==CColunas-> TabuleiroFinal=Tabuleiro4;
        (Vazio==Preso-> % ver se tabuleiro esta preso e nao consegue continuar
            Tentativa1 is Tentativa+1,
            arrisca_aux((TabuleiroInicial,CLinhas,CColunas),Tentativa1,TabuleiroFinal); % por uma tenda num dos sitios possiveis e ver se consegue resolver com isso
    resolve((Tabuleiro4,CLinhas,CColunas),TabuleiroInicial,Vazio,Tentativa,TabuleiroFinal))). % repetir o resolve com um tabuleiro diferente

%-------------------------------------------(Explicacao de situacoes)----------------------------------------------------------------

/*
O uso do 'Tabuleiro Final' tem sido constante nas 'Estrategias' (3) e na abordagem de 'Tentativa e Erro' (4),
pois e mais compreensivel para trabalhar e facilita a compreensao das acoes de cada predicado.

No predicado resolve,linha 373,
o 'TabuleiroInicial' serve para caso 'arricas_aux' nao funcionar e comecar no inicio,
o 'Preso' serve para ver se o 'resolve' fica preso e nao consegue continua sozinho,
o 'Tentativa' serve para tentar por uma tenda numa posicao diferente cada vez.
Tambem e usado o predicado aproveita_aux em vez do predicado aproveita pois,
o aproveita_aux poem tendas onde tem o mesmo espaco de 'Vizinhanca' de arvores que tendas pedidas na linha/coluna
em vez de ver os espacos livres.
*/
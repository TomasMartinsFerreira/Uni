def eh_intersecao(intersecao):
    '''
    Devolve True caso o seu argumento seja um TAD intersecao e False caso contrario.
    '''
    if type(intersecao) == tuple and len(intersecao)==2: # verificar a intersecao total

        if type(obtem_col(intersecao)) == str and len(obtem_col(intersecao))==1 and ord('S') >= ord(obtem_col(intersecao)) >= ord('A') : # verificar o primeiro elemento da intersecao

            if type(obtem_lin(intersecao)) == int and 19>= obtem_lin(intersecao) >= 1: # verificar o segundo elemento da intersecao

                return True
            else:
                return False
        else:
            return False
    else:
        return False

def cria_intersecao(col,lin):
    '''
    Recebe um caracter e um inteiro correspondentes 
    a coluna col e a linha lin e devolve a intersecao correspondente.
    Verifica se a intersecao segue as regras.
    '''
    intersecao =(col,lin)
    if eh_intersecao(intersecao) != True:
        raise ValueError('cria_intersecao: argumentos invalidos')
    return intersecao

def obtem_col(intersecao):
    '''
    Devolve a coluna da intersecao.
    '''
    return intersecao[0]

def obtem_lin(intersecao):
    '''
    Devolve a linha da intersecao.
    '''
    return intersecao[1]

def intersecoes_iguais(intersecao1,intersecao2):
    '''
    Devolve True apenas se intersecao1 e iintersecao2 sao intersecoes e sao iguais, e False caso contrario.
    '''
    return obtem_col(intersecao1) == obtem_col(intersecao2) and obtem_lin(intersecao1) == obtem_lin(intersecao2)

def intersecao_para_str(intersecao):
    '''
    Devolve a cadeia de caracteres que representa o seu
    argumento, EX: 'A1'.
    '''
    num_str = str(obtem_lin(intersecao))
    return obtem_col(intersecao)+num_str

def str_para_intersecao(string):
    '''
    Devolve a intersecao representada pelo seu argumento.
    '''
    str_num = int(string[1:])
    return (string[0],str_num)

def obtem_intersecoes_adjacentes(intersecao_para_adjacente,intersecao_final):
    '''
    Verificamos se existem intersecoes apenas nas laterais e se segue as regras do goban atraves da ultima intersecao.
    '''

    intersecoes_adjacentes=()

    if obtem_lin(intersecao_para_adjacente) > 1: # serve para ver se existe um ponto debaixo da intersecao
        intersecoes_adjacentes += (cria_intersecao(obtem_col(intersecao_para_adjacente),obtem_lin(intersecao_para_adjacente)-1),)

    if  ord(obtem_col(intersecao_para_adjacente)) >ord('A'): # serve para ver se existe um ponto a esquerda da intersecao
        intersecoes_adjacentes += (cria_intersecao(chr(ord(obtem_col(intersecao_para_adjacente))-1),obtem_lin(intersecao_para_adjacente)),)

    if  ord(obtem_col(intersecao_final)) > ord(obtem_col(intersecao_para_adjacente)): # serve para ver se existe um ponto a direita da intersecao
        intersecoes_adjacentes += (cria_intersecao(chr(ord(obtem_col(intersecao_para_adjacente))+1),obtem_lin(intersecao_para_adjacente)),)

    if obtem_lin(intersecao_final)>obtem_lin(intersecao_para_adjacente): # serve para ver se existe um ponto em cima da intersecao
        intersecoes_adjacentes += (cria_intersecao(obtem_col(intersecao_para_adjacente),obtem_lin(intersecao_para_adjacente)+1),)

    return intersecoes_adjacentes

def ordena_intersecoes(tuplo):
    '''
    Ordena as intersecoes seguindo a ordem pedida.
    '''
    return tuple(sorted(sorted(tuplo, key=lambda x: obtem_col(x)),key=lambda x: obtem_lin(x)))

def cria_pedra_branca():
    '''
    Devolve uma pedra pertencente ao jogador branco.
    '''
    return 'O'

def cria_pedra_preta():
    '''
    Devolve uma pedra pertencente ao jogador preto.
    '''
    return 'X'

def cria_pedra_neutra():
    '''
    Devolve uma pedra neutra.
    '''
    return '.'

def eh_pedra(arg):
    '''
    Devolve True caso o seu argumento seja um TAD pedra e False caso contrario.
    '''
    return arg == cria_pedra_preta() or arg ==cria_pedra_branca() or arg ==cria_pedra_neutra()

def eh_pedra_branca(pedra):
    '''
    Devolve True caso a pedra seja do jogador branco e False caso contrario.
    '''
    return pedra==cria_pedra_branca()

def eh_pedra_preta(pedra):
    '''
    Devolve True caso a pedra seja do jogador preto e False caso contrario.
    '''
    return pedra==cria_pedra_preta()

def pedras_iguais(pedra1,pedra2):
    '''
    Devolve True apenas se a pedra1 e pedra2 sao pedras e sao iguais.
    '''
    return eh_pedra(pedra1)==True and eh_pedra(pedra2)==True and pedra1==pedra2

def pedra_para_str(pedra):
    '''
    Devolve a cadeia de caracteres que representa o jogador dono
    da pedra, isto  ́e, 'O', 'X' ou '.' para pedras do jogador branco, preto ou neutra respetivamente.
    '''
    return pedra

def eh_pedra_jogador(pedra):
    '''
    Devolve True caso a pedra seja de um jogador e False caso contrario.
    '''
    return pedras_iguais(pedra,cria_pedra_branca())== True or pedras_iguais(pedra,cria_pedra_preta())==True

def cria_goban_vazio(tamanho):
    '''
    Devolve um goban de tamanho tamanhoxtamanho, sem intersecoes ocupadas.
    Verifica que o argumento pertence (9,13,19) caso contrario 
    devolve ValueError.
    '''
    goban =[]
    if tamanho !=9 and tamanho !=13 and tamanho != 19:
        raise ValueError('cria_goban_vazio: argumento invalido')
    if not isinstance(tamanho,int):
        raise ValueError('cria_goban_vazio: argumento invalido')
    
    for letras in range(0,tamanho,1): # Comeca com a primeira linha
        goban += [chr(65+letras),]
    for linhas in range(0,tamanho,1): # continua-mos com as linhas centrais, como acaba no 1 nao e necessario adicionar +1
        for espaço in range(0,tamanho+2,1): # temos de considerar os numeros laterais logo +2
            if espaço == 0 or espaço == tamanho+1: # vemos o numero lateral 
                goban += [tamanho-linhas,]
            else:
                goban +=[cria_pedra_neutra(),] # caso nao seja numero latera e uma pedra vazia
    for letras in range(0,tamanho,1): # acaba com a primeira linha pois sao iguais
        goban += [chr(65+letras),]
    return goban

def posicao_aux(goban,intersecao):
    '''
    Devolve a posicao da intersecao no goban.
    '''
    tamanho=tamanho_goban_aux(goban)
    return 2*tamanho-obtem_lin(intersecao)+(tamanho+1)*(tamanho-obtem_lin(intersecao))+ord(obtem_col(intersecao))-64 #magia

def cria_goban(tamanho,tuplo_pedras_brancas,tuplo_pedras_pretas):
    '''
    Devolve um goban de tamanho nxn, com as intersecoes
    do tuplo_pedras_brancas ocupadas por pedras brancas e as intersecoes do tuplo_pedras_pretas
    ocupadas por pedras_pretas. Verifica a validade dos argumentos, gerando
    um ValueError caso os seus argumentos nao sejam validos.
    '''
    if tamanho !=9 and tamanho !=13 and tamanho != 19:
        raise ValueError('cria_goban: argumentos invalidos')
    if not isinstance(tamanho,int) or not isinstance(tuplo_pedras_pretas,tuple) or not isinstance(tuplo_pedras_brancas,tuple):
        raise ValueError('cria_goban: argumentos invalidos')
    
    goban = cria_goban_vazio(tamanho) # começa-mos com o goban vazio

    for pedras_brancas in tuplo_pedras_brancas: # adiciona-mos as pedras_brancas
        if eh_intersecao_valida(goban,pedras_brancas) != True: # verificamos a intersecao
            raise ValueError('cria_goban: argumentos invalidos')
        posicao=posicao_aux(goban,pedras_brancas)
        goban[posicao] = cria_pedra_branca()
        rep = 0
        for pedras_brancas_rep in tuplo_pedras_brancas: # verificamos que nao ha repeticao de uma intesercao
            if pedras_brancas ==pedras_brancas_rep:
                rep +=1

            if rep > 1:
                raise ValueError('cria_goban: argumentos invalidos')

    for pedras_pretas in tuplo_pedras_pretas: # adiciona-mos as pedras_pretas
        if eh_intersecao_valida(goban,pedras_pretas) != True: # verificamos a intersecao
            raise ValueError('cria_goban: argumentos invalidos')
        posicao=posicao_aux(goban,pedras_pretas)
        goban[posicao] = cria_pedra_preta()
        rep = 0
        for pedras_pretas_rep in tuplo_pedras_pretas: # verificamos que nao ha repeticao de uma intesercao
            if pedras_pretas ==pedras_pretas_rep:
                rep +=1
                
            if rep > 1:
                raise ValueError('cria_goban: argumentos invalidos')
            
    for pedras_brancas in tuplo_pedras_brancas: # verificamos que nao ha repeticao de uma intesercao entre os dois tuplos
        for pedras_pretas in tuplo_pedras_pretas:
            if pedras_brancas==pedras_pretas:
                raise ValueError('cria_goban: argumentos invalidos')

    return goban

def tamanho_goban_aux(goban):
    '''
    Devolve o tamanho do goban.
    Verificando que o goban segue as regras.
    '''
    if len(goban) == 437:
        return 19
    elif len(goban) == 221:
        return 13
    elif len(goban) == 117:
        return 9
    else:
        return False


def cria_copia_goban(goban):
    '''
    Recebe um goban e devolve uma copia do goban.
    '''
    x=[]
    x+=goban
    return x

def obtem_ultima_intersecao(goban):
    '''
    Devolve a intersecao que corresponde ao canto superior direito do goban.
    '''
    if len(goban) == 437:
        return cria_intersecao('S',19)
    elif len(goban) == 221:
        return cria_intersecao('M',13)
    else:
        return cria_intersecao('I',9)

def obtem_pedra(goban,intersecao):
    '''
    Devolve a pedra na intersecao do goban. Se a intersecao nao estiver ocupada, devolve uma pedra neutra.
    '''
    posicao=posicao_aux(goban,intersecao)
    return goban[posicao]

def obtem_cadeia(goban,intersecao):
    """
    Devolve o tuplo formado pelas intersecoes (em ordem de leitura)
    das pedras da mesma cor que formam a cadeia que passa pela intersecao.
    Se a posicao nao estiver ocupada, devolve a cadeia de intersecoes livres.
    """
    cadeia1=()
    cadeia=(intersecao,) #começamos com a intersecao
    res=(intersecao,)
    
    while len(cadeia) != 0:

        cadeia1=()
        for intersecao1 in cadeia:

            cadeia=()

            for intersecao2 in obtem_intersecoes_adjacentes(intersecao1,obtem_ultima_intersecao(goban)):

                if  pedras_iguais(obtem_pedra(goban,intersecao2),obtem_pedra(goban,intersecao))== True and intersecao2 not in res: #verificar a pedra da intersecao

                    res += (intersecao2,)

                    cadeia1 += (intersecao2,)

        cadeia = cadeia1

    return ordena_intersecoes(res)

def coloca_pedra(goban,intersecao,pedra):
    '''
    Modifica destrutivamente o goban colocando a pedra
    do jogador na intersecao, e devolve o proprio goban.
    '''
    posicao =posicao_aux(goban,intersecao)
    goban[posicao]=pedra
    return goban

def remove_pedra(goban,intersecao):
    '''
    Modifica destrutivamente o goban removendo a pedra
    da intersecao, e devolve o proprio goban.
    '''
    posicao=posicao_aux(goban,intersecao)
    goban[posicao]= cria_pedra_neutra()
    return goban

def remove_cadeia(goban,tuplo):
    '''
    Modifica destrutivamente o goban removendo as pedras
    das intersecoes do tuplo, e devolve o proprio goban.
    '''
    for intersecao1 in tuplo:
        remove_pedra(goban,intersecao1)
    return goban

def eh_goban(arg):
    '''
    Devolve True caso o seu argumento seja um TAD goban e False caso contrario.
    '''
    linhas_centrais=0
    if type(arg) == bool or type(arg) == int:
        return False
    
    if tamanho_goban_aux(arg) != False: # verificamos se o tamanho esta certo
        tamanho= tamanho_goban_aux(arg)        
    else:
        return False
    
    for num in range(0,tamanho,1): # verificar a primeira linha
        if  chr(65+num) != arg[num]:
            return False
        
    for num in range(tamanho,0,-1): # verificar as linhas centrais
        if num != arg[tamanho+linhas_centrais]:
            return False
        
        for pedra in range(1,tamanho+1,1):
            if pedras_iguais(arg[tamanho+pedra+linhas_centrais],cria_pedra_neutra()) != True and  pedras_iguais(arg[tamanho+pedra+linhas_centrais],cria_pedra_branca()) != True and  pedras_iguais(arg[tamanho+pedra+linhas_centrais],cria_pedra_preta()) != True:
                return False

        if num != arg[tamanho+linhas_centrais+(tamanho+1)]:
            return False
        linhas_centrais += tamanho+2

    
    for num in range(0,tamanho,1): # verificar a ultima linha
        if chr(65+num) != arg[len(arg)-tamanho+num]:
            return False
    
    return True

def eh_intersecao_valida(goban,intersecao):
    '''
    Verifica-mos se a intersecao pertence ao goban e se a intersercao segue as regras.
    '''
    tamanho = tamanho_goban_aux(goban) # utilizamos o tamanho_goban_aux para valores mais exatos
    return eh_intersecao(intersecao) == True and ord(chr(64+tamanho)) >= ord(obtem_col(intersecao)) >= ord('A') and tamanho>= obtem_lin(intersecao) >= 1

def gobans_iguais(goban1,goban2):
    '''
    Devolve True apenas se goban1 e goban2 forem gobans e forem iguais.
    '''
    return eh_goban(goban1) and eh_goban(goban2) and goban1 == goban2

def goban_para_str(goban):
    '''
    Devolve a cadeia de caracteres que representa o goban.
    '''
    linhas_centrais = 0 # serve para passar para a proxima linha
    string ='  '
    tamanho=tamanho_goban_aux(goban)
    for num in range(0,tamanho,1): # A primeira linha
        string += ' '+str(chr(65+num))

    string += '\n'
        
    for num in range(tamanho,0,-1): # As linhas centrais
        if num >9: # verificar se o numero lateral tem 1 ou 2 digitos
            string += str(num)
        else:
            string += ' ' + str(num)
        
        for pedra in range(1,tamanho+1,1): # as pedras nas linhas centrais
            string += ' ' + pedra_para_str(goban[tamanho+pedra+linhas_centrais])
                

        if num >9:  # verificar se o numero lateral tem 1 ou 2 digitos
            string += ' ' + str(num)
        else:
            string += '  ' + str(num)
        linhas_centrais += tamanho+2 # +2 devido as numeros laterais
        string += '\n'

    string += '  '

    for num in range(0,tamanho,1): # A ultima linha
        string += ' '+str(chr(65+num))
    
    return string

def obtem_territorios(goban):
    '''
    Devolve o tuplo formado pelos tuplos com as intersecoes de cada territorio.
    A funcao devolve as intersecoes de cada territorio ordenadas
    em ordem de leitura do tabuleiro de Go, e os territorios ordenados em ordem de
    leitura da primeira intersecao do territorio.
    '''
    tamanho=tamanho_goban_aux(goban)

    intersecoes=()
    cadeia=()
    for numeros in range(1,tamanho+1,1):

        for letras in range(1,tamanho+1,1): 

            if pedras_iguais(obtem_pedra(goban,cria_intersecao(chr(64+letras),numeros)),cria_pedra_neutra())==True and cria_intersecao(chr(64+letras),numeros) not in cadeia:

                cadeia += obtem_cadeia(goban,cria_intersecao(chr(64+letras),numeros)) # serve para impedir a repeticao de territorios
                intersecoes += (obtem_cadeia(goban,cria_intersecao(chr(64+letras),numeros)),)

    return intersecoes

def obtem_adjacentes_diferentes(goban,tuplo):
    '''
Devolve o tuplo ordenado formado pelas intersecoes adjacentes as intersecoes do tuplo:

(a) livres, se as intersecoes do tuplo estao ocupadas por pedras de jogador;

(b) ocupadas por pedras de jogador, se as intersecoes do tuplo estao livres.
    '''
    if not isinstance(tuplo,tuple):
        raise ValueError('obtem_adjacentes_diferentes: argumentos invalidos')
    vales_da_cadeia=()
    for intersecao in tuplo: #escolhemos um interseção
        if pedras_iguais(obtem_pedra(goban,intersecao),cria_pedra_neutra()) == False:    
            for intersecao_adjacente in obtem_intersecoes_adjacentes(intersecao,obtem_ultima_intersecao(goban)): #vemos as interseções adjacentes dessa interseção
                
                if pedras_iguais(obtem_pedra(goban,intersecao_adjacente),cria_pedra_neutra()) == True and intersecao_adjacente not in vales_da_cadeia: #vemos o valor da interseção e impedimos a repetição dessa interseçao

                    vales_da_cadeia += (intersecao_adjacente,)
        else:
            for intersecao_adjacente in obtem_intersecoes_adjacentes(intersecao,obtem_ultima_intersecao(goban)): #vemos as interseções adjacentes dessa interseção
    
                if pedras_iguais(obtem_pedra(goban,intersecao_adjacente),cria_pedra_neutra()) == False and intersecao_adjacente not in vales_da_cadeia: #vemos o valor da interseção e impedimos a repetição dessa interseçao

                    vales_da_cadeia += (intersecao_adjacente,)

    return ordena_intersecoes(vales_da_cadeia)

def jogada(goban,intersecao,pedra):
    '''
    Modifica destrutivamente o goban colocando a pedra do jogador
    na intersecao e remove todas as pedras do jogador contrario pertencentes a
    cadeias adjacentes que ficam sem liberdades, devolvendo o proprio goban.
    '''
    intersecoes_adjacentes=obtem_intersecoes_adjacentes(intersecao,obtem_ultima_intersecao(goban))
    coloca_pedra(goban,intersecao,pedra)
    for intersecao1 in intersecoes_adjacentes:

        if pedras_iguais(obtem_pedra(goban,intersecao1),recebe_pedra_oposta_aux(pedra)) == True:
            
            cadeia=obtem_cadeia(goban,intersecao1) 

            if len(obtem_adjacentes_diferentes(goban,cadeia))==0:
                #Se as pedras do jogador contrario pertencentes a cadeias adjacentes que ficam sem liberdades
                remove_cadeia(goban,cadeia)

    return goban

def obtem_pedras_jogadores(goban):
    '''
    Devolve um tuplo de dois inteiros que correspondem 
    ao numero de intersecoes ocupadas por pedras do jogador branco e preto, respetivamente.
    '''
    tamanho=tamanho_goban_aux(goban)

    O=0
    X=0

    for numeros in range(1,tamanho+1,1):
        for letras in range(1,tamanho+1,1): 
            if pedras_iguais(obtem_pedra(goban,cria_intersecao(chr(64+letras),numeros)),cria_pedra_preta()) == True:
                X += 1
            if pedras_iguais(obtem_pedra(goban,cria_intersecao(chr(64+letras),numeros)),cria_pedra_branca()) == True:
                O+= 1

    return (O,X)

def calcula_pontos(goban):
    '''
    E uma funcao auxiliar que recebe um goban e devolve o tuplo de dois
    inteiros com as pontuacoes dos jogadores branco e preto, respetivamente.
    '''
    X1=0
    O1=0
    pedras=obtem_pedras_jogadores(goban)
    O=pedras[0]
    X=pedras[1]
    terr=obtem_territorios(goban)
    for territorios in terr:
        obtem_adjacentes_diferentes1 = obtem_adjacentes_diferentes(goban,territorios)
        for adjacente_diferente in obtem_adjacentes_diferentes1 :
            if pedras_iguais(obtem_pedra(goban,adjacente_diferente),cria_pedra_preta()) == True:
                X1 +=1
            else:
                O1 += 1
            
            if X1 == len(obtem_adjacentes_diferentes1): # Se territorio so tem pedra preta como fronteira adicionamos o territorio
                X += len(territorios)
            
            if O1 == len(obtem_adjacentes_diferentes1): # Se territorio so tem pedra branca como fronteira adicionamos o territorio
                O += len(territorios)
        X1=0
        O1=0
    return (O,X)

def recebe_pedra_oposta_aux(pedra):
    '''
    Serve para devolver a pedra oposta da pedra recebida.
    '''
    if  pedras_iguais(pedra,cria_pedra_preta()) == True:
        return cria_pedra_branca()
    else:
        return cria_pedra_preta()

def eh_jogada_legal(goban,intersecao,pedra,goban_repeticao):
    '''
    Devolve True se a jogada for legal ou False caso contrario,
    sem modificar goban ou goban_repeticao. Para a detecao de repeticao, considere que goban_repeticao
    representa o estado de tabuleiro que nao pode ser obtido apos a resolucao completa da jogada.
    '''
    replica_goban=[]
    replica_goban+=goban
    
    posicao=posicao_aux(goban,intersecao)

    jogada(replica_goban,intersecao,pedra)

    cadeia=obtem_cadeia(replica_goban,intersecao)

    if len(obtem_adjacentes_diferentes(replica_goban,cadeia))==0: # verificar se tem um territorio a sua cadeia/ regra suicidio
        return False
                   
    if gobans_iguais(replica_goban,goban_repeticao)== True: # verificar a repeticao / regra g_ko
        return False
    
    if goban[posicao] != cria_pedra_neutra():
        return False
    

    return True

    
def turno_jogador(goban,pedra,goban_repeticao):
    '''
    Se o jogador passar, a funcao devolve False sem modificar os argumentos.
    Caso contrario, a funcao devolve True e modifica destrutivamente o tabuleiro go de acordo com a jogada realizada.
    '''
    if eh_pedra_preta(pedra) == True:
        resposta = input("Escreva uma intersecao ou 'P' para passar [X]:")
        if resposta=='P':
            return False
        else:
            if 3>= len(resposta) >= 2: #verifica o tamanho da resposta
                if resposta[1:].isdigit()== True: #verifica se e um numero
                    intersecao=str_para_intersecao(resposta)
                    if eh_intersecao_valida(goban,intersecao) != True: # verificr a validade
                        return turno_jogador(goban,cria_pedra_preta(),goban_repeticao)
                    if eh_jogada_legal(goban,intersecao,cria_pedra_preta(),goban_repeticao) != True: # verificar que essa jogada pode ser feita
                        return turno_jogador(goban,cria_pedra_preta(),goban_repeticao)
                    jogada(goban,intersecao,pedra)
                    return True
                else:
                    return turno_jogador(goban,cria_pedra_preta(),goban_repeticao)
            else:
                return turno_jogador(goban,cria_pedra_preta(),goban_repeticao)
    else:
        resposta = input("Escreva uma intersecao ou 'P' para passar [O]:")
        if resposta=='P':
            return False
        else:
            if 3>= len(resposta) >= 2: #verifica o tamanho da resposta
                if resposta[1:].isdigit()== True: #verifica se e um numero
                    intersecao=str_para_intersecao(resposta)
                    if eh_intersecao_valida(goban,intersecao) != True: # verificr a validade da intersecao
                        return turno_jogador(goban,cria_pedra_branca(),goban_repeticao)
                    if eh_jogada_legal(goban,intersecao,cria_pedra_branca(),goban_repeticao) != True: # verificar que essa jogada pode ser feita
                        return turno_jogador(goban,cria_pedra_branca(),goban_repeticao)
                    jogada(goban,intersecao,pedra)
                    return True
                else:
                    return turno_jogador(goban,cria_pedra_branca(),goban_repeticao)
            else:
                 return turno_jogador(goban,cria_pedra_branca(),goban_repeticao)

def go(n,tuplo_branco,tuplo_preto):
    '''
    E a funcao principal que permite jogar um jogo completo do Go de dois jogadores.
    O jogo termina quando os dois jogadores passam a vez de jogar consecutivamente. 
    A funcao devolve True se o jogador com pedras brancas conseguir ganhar o jogo, ou False caso contrario.
    Verificar a validade dos seus argumentos, gerando um ValueError caso os seus argumentos nao sejam validos.
    '''
    if n !=9 and n !=13 and n != 19:
        raise ValueError('go: argumentos invalidos')
    if not isinstance(n,int) or not isinstance(tuplo_branco,tuple) or not isinstance(tuplo_preto,tuple):
        raise ValueError('go: argumentos invalidos')
    tuplo_preto1=()
    tuplo_branco1=()
    if tuplo_preto != ():
        for pedras_pretas in tuplo_preto: # Verificar as intersecoes
            rep = 0
            if not isinstance(pedras_pretas,str): # verificar que e string
                raise ValueError('go: argumentos invalidos')
            if  pedras_pretas[1:].isdigit()!= True: #verifica se e um numero
                raise ValueError('go: argumentos invalidos')
            if eh_intersecao_valida(cria_goban_vazio(n),str_para_intersecao(pedras_pretas)) != True: # verificr a validade da intersecao
                raise ValueError('go: argumentos invalidos')
            tuplo_preto1+=(str_para_intersecao(pedras_pretas),)
            for pedras_pretas_rep in tuplo_preto: # verificar que nao repeticao no mesmo tuplo
                if pedras_pretas==pedras_pretas_rep:
                    rep +=1
                        
                if rep > 1:
                    raise ValueError('go: argumentos invalidos')
            
    if tuplo_branco != ():
        for pedras_brancas in tuplo_branco:  # Verificar as intersecoes
            rep = 0
            if not isinstance(pedras_brancas,str): # verificar que e string
                raise ValueError('go: argumentos invalidos')
            if pedras_brancas[1:].isdigit() != True: #verifica se e um numero
                raise ValueError('go: argumentos invalidos')
            if eh_intersecao_valida(cria_goban_vazio(n),str_para_intersecao(pedras_brancas)) != True: # verificr a validade da intersecao
                raise ValueError('go: argumentos invalidos')
            tuplo_branco1+=(str_para_intersecao(pedras_brancas),)
            for pedras_brancas_rep in tuplo_branco: # verificar que nao repeticao no mesmo tuplo
                if pedras_brancas == pedras_brancas_rep:
                    rep +=1
                    
                if rep > 1:
                    raise ValueError('go: argumentos invalidos')
          
    for pedras_brancas in tuplo_branco1: #verificar as intersecoes
        for pedras_pretas in tuplo_preto1:
            if pedras_brancas==pedras_pretas:
                raise ValueError('go: argumentos invalidos')

    turno=0
    goban=cria_goban(n,tuplo_branco1,tuplo_preto1)
    pontos=calcula_pontos(goban)
    print("Branco (O) tem",pontos[0],"pontos")
    print("Preto (X) tem",pontos[1],"pontos")
    print(goban_para_str(goban))
    passar=0

    goban1=cria_copia_goban(goban) # goban1 e goban2 servem para ver se ha repeticao do tabuleiro
    goban2=cria_copia_goban(goban)

    while passar!= 2: # Apenas acaba quando os dois passarem

        if turno % 2 ==0: # verificar quem e a vez de jogar
            pedra=cria_pedra_preta()
        else:
            pedra=cria_pedra_branca()

        goban2=cria_copia_goban(goban1) # serve para a regra de repeticao
        goban1=cria_copia_goban(goban)   

        if turno_jogador(goban,pedra,goban2) != True: # verificar se o jogador decidiu jogar ou passar
            passar+=1
            turno+=1
            print("Branco (O) tem",pontos[0],"pontos")
            print("Preto (X) tem",pontos[1],"pontos")
            print(goban_para_str(goban))
        else:
            pontos=calcula_pontos(goban)
            turno+=1
            passar=0
            print("Branco (O) tem",pontos[0],"pontos")
            print("Preto (X) tem",pontos[1],"pontos")
            print(goban_para_str(goban))

    return pontos[0]>pontos[1]


#go(9, (), ())
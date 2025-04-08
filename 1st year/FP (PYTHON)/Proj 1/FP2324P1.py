def eh_territorio(territorio):
    '''
    Verifica se o territorio está correto verificando o seu type e das suas linhas e elementos,
    verificar que o seu comprimento e o das sua linhas esta entre os regulamentos
    e se o comprimento é igual em cada linha,verifica se os elementos estão corretos.

    '''
    
    if not isinstance(territorio,tuple): #começar pelo territorio total
        return False
    
    if not 26>=len(territorio)>=1:
        return False
    
    for linha in territorio: # agora verificar as linhas
        if not isinstance(linha,tuple): 
            return False
        
        if len(linha) != len(territorio[0]):
            return False
        
        if not 99>=len(linha)>=1:
            return False

        for elemento in linha: # agora verificar as linhas
            if  isinstance(elemento,int): # o elemento tem de ser exatamente 1 ou 0 não pode ser 1.000(0)1 ou 0.000(0)1

                if not( elemento == 0 or elemento == 1):
                    return False    
            else:
                return False
            
    return True

def obtem_ultima_intersecao(territorio):
    '''
    Devolver a última interseção indo ver os valores da intersecao através dos comprimentos do territorio e da linha. 
    '''
    return (chr(64+len(territorio)),len(territorio[0])) # É utilizado 64 pois o territorio vai ter obrigatóriamente no minimo 1 no comprimento/len

def eh_intersecao(intersecao):
    '''
    Verficar que a intersecao segue as regras implementadas vendo se o primeiro elemento é um string/str de letra maiuscula 
    e se o segundo elemento é um numero inteiro entre 1 e 99 incluindo os dois.
    '''
    if isinstance(intersecao,tuple) and len(intersecao)==2: # verificar a intersecao total

        if isinstance(intersecao[0],str) and len(intersecao[0])==1 and ord('Z') >= ord(intersecao[0]) >= ord('A') : # verificar o primeiro elemento da intersecao

            if isinstance(intersecao[1],int) and 99>= intersecao[1] >= 1: # verificar o segundo elemento da intersecao

                return True
    else:
        return False

def eh_intersecao_valida(territorio,intersecao):
    '''
    Verificar que a intersecao pertence ao territorio utilizando o comprimento/len do territorio. 
    Mesmo não ser necessário adicionar o eh_territorio e o eh_intersecao foi decidido adicionar 
    para esta função estar mais correta e para só utilizar uma função em vez de três nas futuras funções. 
    '''
    if eh_territorio(territorio) != True or eh_intersecao(intersecao) != True:
        return False
     
    if 64+len(territorio)>=ord(intersecao[0])>= ord('A') and len(territorio[0])>=intersecao[1]>=1: 
        return True
    else:
        return False

def eh_intersecao_livre(territorio,intersecao):
   '''
   Verififcar que a intersecao está correta e que a intersecao é igual a 0
   '''
   return eh_intersecao_valida(territorio,intersecao) == True and territorio[ord(intersecao[0])-65][intersecao[1]-1]==0

def obtem_intersecoes_adjacentes(territorio,intersecao):
    '''
    Verificamos se existem intersecoes apenas nas laterais e se segue as regras do territorio.
    '''
    if eh_intersecao_valida(territorio,intersecao) != True:
        raise ValueError('obtem_intersecoes_adjacentes: argumento invalido')
    
    intersecoes_adjacentes=()

    if intersecao[1] > 1: # serve para ver se existe um ponto debaixo da intersecao
        intersecoes_adjacentes += ((intersecao[0],intersecao[1]-1),)

    if  ord(intersecao[0]) >ord('A'): # serve para ver se existe um ponto a esquerda da intersecao
        intersecoes_adjacentes += ((chr(ord(intersecao[0])-1),intersecao[1]),)

    if  64+len(territorio) > ord(intersecao[0]): # serve para ver se existe um ponto a direita da intersecao
        intersecoes_adjacentes += ((chr(ord(intersecao[0])+1),intersecao[1]),)

    if len(territorio[0])>intersecao[1]: # serve para ver se existe um ponto em cima da intersecao
        intersecoes_adjacentes += ((intersecao[0],intersecao[1]+1),)

    return intersecoes_adjacentes
    
def ordena_intersecoes(tuplo):
    '''
    A função ordena as intersecoes vendo se existem no tuplo seguindo a ordem pedida.
    '''
    
    if tuplo == (): # temos de verificar caso o tuplo seja ()
        return ()
    
    resultado=()

    for numeros in range(1,99+1,1): # É adicionado +1 pois queremos que a ultima tentativa seja 99/26

        for letras in range(0,26+1,1): 

            if (chr(65+letras),numeros) in tuplo:

                resultado += ((chr(65+letras),numeros),)
                
    return resultado

def territorio_para_str(territorio):
    '''
    Nesta função é necessário turnar o territorio para string/str então separamos em três secções;
    linha inicial que poem as letras de coluna para coluna
    linhas centro que mostra se na intersecao é 1 ou 0 representado-os através de X e ..
    linha final que é igual a linha inicial mas aparece como a última linha
    '''
    if eh_territorio(territorio) != True:
        raise ValueError('territorio_para_str: argumento invalido')
    
    resultado='  '
    for linha in territorio:

        for letras in range(0,len(territorio),1): # linha inicial
            resultado+=' '+chr(65+letras)
            
        if len(linha)> 9:# como há números de 2 digitos e para ter coerencia na string não se adiciona espaço aos números com 2 digitos
            resultado+= '\n'
        else:
            resultado += '\n '

        for numero in range(0,len(linha),1): # linhas centro 
            resultado+=str(len(linha)-numero)

            for letras in range(0,len(territorio),1): 

                if territorio[letras][len(linha)-numero-1] == 0:
                    resultado += ' .'
                else:
                    resultado += ' X'

            resultado+=' '

            if len(linha)-numero> 9: 
                resultado +=str(len(linha)-numero)
            else: 
                resultado +=' '+str(len(linha)-numero)
            
            numero+=1 # é necessário adicionar 1 ao número para ver se o próximo número contem 1 ou 2 digitos

            if len(linha)-numero> 9:
                resultado+='\n'
            else: 
                resultado+='\n '

        resultado+=' '

        for letras in range(0,len(territorio),1): # linha final 
            resultado+=' '+chr(65+letras)

        return resultado


def obtem_cadeia(territorio,intersecao):
    """
    No obtem_cadeia primeiro vemos o valor da intersecao , 1 ou 0,
    depois vemos as suas intersecoes adjacente e repetimos isso muitas vezes até ao máximo de comprimento 26x99
    e no fim devolvemos a função com a ordena_intersecoes
    """
    if eh_intersecao_valida(territorio,intersecao) != True:
        raise ValueError('obtem_cadeia: argumentos invalidos')
    
    repeticao=0
    cadeia=(intersecao,) #começamos com a intersecao

    if territorio[ord(intersecao[0])-65][intersecao[1]-1]==1: #verificar o valor da intersecao
        while 20000>repeticao: # repetimos muita vezes o for para as intersecoes adjacentes
        
            for linha in cadeia:
        
                if linha[1] > 1 and territorio[ord(linha[0])-65][linha[1]-2]== 1 and (linha[0],linha[1]-1) not in cadeia: # verificamos se tem interseção em baixo,se o valor da interseção é 1, e se não pertence ao resultado para não repetir valores
                    cadeia += ((linha[0],linha[1]-1),)

                if  ord(linha[0]) >65 and territorio[ord(linha[0])-66][linha[1]-1]== 1 and (chr(ord(linha[0])-1),linha[1]) not in cadeia:# verificamos se tem interseção a esquerda,se o valor interseção é 1, e se não pertence ao resultado para não repetir valores
                    cadeia += ((chr(ord(linha[0])-1),linha[1]),)

                if  64+len(territorio) > ord(linha[0]) and territorio[ord(linha[0])-64][linha[1]-1]==1 and (chr(ord(linha[0])+1),linha[1]) not in cadeia:# verificamos se tem interseção a direita,se o valor da interseção é 1, e se não pertence ao resultado para não repetir valores
                    cadeia += ((chr(ord(linha[0])+1),linha[1]),)

                if len(territorio[0])>linha[1] and territorio[ord(linha[0])-65][linha[1]] == 1 and (linha[0],linha[1]+1) not in cadeia:# verificamos se tem interseção em cima,se o valor da interseção é 1, e se não pertence ao resultado para não repetir valores
                    cadeia += ((linha[0],linha[1]+1),)
        
                repeticao+=1
    else:    
        while 20000>repeticao:
        
            for linha in cadeia:
        
                if linha[1] > 1 and territorio[ord(linha[0])-65][linha[1]-2]== 0 and (linha[0],linha[1]-1) not in cadeia: # verificamos se tem interseção em baixo,se o valor da interseção é 0, e se não pertence ao resultado para não repetir valores
                    cadeia += ((linha[0],linha[1]-1),)

                if  ord(linha[0]) >65 and territorio[ord(linha[0])-66][linha[1]-1]== 0 and (chr(ord(linha[0])-1),linha[1]) not in cadeia:  # verificamos se tem interseção  a esquerda,se o valor da interseção é 0, e se não pertence ao resultado para não repetir valores
                    cadeia += ((chr(ord(linha[0])-1),linha[1]),)

                if  64+len(territorio) > ord(linha[0]) and territorio[ord(linha[0])-64][linha[1]-1]==0 and (chr(ord(linha[0])+1),linha[1]) not in cadeia:  # verificamos se tem interseção a direita,se o valor da interseção é 0, e se não pertence ao resultado para não repetir valores
                    cadeia += ((chr(ord(linha[0])+1),linha[1]),)


                if len(territorio[0])>linha[1] and territorio[ord(linha[0])-65][linha[1]] == 0 and (linha[0],linha[1]+1) not in cadeia:  # verificamos se tem interseção em cima,se o valor da interseção é 0, e se não pertence ao resultado para não repetir valores
                    cadeia += ((linha[0],linha[1]+1),)
        
                repeticao+=1
                
    return ordena_intersecoes(cadeia)

def obtem_vale(territorio,intersecao):
    '''
    Para obter os vales da cadeia da interseção verificamos as interseções adjacentes que têm valor de zero
    '''
    if eh_intersecao_valida(territorio,intersecao) != True:
        raise ValueError('obtem_vale: argumentos invalidos')
    
    vales_da_cadeia=()
    cadeia_montanha= obtem_cadeia(territorio,intersecao)

    if territorio[ord(intersecao[0])-65][intersecao[1]-1]==1:

        for linha in cadeia_montanha: #escolhemos um interseção
             
             for intersecao_adjacente in obtem_intersecoes_adjacentes(territorio,linha): #vemos as interseções adjacentes dessa interseção

                if territorio[ord(intersecao_adjacente[0])-65][intersecao_adjacente[1]-1]==0 and intersecao_adjacente not in vales_da_cadeia: #vemos o valor da interseção e impedimos a repetição dessa interseçao

                    vales_da_cadeia += (intersecao_adjacente,)

        return ordena_intersecoes(vales_da_cadeia)
    else:
        raise ValueError('obtem_vale: argumentos invalidos') # só consegue-se obter vales se a interseção for 1
    
def verifica_conexao(territorio,intersecao1,intersecao2):
    '''
    Verifica se existe uma ligação entre a intersecao1 e a intersecao2
    '''
    
    if eh_intersecao_valida(territorio,intersecao1)!= True: #verificar que segue as regras do exercício
        raise ValueError('verifica_conexao: argumentos invalidos')
    
    if eh_intersecao_valida(territorio,intersecao2) != True: #verificar que segue as regras do exercício
        raise ValueError('verifica_conexao: argumentos invalidos')
    
    cadeia= obtem_cadeia(territorio,intersecao1)

    if intersecao2 in cadeia: # Como já se sabe que a intersecao1 está na cadeia é ver se a intersecao2 pertence
        return True    
    else:
        return False

    
def calcula_numero_montanhas(territorio):
    '''
    calcular o número de montanhas ou seja ver quantos elementos no territorio são igual a 1
    Então vamos ver de elemento a elemento o seu valor
    '''
    if eh_territorio(territorio) != True:
        raise ValueError('calcula_numero_montanhas: argumento invalido')
    
    montanha=0
    
    for linha in territorio:

        for elementos in linha:

            if elementos==1:
                
                montanha+=1

    return montanha

def calcula_numero_cadeias_montanhas(territorio):
    '''
    Aproveitamos o obtem_cadeia e sempre que ouver uma cadeia adicionamos 1.
    verificamos para cada ponto
    '''
    if eh_territorio(territorio) != True:
        raise ValueError('calcula_numero_cadeias_montanhas: argumento invalido')
    
    cadeias_montanhas=()
    letras=0
    numero=0
    montanhas=0
    
    for linha in territorio:

        for elementos in linha:

            if elementos==1 and ((chr(65+letras),numero+1)) not in cadeias_montanhas: # não podemos deixa outra interseção da mesma cadeia repetir

                cadeias_montanhas += obtem_cadeia(territorio,(chr(65+letras),numero+1)) # impedir a repetição da cadeia

                montanhas+=1
            
            numero+=1
        
        letras+=1
        numero=0
    
    return montanhas


def calcula_tamanho_vales(territorio):
    '''
    aproveitamos do obtem_cadeia e do obtem_vale utilizamos o obtem_cadeia para não repetirmos essa cadeia
    e o obtem_vale.
    utlizamos o vales1 para tirar todas as repetições em vales.
    e devolvemos o comprimento/len do vales1.
    '''
    if eh_territorio(territorio) != True:
        raise ValueError('calcula_tamanho_vales: argumento invalido')
    
    cadeia=()
    letra=0
    numero=0
    vales_total=()
    tamanho_vales=()

    for linha in territorio:

        for elementos in linha:

            if elementos==1 and ((chr(65+letra),numero+1))not in cadeia: # não deixar uma interseção da mesma cadeia repetir

                cadeia += obtem_cadeia(territorio,(chr(65+letra),numero+1)) # impedir a repetição
                
                vales_total+= obtem_vale(territorio,(chr(65+letra),numero+1)) # receber os vales dessa cadeia
            
            numero+=1
        
        letra+=1
        numero=0
    
    for linha in vales_total:

        if linha not in tamanho_vales: # podem haver interseções repetidas no vales_total logo não deixa-mos passar

            tamanho_vales+=(linha,)
    
    return len(tamanho_vales) # como pedem o tamanho utilizamos os len/comprimento do tamanho_vales
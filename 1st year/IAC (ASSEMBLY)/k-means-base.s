# IAC 2023/2024 k-means
# 
# Grupo: Team BuzzCut
# Campus: Leic-T
#
# Autores:
# 109881, Tomás Ferreira
# 110048, Enzo Barato
# 109750, Gonçalo Protásio
#
# Tecnico/ULisboa

# ALGUMA INFORMACAO ADICIONAL PARA CADA GRUPO:
# - A "LED matrix" deve ter um tamanho de 32 x 32
# - O input e' definido na seccao .data. 
# - Abaixo propomos alguns inputs possiveis. Para usar um dos inputs propostos, basta descomentar 
#   esse e comentar os restantes.
# - Encorajamos cada grupo a inventar e experimentar outros inputs.
# - Os vetores points e centroids estao na forma x0, y0, x1, y1, ...

# Variaveis em memoria
.data

seed: .word 1 #valor inicial da seed
#Input A - linha inclinada
#n_points:    .word 9
#points:      .word 0,0, 1,1, 2,2, 3,3, 4,4, 5,5, 6,6, 7,7 8,8

#Input B - Cruz
#n_points:    .word 5
#points:     .word 4,2, 5,1, 5,2, 5,3 6,2

#Input C
#n_points:    .word 23
#points: .word 0,0, 0,1, 0,2, 1,0, 1,1, 1,2, 1,3, 2,0, 2,1, 5,3, 6,2, 6,3, 6,4, 7,2, 7,3, 6,8, 6,9, 7,8, 8,7, 8,8, 8,9, 9,7, 9,8

#Input D
n_points:    .word 30
points:      .word 16, 1, 17, 2, 18, 6, 20, 3, 21, 1, 17, 4, 21, 7, 16, 4, 21, 6, 19, 6, 4, 24, 6, 24, 8, 23, 6, 26, 6, 26, 6, 23, 8, 25, 7, 26, 7, 20, 4, 21, 4, 10, 2, 10, 3, 11, 2, 12, 4, 13, 4, 9, 4, 9, 3, 8, 0, 10, 4, 10


# Valores de centroids e k a usar na 1a parte do projeto:
#centroids:   .word 0,0,0,0,0
#k:           .word 1

# Valores de centroids, k e L a usar na 2a parte do prejeto:
centroids:   .word 0,0,0,0,0, 0,10,0,0,0, 10,0,0,0,0
k:           .word 3
L:           .word 10
# na lista centroid a distancia entre centroid e de 20 bytes, pois cada centroid
# corresponde a 5 words: abcissa, ordenada, n pontos associados ao seu cluster, soma das abcissas dos pontos associados ao seu cluster
# e soma das ordenadas dos pointos associados ao seu cluster, respetivamente.

# Abaixo devem ser declarados o vetor clusters (2a parte) e outras estruturas de dados
# que o grupo considere necessarias para a solucao:
#clusters:    

#Definicoes de cores a usar no projeto 

colors:      .word 0xff0000, 0x00ff00, 0x0000ff  # Cores dos pontos do cluster 0, 1, 2, etc.

.equ         black      0
.equ         white      0xffffff


# Codigo
 
.text
    # Chama funcao principal da 1a parte do projeto
    #jal mainSingleCluster
    # Descomentar na 2a parte do projeto:
    jal mainKMeans
    
    #Termina o programa (chamando chamada sistema)
    li a7, 10
    ecall


### printPoint
# Pinta o ponto (x,y) na LED matrix com a cor passada por argumento
# Nota: a implementacao desta funcao ja' e' fornecida pelos docentes
# E' uma funcao auxiliar que deve ser chamada pelas funcoes seguintes que pintam a LED matrix.
# Argumentos:
# a0: x
# a1: y
# a2: cor

printPoint:
    li a3, LED_MATRIX_0_HEIGHT
    sub a1, a3, a1
    addi a1, a1, -1
    li a3, LED_MATRIX_0_WIDTH
    mul a3, a3, a1
    add a3, a3, a0
    slli a3, a3, 2
    li a0, LED_MATRIX_0_BASE
    add a3, a3, a0   # addr
    sw a2, 0(a3)
    jr ra

numeropseudoaleatorio:  #utilizando o Algoritmo LCG (X_n = (a * X_(n-1) + c) % m)
    li x17, 30 
    ecall
    add t2, a0, x0 #o valor do tempo está na variável t2

    # Carregar o valor da seed para t1
    la t0, seed
    lw t1, 0(t0)

    # ALgoritmo LCG: constantes
    li t3, 12345       # c
    li t4, 2147483648  # m (2^31)

    # LCG Algorithm: X_n = (a * X_(n-1) + c) % m
    mul t1, t1, a0     # t1 = t1 * a
    add t1, t1, t3     # t1 = t1 + c 
    rem t1, t1, t4     # t1 = t1 % m

    sw t1, 0(t0) # guardar o valor da seed

    # Ensure the result is within the interval [0, 31]
    li t5, 32
    remu t1, t1, t5    # Forçar o resultado a estar no intervalo [0,31], Usamos remu e não rem para evitar valores negativos.
    jr ra #o numero pseudo aleatório está em t1

#gera um centroid aleatorio usando a função numeropseudoaleatorio
geradorcentroidsaleatorio:
    addi sp, sp, -12
    sw ra,0(sp)
    sw s0, 4(sp) 
    sw s1, 8(sp) #push das saved registers e ra para o stack
    la s0, centroids
    lw s1, k #k vai servir de contaor
    loopgeradorcentroidsaleatorio:
    jal numeropseudoaleatorio #guarda numero pseudo aleatório em t1
    sw t1,0(s0) #pôr um número aleatório na abcissa do ponto
    jal numeropseudoaleatorio
    sw t1,4(s0) #pôr um número aleatŕio na ordenada do ponto
    addi s0, s0, 20
    addi s1, s1, -1
    bgt s1, x0, loopgeradorcentroidsaleatorio #quando o contador chega a zero, acabar o loop

    endloopgeradorcentroidsaleatorio:

    lw ra,0(sp)
    lw s0, 4(sp) 
    lw s1, 8(sp) #dar "pop" aos saved registers e ra
    addi sp, sp, 12
    jr ra

### cleanScreen
# Limpa todos os pontos do ecrã
# Argumentos: nenhum
# Retorno: nenhum

cleanScreen:
    # POR IMPLEMENTAR (1a parte)
    addi sp, sp, -4
    sw ra, 0(sp) #guardar endereço da linha a seguir de clean screen
    li a1, -1 # y para -1                                                                 
    li a2, 0xffffff #cor branca
    li t3, 33 #contador para as linhas                                             
    
    stampede:
    addi t3, t3, -1 #decrementar o contador para as linhas
    beq t3, x0, return #se todas as colunas estiverem preenchidas voltar para a main
    li a0, 0 #x para 0
    li t2, 32  #contador para as colunas restablecido para 32                               
    addi a1, a1, 1 #incrementar o y para passar para a proxima coluna
    whileloopcleanscreen:
        addi sp, sp, -8
        sw a0, 0(sp) #guardar a0 
        sw a1, 4(sp) #guardar a1
        
        jal printPoint
        
        lw a0, 0(sp) 
        lw a1, 4(sp)
        addi sp, sp, 8
        
        addi a0, a0, 1 #acrescentar 1 ao x
        addi t2, t2, -1 #decrementar o t2(contador) por -1
        beq t2, x0, stampede #se o contador for igual a zero, quer dizer que a coluna toda foi preenchida, logo, vamos voltar para o stampede e dar reset ao valor de x e acrescentar 1 ao y para passar para a proxima coluna
        j whileloopcleanscreen
    
    return:
    lw ra, 0(sp) #ir buscar o endereço da linha a seguir da chamada de cleanscreen, caso contrario o programa ia para a linha a seguir do printpoint
    addi sp, sp, 4 
    jr ra
        
### printClusters
# Pinta os agrupamentos na LED matrix com a cor correspondente.
# Argumentos: nenhum
# Retorno: nenhum

printClusters:
    addi sp, sp, -12
    sw ra, 0(sp) #guardar endereço da linha a seguir de print clusters
    sw s0, 4(sp) #salvaguardar variáveis s
    sw s1,8(sp)
    lw s0, n_points #vai servir de contador
    la s1, points
    la a5, colors #selecionar a cor 
    li t4,4  # dar o valor 4 ao t4 para depois no index
        
        whileloopprintclusters:            
        addi s0, s0, -1 #decrementar o contador por 1 
        bltz s0,continuemain1 #se o contador chegar a -1, sair desta função     
        lw a0, 0(s1) # dar load da abcissa do ponto para a0
        lw a1, 4(s1) # dar load da ordenada do ponto para a1
        jal nearestCluster #receber o index do centroid mais perto
        mul a0,a0,t4 # multiplicar para adicionar ao colors o index recebido
        add a5,a5,a0 # fazer com que o 0 seja a cor do centroid mais perto
        lw a2,0(a5) # dar a cor ao a2
        sub a5,a5,a0 # por o colors na posição antiga
        lw a0,0(s1) # devolver o valor x do ponto ao a0
        addi s1, s1, 8 #ir para a frente duas posicoes no vetor pontos
     
        jal printPoint # dar print ao ponto, depois da função, guarda o adress da proxima linha
        j whileloopprintclusters
        
    continuemain1:
    lw ra, 0(sp)
    lw s0, 4(sp) #salvaguardar variáveis s
    lw s1,8(sp)
    addi sp,sp,12
    jr ra # voltar para a main

### printCentroids
# Pinta os centroides na LED matrix
# Nota: deve ser usada a cor preta (black) para todos os centroides
# Argumentos: nenhum
# Retorno: nenhum

printCentroids:
    
    # POR IMPLEMENTAR (1a e 2a parte)
    addi sp, sp, -4
    sw ra, 0(sp) #guardar endereço da linha a seguir de clean screen
    lw t0, k #vai servir de contador
    
    la t1, centroids # guardar o adresso do vetor dos centroids
    li a2, 0 #selecionar a cor para preto
    
whileloopprintcentroids:
    
    lw  a0,0(t1)
    lw  a1,4(t1) #guardar as coordenadas do centroid
    jal printPoint #imprimir as coordenadas
    addi t1,t1,20 #ir para as proximas coordenadas
    addi t0, t0, -1
    bgt t0, x0, whileloopprintcentroids
    
endprintcentroids:    
 
    lw ra, 0(sp) #guardar endereço da linha a seguir de clean screen
    addi sp, sp, 4
    jr ra
     
### calculateCentroids
# Calcula os k centroides, a partir da distribuicao atual de pontos associados a cada agrupamento (cluster)
# Argumentos: nenhum
# Retorno: s11, 0 se houve mudança da posicao de centroids, 1 caso contrario 
# utiliza e altera t0, t3, t4

calculateCentroids:
    addi sp, sp, -12
    sw ra, 0(sp) #guardar o endereço de retorno no stack
    sw s1, 4(sp) #guardar valor de s1
    sw s2, 8(sp) #guardar valor de s2 
    la t0, centroids #t0 guarda centroids
    la s1, points #s1 guarda o vetor de pontos
    lw s2, n_points #s2 guarda o numero de pontos
    lw t3, k # t3 guarda o numero de centroids e serve de contador
       
    ### inicializacao dos centroids ###
    centroidinicializer:
        sw x0, 8(t0) #inicializar o valor de n pontos no cluster com valor igual a zero para cada centroid
        sw x0, 12(t0) #inicializar o valor da soma das abcissas dos pontos no cluster com valor igual a zero para cada centroid
        sw x0, 16(t0) #inicializar o valor da soma das ordenadas dos pontos no cluster com valor igual a zero para cada centroid

        addi t0, t0, 20 #passa para o proximo centroid
        addi t3, t3, -1 #contador decresce
        
        bgtz t3, centroidinicializer #se contador maior que zero entao continuar loop
        j clustermaker #senao avancar para o calculador de clusters
         
    clustermaker:
          
        addi t4, x0, 20 #t4 ganha o valor de 20
        la t0, centroids #t0 aponta para o inicio de centroids
        
        lw a0, 0(s1) #a0 guarda o valor de x do ponto
        lw a1, 4(s1) #a1 guarda o valor de y do ponto
        
        jal nearestCluster #calcular o indice do centroid mais proximo do ponto
        mul a0, a0, t4 #multiplicasse o indice a0 por 20 
        add t0, t0, a0 #t0 tem o endereço do centroid mais proximo do ponto
        
        lw t4, 8(t0) #somar 1 a contagem de pontos
        addi t4, t4, 1
        sw t4, 8(t0) # muda-se a somagem de pontos
        
        lw t4, 12(t0) # somar abcissa do ponto
        lw t5, 0(s1)
        add t4, t4, t5
        sw t4, 12(t0) # muda-se a soma das abcissas dos pontos
        
        lw t4, 16(t0) #somar ordenada do ponto
        lw t5, 4(s1)
        add t4, t4, t5
        sw t4, 16(t0) #muda-se a soma das ordenadas dos pontos
        
        addi s1, s1, 8 #passa para o proximo ponto
        addi s2, s2, -1 #desconta do contador de pontos
        
        bgtz s2, clustermaker #se contador maior que zero continua a iterar
        lw t3, k #t3 volta a apontar para o valor de centroids
        la t0, centroids #t0 volta a apontar para o inicio de centroids
        j centroidupdater
    
    centroidupdater:
        lw s2, 8(t0) #s2 ganha o valor de npoints in cluster
        beqz s2, skip # se nao houver pontos no cluster entao skip
        lw t4, 12(t0) #update a abcissa
        div t4, t4, s2 #divide a soma das abcissas pelo numero de pontos
        lw s1, 0(t0) #s1 aponta para a abcissa original
        sw t4, 0(t0) #mudar a abcissa
        
        bne t4, s1, mudancacheck1 #ver se houve mudanca
        j continuacao #senao nao se altera o valor de s11 para 1
        mudancacheck1:
            addi s11, x0, 1
            
        continuacao:
            lw t4, 16(t0) #update a ordenada
            div t4, t4, s2 #divide a soma das ordenadas pelo numero de pontos
            lw s1, 4(t0) #s1 aponta para a ordenada original
            sw t4, 4(t0) #mudar ordenada
            
            bne t4,s1, mudancacheck2 #ver se houve mudanca de valor
            j skip #senao nao se altera o valor de s11 para 1
            mudancacheck2:
                addi s11, x0,1
        
        skip: ## terá que acontecer sempre
        addi t0, t0, 20  #passa para o proximo centroid
        addi t3, t3, -1 #desconta do contador
        bgtz t3, centroidupdater #se contador maior que zero continuar a iterar
        
    fim:
        lw ra, 0(sp) #repor o valor de ra
        lw s1, 4(sp) #repor o valor de s1
        lw s2, 8(sp) # repor o valor de s2
        addi sp, sp, 12 # dealocar o espaco no stack
        jr ra #retomar   

### EuclideanDistance
# Calcula uma variação da distancia Euclidiana entre (x0,y0) e (x1,y1)
# É utilizado a Euclidiana pois é mais eficiente e devolve melhor resultado que a manhattan
# Argumentos:
# a0, a1: x0, y0
# a2, a3: x1, y1
# Retorno:
# a0: distance

EuclideanDistance:
    # POR IMPLEMENTAR (2a parte)
    sub t6,a2,a0 #x2=x1-x0
    sub t5,a3,a1 #y2=y1-y0
    mul t6,t6,t6 #x3=(x2)^2
    mul t5,t5,t5 #y3=(y2)^2
    add a0,t5,t6 # a0=x3+y3
    jr ra
          
### nearestCluster
# Determina o centroide mais perto de um dado ponto (x,y).
# Argumentos:
# a0, a1: (x, y) point
# Retorno:
# a0: cluster index

nearestCluster:
    # POR IMPLEMENTAR (2a parte)
    addi sp, sp, -8
    sw ra, 0(sp) #endereço da linha a seguir do nearestCluster
    sw s0, 4(sp) #guardar endereço do valor anterior de s0
    lw s0, k #dar store ao numero de centroids
    la a4, centroids #receber a lista de pontos
    li t1,0 #ver quantas comparações já foram feitas
    li t2,0 #vai servir para index temporario
    addi a6,a0,0 #guardar o valor x do ponto
    li t3,2049 #serve comparar distancias inicialmente sendo um valor acima do possível 
    
    LoopVerDistancia:
        addi a0,a6,0 # devolver o valor x do ponto para a0
        addi t1,t1,1 # indicar o index da comparação atual
        lw a2,0(a4) # dar o valor de x de um dos centroids
        lw a3,4(a4) # dar o valor de y de um dos centroids
        jal EuclideanDistance # ver a distancia entre pontos
        blt a0,t3,MudarIndex # se a distancia for menor do que a ultima distancia guardada mudamos o index
        bge t1,s0,end # se o index chegou ao fim acabar
        addi a4, a4, 20 # avançar o vetor para o próximo centroid
        j LoopVerDistancia # recomeçar o loop com um centroid novo

     MudarIndex:
         addi t3,a0,0 # guardar a distancia nova
         addi t2,t1,-1 # trocar o t2 para o novo index
         jr ra # voltar para LoopVerDistancia
         
    end:
        addi a0,t2,0 # trocar o ultimo index guardado para a0
        lw ra, 0(sp) # devolver o valor da próxima linha ao ra
        lw s0, 4(sp) #devolver endereço do valor anterior de s0
        addi sp, sp, 8
        jr ra
        

### ResetCentroids
# Limpa todos os Centroids do ecrã.Faz mais eficiente o MainKMeans
# sendo que só mudamos entre 1 a 3 pontos em vez de 2048 pontos no ecra por cada iteração
# Argumentos: nenhum
# Retorno: nenhum        
ResetCentroids:
    addi sp, sp, -4
    sw ra, 0(sp) #endereço da linha a seguir do ResetCentroid
    la a4,centroids #receber a lista de Centroids
    lw a5, k #receber o número de centroids
    la a6,points #receber a lista de Pontos
    li a2, 0xffffff # pôr a cor branca
    li t0,4 # serve para multiplicar por 4
    mul t5,a5,t0 # o valor necessário para dar reset ao vetor centroids
    li t0,0 # serve para verificar quantos centroids já foram limpos

    CleanCentroids:
        lw a0,0(a4) # carregar o x do centroid
        lw a1,4(a4) # carregar o y do centroid
        jal printPoint # limpar o ponto
        addi a4,a4,20 # ir para o proximo ponto
        addi t0,t0,1 # adicionar mais um centroid feito ao contador
        bne t0,a5,CleanCentroids # ver se o contador é igual ao numero de centroids
        sub a4,a4,t5 # voltar ao inicio do  vetor
        lw ra, 0(sp) #endereço da linha a seguir do nearestCluster
        addi sp, sp, 4 # remover o espaço de vetor
        jr ra # ir para a próxima linha
        
### mainKMeans
# Executa o algoritmo *k-means*.
# Argumentos: nenhum
# Retorno: nenhum

mainKMeans:  
    # POR IMPLEMENTAR (2a parte)
    addi sp, sp, -4
    sw ra, 0(sp)
    lw s10, L #s10 sera o contador para as iteracoes
    jal geradorcentroidsaleatorio
    jal cleanScreen #limpa o ecran
    iteracoes:
        li s11, 0 #a cada iteracao s11 comeca com valor 0, se s11 acabar com este mesmo valor
                  #entao nao houve mudancas nas coordenadas dos centroids
    
  
        jal calculateCentroids # calcula a posição do centroid
    
        jal printClusters # faz print aos clusters
    
        jal printCentroids # faz print aos Centroids
    
        addi s10, s10, -1 #reduzir o valor do contador
    
        beqz s11, final #se nao houve mudancas  acaba
        jal ResetCentroids # faz limpa os centroids do ecran
        bgtz s10, iteracoes # se L nao for 0 entao continua
    
    final:
    lw ra, 0(sp)
    addi sp, sp, 4
    jr ra

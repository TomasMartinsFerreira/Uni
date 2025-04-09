import sys
from pulp import LpMaximize, LpProblem, LpVariable, lpSum, PULP_CBC_CMD

def resolver_distribuicao(entrada):
    # Parsear a entrada
    n, m, t = map(int, entrada[0].split())  # número de fábricas, países e crianças
    fabricas = [list(map(int, linha.split())) for linha in entrada[1:n+1]]
    paises = [list(map(int, linha.split())) for linha in entrada[n+1:n+1+m]]
    pedidos = [list(map(int, linha.split())) for linha in entrada[n+1+m:]]

    # Criar o modelo
    modelo = LpProblem("Distribuicao_de_Brinquedos", LpMaximize)

    # dicionários para acelerar as pesquisas
    variaveis_por_fabrica = {i: [] for i in range(1, n + 1)} # {fábrica: pedidos}
    variaveis_por_pais_destino = {j: [] for j in range(1, m + 1)} # {país_destino: pedidos} (pedidos a fábricas desse país)
    variaveis_por_pais_origem = {j: [] for j in range(1, m + 1)} # {país_origem: pedidos} (pedidos de crianças desse país)
    variaveis_por_crianca = {k: [] for k in range(1, t + 1)} # {criança: pedidos}

    # Variáveis de decisão
    x = {}
    for pedido in pedidos:
        k, j, *fabrics = pedido

        for f in fabrics:
            # se stock da fabrica = 0 -> ignorar pedido
            if fabricas[f-1][2] == 0:
                continue

            x[k, f] = LpVariable(f"x_{k}_{f}", cat="Binary")
            variaveis_por_pais_destino[fabricas[f-1][1]].append([k, f])
            variaveis_por_pais_origem[j].append([k, f])
            variaveis_por_fabrica[f].append([k, f])
            variaveis_por_crianca[k].append([k, f])

    # Função objetivo: Maximizar o número de crianças atendidas
    modelo += lpSum(x[k, f] for k, f in x), "Maximizar_Criancas_Atendidas"

    maior = max(n, m, t)
    for i in range(1, maior + 1):
        if i <= n:
            # Restrições de produção (estoque máximo por fábrica)
            modelo += lpSum(x[k, f] for k, f in variaveis_por_fabrica[i]) <= fabricas[i-1][2], f"Estoque_Fabrica_{i}"
        if i <= m:
            # Restrições de exportação por país
            modelo += lpSum(x[k, f] for k, f in variaveis_por_pais_destino[i] if pedidos[k-1][1] != i) <= paises[i-1][1], f"Exportacao_Pais_{i}"
            # Restrições de entrega mínima por país
            modelo += lpSum(x[k, f] for k, f in variaveis_por_pais_origem[i] if pedidos[k-1][1] == i) >= paises[i-1][2], f"Entrega_Minima_Pais_{i}"
        if i <= t:
            # Restrições de atendimento de crianças
            modelo += lpSum(x[k, f] for k, f in variaveis_por_crianca[i]) <= 1, f"Uma_Crianca_Um_Pedido_{i}"

    # Resolver o modelo
    solver = PULP_CBC_CMD(msg=False)
    status = modelo.solve(solver)

    # Resultado
    if status == 1:
        return int(modelo.objective.value())
    else:
        return -1

if __name__ == "__main__":
    # Ler o input todo de uma vez
    entrada = sys.stdin.read().strip().split("\n")

    # Processar entrada e resolver o problema
    resultado = resolver_distribuicao(entrada)
    print(resultado)
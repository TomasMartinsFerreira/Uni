# nuruomino.py: Template para implementação do projeto de Inteligência Artificial 2024/2025.
# Devem alterar as classes e funções neste ficheiro de acordo com as instruções do enunciado.
# Além das funções e classes sugeridas, podem acrescentar outras que considerem pertinentes.

# Grupo 12:
# 109881 Tomas Ferreira
# 109639 Diogo Matias
import utils
import search
import sys

from search import Node
from search import Problem
from search import depth_first_tree_search

# Dicionário com todas as formas (rotações e reflexões únicas) dos 4 tetrominos
# Cada forma tem coordenadas relativas, começando em (1,1)
FORMAS = {
    'L': [
        [(1, 1), (2, 1), (3, 1), (3, 2)],       # Figuras 2 e 3 do enunciado
        [(1, 1), (1, 2), (1, 3), (2, 1)],
        [(2, 1), (2, 2), (2, 3), (1, 3)],
        [(1, 1), (1, 2), (2, 2), (3, 2)],
        [(1, 2), (2, 2), (3, 2), (3, 1)],
        [(1, 1), (1, 2), (1, 3), (2, 3)],
        [(1, 1), (2, 1), (2, 2), (2, 3)],
        [(1, 1), (2, 1), (3, 1), (1, 2)],
    ],
    'I': [
        [(1, 1), (2, 1), (3, 1), (4, 1)],
        [(1, 1), (1, 2), (1, 3), (1, 4)],
    ],
    'T': [
        [(1, 1), (1, 2), (1, 3), (2, 2)],
        [(1, 2), (2, 1), (2, 2), (3, 2)],
        [(2, 1), (2, 2), (2, 3), (1, 2)],
        [(1, 1), (2, 1), (2, 2), (3, 1)],
    ],
    'S': [
        [(1, 2), (1, 3), (2, 1), (2, 2)],
        [(1, 1), (2, 1), (2, 2), (3, 2)],  
        [(1, 1), (1, 2), (2, 2), (2, 3)],  
        [(1, 2), (2, 1), (2, 2), (3, 1)],  
    ]
}

def piece_coords(piece:list, origin:tuple) -> list:
    """Devolve as coordenadas reais da peça quando colocada na posição 'origin'."""
    coordenadas = []
    for i in range(4):
        coord = (origin[0] + piece[i][0] - 1, origin[1] + piece[i][1] - 1)
        coordenadas.append(coord)
    return coordenadas

class Board:
    """Representação interna de um tabuleiro do Puzzle Nuruomino.
    Os métodos desta classe que pedem coordenadas, recebem e devolvem coordenadas com índices a começar em 1.
    Ou seja, ao utilizar estes métodos, ter em conta que o canto superior esquerdo da grelha tem coordenadas (1, 1)
    e o canto inferior direito (N, N)."""

    def __init__(self, size:int):
        """Inicializa uma instância de Board.
        Cria uma lista de listas para representar a grelha e preenche com zeros."""
        self.size = size
        self.grid = [[0 for _ in range(size)] for _ in range(size)]

    def insert_value(self, row:int, col:int, value):
        """Preenche a posição (row, col) da grelha com o valor dado."""
        self.grid[row - 1][col - 1] = value

    def get_value(self, row:int, col:int):
        """Devolve o valor presente na posição (row, col) da grelha."""
        return self.grid[row - 1][col - 1]
    
    def adjacent_regions(self, region:int) -> list:
        """Devolve uma lista das regiões que fazem fronteira com a região enviada no argumento.
        Nota: só funciona corretamente para tabuleiros sem peças colocadas"""
        values = []
        
        for pos_row , pos_col in self.region_positions(region):
            for value in self.orthogonal_values(pos_row,pos_col):
                if value not in values and value != region and isinstance(value, int):
                    values.append(value)
        return values
    
    def is_inside_board(self, row:int, col:int) -> bool:
        """Verifica se as coordenadas (row, col) estão dentro dos limites da grelha."""
        return 0 <= row - 1 < self.size and 0 <= col - 1 < self.size
    
    def adjacent_positions(self, row:int, col:int) -> list:
        """Devolve as posições adjacentes à região, em todas as direções, incluindo diagonais."""
        directions = [(-1,-1),(-1,0),(-1,1),(0,-1),(0,1),(1,-1),(1,0),(1,1)]
    
        adjacent = []
        for x, y in directions:
            new_row, new_col = row + x, col + y
            if self.is_inside_board(new_row, new_col):
                adjacent.append((new_row, new_col))
        
        return adjacent
    
    def adjacent_values(self, row:int, col:int) -> list:
        """Devolve os valores das celulas adjacentes à região, em todas as direções, incluindo diagonais."""
        adjacent = self.adjacent_positions(row,col)
        values = []

        for x, y in adjacent:
            values.append(self.get_value(x, y))
        return values
    
    def orthogonal_positions(self, row:int, col:int) -> list:
        """Devolve as posições adjacentes à região, em todas as direções, exceto diagonais."""
        directions = [(-1,0),(0,-1),(0,1),(1,0)]
    
        adjacent = []
        for x, y in directions:
            new_row, new_col = row + x, col + y
            if self.is_inside_board(new_row, new_col):
                adjacent.append((new_row, new_col))
        
        return adjacent
    
    def orthogonal_values(self, row:int, col:int) -> list:
        """Devolve os valores das celulas adjacentes à região, em todas as direções, exceto diagonais."""
        adjacent = self.orthogonal_positions(row,col)
        values = []

        for x, y in adjacent:
            values.append(self.get_value(x, y))
        return values

    @staticmethod
    def parse_instance():
        """Lê o test do standard input (stdin) que é passado como argumento
        e retorna uma instância da classe Board.

        Por exemplo:
            $ python3 pipe.py < test-01.txt

            > from sys import stdin
            > line = stdin.readline().split()
        """
        # lê o input
        lines = sys.stdin.readlines()
        # inicializa a board com tamanho N x N    (N = len(lines))
        board = Board(len(lines)) 
        row, col = 1, 1
        # coloca os valores na grelha
        for line in lines: 
            for value in line.strip().split():
                board.insert_value(row, col, int(value))
                col += 1
            col = 1
            row += 1
        return board

    def print_instance(self):
        """Imprime uma representação visual da grelha. Útil para testar o programa."""
        for row in range(1, self.size + 1):
            line = []
            for col in range(1, self.size + 1):
                line.append(str(self.get_value(row, col)))
            print("\t".join(line))

    def region_positions(self,region:int) -> list:
        """Devolve todas as posições de região"""
        position = []

        for row in range(1, self.size + 1):
            for col in range(1, self.size + 1):
                if self.get_value(row, col) == region:
                    position.append((row, col))
        return position
    
    def creates_square(self, coordenadas:list) -> bool:
        """Verifica se a peça (dada em coordenadas) cria um quadrado 2x2 neste tabuleiro."""
        def is_filled(r, c):
            if not self.is_inside_board(r, c):
                return False
            return (r, c) in coordenadas or not isinstance(self.get_value(r, c), int)

        for r, c in coordenadas:
                   # quadrado top-left
            if ((is_filled(r - 1, c) and
                is_filled(r, c - 1) and
                is_filled(r - 1, c - 1))
                or # quadrado top-right
                (is_filled(r - 1, c) and
                is_filled(r, c + 1) and
                is_filled(r - 1, c + 1))
                or # quadrado bottom-left
                (is_filled(r + 1, c) and
                is_filled(r, c - 1) and
                is_filled(r + 1, c - 1))
                or # quadrado bottom-right
                (is_filled(r + 1, c) and
                is_filled(r, c + 1) and
                is_filled(r + 1, c + 1))):
                return True
        return False

    def bfs(self, start_row, start_col):
        visited = set()
        queue = [(start_row, start_col)]
        bfs_tree = set()
        visited.add((start_row, start_col))
        bfs_tree.add((start_row, start_col))

        while queue:
            row, col = queue.pop(0)

            for neighbor in self.orthogonal_positions(row, col):
                if neighbor not in visited and str(self.get_value(neighbor[0], neighbor[1])).isalpha():
                    visited.add(neighbor)  # Mark as visited immediately
                    queue.append(neighbor)
                    bfs_tree.add(neighbor)

        return bfs_tree
    
    def orthogonal_region_in_coords(self, region:int,coords:list) -> bool:
        """Verifica se a coordenadas dadas está em contacto com outra região"""
        for row, col in coords:
            for new_row, new_col in self.orthogonal_positions(row, col):
                if self.get_value(new_row, new_col) != region:
                    return False
        return True
    
    def orthogonal_region_in_position(self, region:int) -> list:
        """ Devolve uma lista com as coordenadas de todas as posições ortogonais à região dada."""
        list_of_coords = []
        for row, col in self.region_positions(region):
            for new_row, new_col in self.orthogonal_positions(row, col):
                if self.is_inside_board(new_row, new_col) and self.get_value(new_row, new_col) != region:
                    list_of_coords.append((new_row, new_col)) 
        return list_of_coords
                
class NuruominoState:
    state_id = 0

    def __init__(self, board: Board):
        self.board = board
        self.id = NuruominoState.state_id
        self.current_region = 0
        self.occupied_regions = set() # regiões ocupadas
        self.assignments = {} # atribuições de valores (ações) às variáveis (regiões)
        self.degree = {} # guarda o numero de regiões adjacentes que cada região tem
        self.all_actions = [] # todas as ações possíveis neste tabuleiro
        NuruominoState.state_id += 1

    def __lt__(self, other):
        """ Este método é utilizado em caso de empate na gestão da lista
        de abertos nas procuras informadas. """
        return self.id < other.id
    
    def copy(self, total_regions: int):
        """Devolve uma deepcopy do estado."""
        new_board = Board(self.board.size)
        new_board.grid = [row[:] for row in self.board.grid]
        new_state = NuruominoState(new_board)
        new_state.current_region = self.current_region
        new_state.assignments = self.assignments.copy()
        new_state.degree = self.degree.copy()
        new_state.occupied_regions = self.occupied_regions.copy()

        # fazer uma cópia do all_actions
        new_state.all_actions = [[] for _ in range(total_regions + 1)]
        for region in range(1, total_regions + 1):
            new_state.all_actions[region] = self.all_actions[region].copy()

        return new_state

    def conflicts(self, action, adjacent_regions, initial_board) -> bool:
        """Devolve True se action for incompatível com o estado (se faz com que peças iguais se toquem,
        se forma um quadrado 2x2 ou se faz com que outra peça fique inacessível)."""
        
        for r, c in action[2]: # para cada coordenada da ação
            for new_r, new_c in self.board.orthogonal_positions(r, c): # para cada posição ortogonal
                if (new_r, new_c) in action[2]: # ignorar posições ortogonais dentro da própria peça
                    continue

                value = self.board.get_value(new_r, new_c)
                # se tocar uma peça do mesmo tipo
                if value == action[1]:
                    return True
                
                # se tocar numa peça, qualquer que seja
                if not isinstance(value, int):
                    if self.board.creates_square(action[2]):
                        return True

        if action[0] not in self.assignments.keys() and self.occupied_regions.issuperset(adjacent_regions[action[0]]):
            connection_coords = []
            for (row, col) in self.board.orthogonal_region_in_position(action[0]):
                if str(self.board.get_value(row, col)).isalpha():
                    for new_row, new_col in self.board.orthogonal_positions(row, col):
                        if self.board.get_value(new_row, new_col) == action[0]:
                            connection_coords.append((new_row, new_col))
            if not (set(connection_coords) & set(action[2])):
                return True  # Check if the code is right

        # verificar peças inacessíveis (blocked)
        for region in adjacent_regions[action[0]]: # para cada região adjacente à da peça colocada
            blocked = True

            # se a região não tiver peça, passamos à próxima
            if region not in self.assignments.keys():
                continue
            
            # se a região tiver uma peça, vemos se ela está bloqueada
            # Generalização: se a peça estiver ligada a outras peças, vemos se o seu conjunto está bloqueado
            # (Por "bloqueado" entenda-se: todas as regiões adjacentes ao conjunto de peças têm peça mas nenhuma toca nesse conjunto)

            act = self.assignments[region]
            connected_set = self.board.bfs(*act[2][0])
            adj_regions = set()
            connected_set_regions = set()

            # extrair regiões pertencentes ao connected_set
            for r, c in connected_set:
                connected_set_regions.add(initial_board.get_value(r, c))

            # extrair regiões adjacentes ao connected_set
            for piece_region in connected_set_regions:
                for adj_region in adjacent_regions[piece_region]:
                    if adj_region not in connected_set_regions:
                        adj_regions.add(adj_region)

            adj_regions.remove(action[0])

            # se alguma região vizinha não tiver peça, então este conjunto de peças não está bloqueado
            if any(adj not in self.assignments.keys() for adj in adj_regions):
                continue

            # caso contrário temos que verificar se alguma dessas peças toca nas do connected_set
            for row, col in connected_set: # para cada posição no connected_set
                for new_row, new_col in self.board.orthogonal_positions(row, col): # e para cada posição ortogonal
                    if (new_row, new_col) in connected_set: # ignorar posições ortogonais dentro do próprio connected_set
                        continue
                    # se estiver encostado a outra peça, marcamos como não bloqueado
                    if not isinstance(self.board.get_value(new_row, new_col), int) or (new_row, new_col) in action[2]:
                        blocked = False
                        break
                if not blocked:
                    break 

            # se não encontrarmos nenhuma peça encostada ao connected_set, ele está bloqueado
            if blocked:
                return True
        return False

class Nuruomino(Problem):
    def __init__(self, board: Board):
        """O construtor especifica o estado inicial."""
        self.initial = NuruominoState(board)

        # calcula os tamanhos das regiões do tabuleiro
        regions = set()
        for row in range(1, self.initial.board.size + 1):
            for col in range(1, self.initial.board.size + 1):
                regions.add(self.initial.board.get_value(row, col))
                
        self.total_regions = len(regions)
        self.adjacent_regions = [[] for _ in range(self.total_regions + 1)] # guarda regiões adjacentes a cada região

        for r in range(1, self.total_regions + 1):
            self.adjacent_regions[r] = self.initial.board.adjacent_regions(r)
            self.initial.degree[r] = len(self.adjacent_regions[r])

        self.initial.all_actions = self.get_actions()
        self.initial.current_region = min(range(1, self.total_regions + 1), key= lambda r: (len(self.initial.all_actions[r]), -self.initial.degree[r]))

    def get_actions(self):
        """Devolve todas as ações possíveis para um tabuleiro vazio na forma de um dicionário do tipo
        < região > -> [ações possíveis nessa região]."""
        actions = [[] for _ in range(self.total_regions + 1)]
        state = self.initial

        for row in range(1, state.board.size + 1):
            for col in range(1, state.board.size + 1):
                # para cada entrada do dicionário FORMAS
                for forma in FORMAS:
                    # para cada variação da peça
                    for piece in FORMAS[forma]:
                        coordenadas = piece_coords(piece, (row, col))
                        cabe = True # indica se a peça cabe dentro da região (e da grelha) e se não se sobrepõe a outra peça
                       
                        if not state.board.is_inside_board(*coordenadas[0]):
                            continue

                        # queremos saber a regiao da peça mas (row, col) pode não fazer parte da peça
                        regiao = state.board.get_value(*coordenadas[0])

                        for c in coordenadas[1:]:
                            if not state.board.is_inside_board(*c) or state.board.get_value(*c) != regiao:
                                cabe = False
                                break
                        if cabe:
                            # verifica se a peça tem uma ligação com outra região
                            if state.board.orthogonal_region_in_coords(regiao, coordenadas):
                                continue

                            # cria a ação (região, peça, coordenadas)
                            action = (regiao, forma, coordenadas)
                            actions[regiao].append(action)

        for region in range(1, self.total_regions + 1):
            if len(self.adjacent_regions[region]) == 1:
                main_region = self.adjacent_regions[region][0]
                list_of_adjacent_coords = []
                list_of_coords = self.initial.board.orthogonal_region_in_position(region)
                adjacent_regions = self.adjacent_regions[main_region].copy()
                adjacent_regions.remove(region)
                for adjacent_region in adjacent_regions:
                    list_of_adjacent_coords += self.initial.board.orthogonal_region_in_position(adjacent_region)
                for action in actions[main_region][:]:
                    if set(list_of_coords) & set(action[2]) and set(list_of_adjacent_coords) & set(action[2]):
                        continue
                    actions[main_region].remove(action)

        return actions

    def actions(self, state: NuruominoState):
        """Retorna uma lista de ações que podem ser executadas a
        partir do estado passado como argumento.

        Define-se uma ação como 'a colocação de uma peça numa dada região'.
        Uma ação é um tuplo da forma (região, peça, coordenadas), onde 'região' indica a região onde
        a peça será colocada, 'peça' designa a peça a colocar e 'coordenadas' é a lista das coordenadas 
        que a peça vai ocupar."""
        unassigned = [r for r in range(1, self.total_regions + 1) if r not in state.assignments.keys()]

        # se unassigned tiver vazio, estamos na última região
        if not unassigned:
            return []

        # determinar a current_region (próxima região a explorar)

        # Heuristica MRV -- Regiao com menor numero de açoes possiveis -> len(state.all_actions[r])
        # Heurística do Maior Grau -- Em caso de empate, escolher região com mais regiões adjacentes -> -state.degree[r])
        state.current_region = min(unassigned, key= lambda r: (len(state.all_actions[r]), -state.degree[r]))

        return state.all_actions[state.current_region]
    
    def result(self, state: NuruominoState, action):
        """Retorna o estado resultante de executar a 'action' sobre
        'state' passado como argumento. A ação a executar deve ser uma
        das presentes na lista obtida pela execução de
        self.actions(state)."""
        new_state = state.copy(self.total_regions)

        # preenche tabuleiro com a peça
        new_state.assignments[action[0]] = action
        for row,col in action[2]:
            new_state.board.insert_value(row, col, action[1])

        new_state.occupied_regions.add(state.current_region)

        # Forward Checking -- Retirar do all_actions as ações que se tornaram inválidas
        adj_unassigned = [r for r in self.adjacent_regions[action[0]] if r not in new_state.assignments.keys()]
        for region in adj_unassigned:
            valid = []
            for act in new_state.all_actions[region]:
                # se act não violar nenhuma restrição
                if not new_state.conflicts(act, self.adjacent_regions, self.initial.board):
                    valid.append(act)
            new_state.all_actions[region] = valid

        # baixar o grau das regiões adjacentes
        for r in self.adjacent_regions[state.current_region]:
            new_state.degree[r] -= 1

        return new_state
        
    def goal_test(self, state: NuruominoState):
        """Retorna True se e só se o estado passado como argumento é
        um estado objetivo. Deve verificar se todas as posições do tabuleiro
        estão preenchidas de acordo com as regras do problema.
        
        Como garantimos que 'actions' só gera ações que não criem quadrados 2x2 e que não façam
        duas peças iguais tocarem-se (e uma vez que garantimos que todas as regiões são preenchidas),
        esta função só precisa de verificar se as peças estão conectadas."""

        # se não estivermos na ultima região, não vale a pena verificar nada
        if len(state.assignments.keys()) != self.total_regions:
            return False
        
        first = state.assignments[state.current_region][2][0]
        connected_set = state.board.bfs(*first) # lista de todas as posicoes ligadas
        
        # verificar que o numero de peças é igual ao numero de regiões
        if len(connected_set) != 4 * self.total_regions:
            return False
        
        return True 

    def h(self, node: Node):
        """Função heuristica utilizada para a procura A*.
        Não implementada - não foi usada."""

# Funções auxiliares
    
def violates_constraints(action1, action2, board:Board):
    # verificar se são peças iguais que se tocam
    if action1[1] == action2[1]:
        for r1, c1 in action1[2]:
            for r2, c2 in action2[2]:
                if (r2, c2) in board.orthogonal_positions(r1, c1):
                    return True
    
    # verificar se formam um quadrado 2x2
    all_coords = set(action1[2]) | set(action2[2])

    for (x, y) in all_coords:
        quadrado = {(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)}
        if quadrado.issubset(all_coords):
            return True
    return False

def revise(r1, r2, all_actions, board):
    """Usado no algoritmo AC-3.
    Torna r1 consistente em arco com r2. Devolve True caso o domínio de r1 tenha sido alterado."""
    revised = False
    actions_i = all_actions[r1]
    actions_j = all_actions[r2]

    new_actions_i = []

    for a_i in actions_i:
        supported = False
        for a_j in actions_j:
            if not violates_constraints(a_i, a_j, board):
                supported = True
                break
        if supported:
            new_actions_i.append(a_i)
        else:
            revised = True

    all_actions[r1] = new_actions_i
    return revised

def ac3(all_actions, problem:Nuruomino, total_regions):
    # Inicializa a "queue" com todos os pares de regiões adjacentes
    queue = []
    for r1 in range(1, total_regions + 1):
        for r2 in problem.adjacent_regions[r1]:
            queue.append((r1, r2))

    while queue:
        r1, r2 = queue.pop(0)
        if revise(r1, r2, all_actions, problem.initial.board):
            if len(all_actions[r1]) == 0:
                return False  # domínio vazio = inconsistente
            for rk in problem.adjacent_regions[r1]:
                if rk != r2:
                    queue.append((rk, r1))

    return True  # consistente

if __name__ == "__main__":
    board = Board.parse_instance()
    problem = Nuruomino(board)

    # AC-3 -- Pré Processamento que elimina ações
    ac3(problem.initial.all_actions, problem, problem.total_regions)

    goal_node = depth_first_tree_search(problem)
    goal_node.state.board.print_instance()
    
    # guardar número de estados (para o script de testes)
    import sys
    print(NuruominoState.state_id + 1, file=sys.stderr)

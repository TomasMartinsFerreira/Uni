#include <iostream>
#include <vector>
#include <unordered_set>
#include <queue>

using namespace std;

// executa uma BFS no grafo 'graph' começando no vértice 'start' e devolve a maior distância encontrada
int BFS(vector<vector<int>> &graph, int start, int numberOfNodes){
    vector<bool> visited(numberOfNodes + 1, false);
    vector<int> distance(numberOfNodes + 1, 0);
    queue<int> q;

    visited[start] = true;
    q.push(start);

    int maxDistance = 0;

    while (!q.empty()) {
        int node = q.front();
        q.pop();

        // visitar todos os nodes adjacentes
        for (int adj : graph[node]) {
            if (!visited[adj]) {
                visited[adj] = true;
                distance[adj] = distance[node] + 1;
                maxDistance = max(maxDistance, distance[adj]);
                q.push(adj);
            }
        }
    }

    // se houver uma linha desconectada (i.e. se a BFS nao conseguir visitar todos os vértices)
    for (int i = 1; i <= numberOfNodes; i++){
        if (!visited[i])
            maxDistance = -1;
    }

    return maxDistance;
}

int main(){
    size_t n; // estações
    int m; // ligações
    int l; // linhas

    // leitura da primeira linha de input
    cin >> n >> m >> l;

    // estrutura que guarda o que é dado no input desta forma:
    vector<unordered_set<int>> lines(l + 1);        // linha 1 -> 3, 2, 7, 5
                                                    // linha 2 -> 2, 6, 4
                                                    // linha 3 -> 1, 5

    // numero de estações conectadas a pelo menos uma outra estação
    // se este valor for menor que o número de estações, o resultado é -1
    unordered_set<int> connectedStations;
    // leitura do input
    for (int i = 0; i < m; i++){
        int station1, station2,line;
        cin >> station1 >> station2 >> line;

        lines[line].insert(station1);
        lines[line].insert(station2);
        connectedStations.insert(station1);
        connectedStations.insert(station2);
    }

    if (connectedStations.size() < n){
        cout << -1 << '\n';
        return 0;
    }

    // se alguma linha tiver todas as estações, o resultado é 0
    for (int i = 1; i < l; i++) {
        if (lines[i].size() == n) {
            cout << 0 << '\n';
            return 0;
        }
        
    }

    // grafo onde cada vértice representa uma linha do metro
    vector<vector<int>> graph(l + 1); // l + 1 porque ignoramos o indice 0

    // construção do grafo:
    // verifica se cada par de linhas tem algum elemento em comum
    // se tiver, então existe ligação entre as linhas
    for (int i = 1; i <= l; i++){
        for (int j = i + 1; j <= l; j++){
            for (int elem : lines[i]){
                // se houver um elemento igual
                if (lines[j].find(elem) != lines[j].end()){
                    // adicionar ao grafo as ligações
                    graph[i].push_back(j);
                    graph[j].push_back(i);
                    break;
                }
            }
        }   
    }

    int result = 0;

    // aplicar a BFS a cada node (linha) do grafo
    for (int node = 1; node <= l; node++){
        int lineChanges = BFS(graph, node, l);
        if (lineChanges == -1){
            result = lineChanges;
            break;
        }
        result = max(result, lineChanges);
    }

    cout << result << '\n';

    return 0;
}
#include <iostream>
#include <vector>
#include <unordered_map>
#include <string>
using namespace std;

// solução de programação dinâmica

// resolve o problema da sequencia "seq" e guarda os valores obtidos na matriz "matrix"
void solve(const vector<int>& seq, int sizeOfSequence, vector<vector<pair<unordered_map<int, vector<int>>, vector<int>>>>& matrix, const vector<vector<int>>& opMatrix, int sizeOfOpMatrix){
    // percorremos a matriz na diagonal
    for (int diagonal = 0; diagonal < sizeOfSequence; diagonal++){
        for (int i = 1; i + diagonal <= sizeOfSequence; i++){
            int j = i + diagonal;
            bool done = false;

            // hashtable/hashmap que guarda cada valor encontrado e os valores k, left e right que o originaram
            unordered_map<int, vector<int>> hashMap; // <valor, [k, left, right]>
            // vetor que indica em que ordem as entradas foram inseridas na hashtable
            // exemplo: inserimos uma entrada com chave=2 -> order[0] = 2
            //          inserimos uma outra entrada com chave=3 -> order[1] = 3 ...
            vector<int> order(sizeOfOpMatrix); 
            // index para ser usado neste vetor
            int index = 0; 

            // caso base
            if (j == i){ // não há parentização a fazer. Guardar o próprio número
                hashMap[seq[i]] = {0, 0, 0}; // estes valores a 0 não têm significado e não serão usados
                order[0] = seq[i];           // aqui só interessa mesmo o valor seq[i]
                matrix[i][j] = make_pair(hashMap, order);
                continue; // salta o for dos k's
            }

            // k percorre a sequencia começando na direita para se obter a parentização mais à esquerda
            for (int k = j; k > i; k--){
                if (done) break;
                // percorre os valores das entrdas [i][k-1] e [k][j]
                //      para ajudar a ler o código abaixo lembrar que matrix é do tipo matrix[i][j] = pair(hashtable, [1,3,2,...])
                for (size_t l_i = 0; l_i < matrix[i][k-1].first.size(); l_i++){
                    if (done) break;
                    int left = matrix[i][k-1].second[l_i];
                    for (size_t r_i = 0; r_i < matrix[k][j].first.size(); r_i++){
                        if (done) break;
                        int right = matrix[k][j].second[r_i];
                        int result = opMatrix[left][right];

                        // adicionar o valor à hashtable só se ele ainda não estiver lá
                        if (hashMap.find(result) == hashMap.end()){
                            hashMap[result] = {k, left, right};
                            order[index++] = result;
                        }
                        // se já vimos todos os valores possíveis não vale a pena continuar aqui
                        if (hashMap.size() == static_cast<size_t>(sizeOfOpMatrix)) // temos de converter para o mesmo tipo
                            done = true;
                    }
                }
            }
            matrix[i][j] = make_pair(hashMap, order);
        }
    }
}

// reconstroi a solução a partir da matriz
string getParenthesis(int i, int j, int target, vector<int>& seq, vector<vector<pair<unordered_map<int, vector<int>>, vector<int>>>>& matrix){
    // caso terminal
    if (i == j && seq[i] == target)
        return to_string(seq[i]);
    
    auto map_entry = matrix[i][j].first.find(target);
    if (map_entry == matrix[i][j].first.end())
        return ""; // no caso de não existir parentização é devolvida a string vazia

    // este vetor tem as entradas [k, left, right]
    vector<int> values = map_entry->second;

    // juntar o resultado da esquerda com o da direita
    return "(" + getParenthesis(i, values[0] - 1, values[1], seq, matrix) + " " 
    + getParenthesis(values[0], j, values[2], seq, matrix) + ")";
}

int main() {
    int sizeOfOpMatrix, sizeOfSequence;
    cin >> sizeOfOpMatrix >> sizeOfSequence;

    // matriz que vai guardar os valores obtidos
    // cada entrada é um par (hashtable, vetor)
    //      a hashtable é do tipo:  valor1: [k que originou esse valor, left value usado, right value usado]
    //                              valor2: [k que originou esse valor, left value usado, right value usado]
    //                              ...
    //      o vetor é do tipo:      [valor1, valor2, ...] pela ordem em que foram encontrados
    vector<vector<pair<unordered_map<int, vector<int>>, vector<int>>>> matrix(sizeOfSequence + 1, vector<pair<unordered_map<int, vector<int>>, vector<int>>>(sizeOfSequence + 1));

    // Lê a matriz do operador
    vector<vector<int>> opMatrix(sizeOfOpMatrix + 1, vector<int>(sizeOfOpMatrix + 1));
    for (int i = 1; i <= sizeOfOpMatrix; i++) {
        for (int j = 1; j <= sizeOfOpMatrix; j++) {
            cin >> opMatrix[i][j];
        }
    }

    // Lê a sequência
    vector<int> seq(sizeOfSequence + 1); // Começa do índice 1
    for (int i = 1; i <= sizeOfSequence; i++) {
        cin >> seq[i];
    }

    // Lê o resultado desejado
    int target;
    cin >> target;

    // Resolve o problema
    solve(seq, sizeOfSequence, matrix, opMatrix, sizeOfOpMatrix);

    // Imprime o resultado
    string result = getParenthesis(1, sizeOfSequence, target, seq, matrix);
    if (result != ""){
        cout << 1 << "\n" << result << "\n";
    } else {
        cout << 0 << "\n";
    }

    return 0;
}
import os
import time
import subprocess

# Caminho para os testes e para o programa
test_dir = "sample-nuruominoboards"
programa = "proj2425base-nuruomino/nuruomino.py"

tempo_limite = 50.00
average_time = 0.00

RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RESET = "\033[0m"

# Vai buscar todos os ficheiros .txt na pasta de testes
test_files = [f for f in os.listdir(test_dir) if f.endswith('.txt') and not f.endswith('.out.txt')]

# Corre cada teste
for test_file in sorted(test_files):
    test_path = os.path.join(test_dir, test_file)
    expected_path = test_path.replace(".txt", ".out")

    if not os.path.exists(expected_path):
        expected_path = test_path.replace(".txt",".out.txt")

    print(f"Teste: {test_file}", end="...\t")

    with open(test_path, 'r') as input_file:
        try:
            start = time.time()
            result = subprocess.run(
                ["python3", programa],
                stdin=input_file,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=tempo_limite,
                text=True
            )
            end = time.time()
            duration = end - start

            # Verificar output esperado
            if os.path.exists(expected_path):
                with open(expected_path, 'r') as expected_file:
                    esperado = expected_file.read().strip()
                    obtido = result.stdout.strip()
                    debug = result.stderr.strip()

                    num_estados = int(debug)

                    if obtido == esperado:
                        status = f"{GREEN}✔ Passou {RESET}"
                        sep = f"{GREEN}|{RESET}"
                    else:
                        status = f"{RED}✘ Falhou {RESET}"
                        sep = f"{RED}|{RESET}"

                    if num_estados < 100:
                        num_estados = f"{GREEN}{num_estados} {RESET}"
                    elif num_estados < 1000:
                        num_estados = f"{YELLOW}{num_estados} {RESET}"
                    else:
                        num_estados = f"{RED}{num_estados} {RESET}"
            average_time += duration
            print(f"{status}{duration:.3f} segundos{RESET}", end="\t")
            print(f"{sep} ESTADOS: {num_estados}")

        except subprocess.TimeoutExpired:
            print(f"{RED}Timeout após {tempo_limite} segundos{RESET}")
print(f"{GREEN}Média de tempo: {average_time / len(test_files):.3f} segundos{RESET}")
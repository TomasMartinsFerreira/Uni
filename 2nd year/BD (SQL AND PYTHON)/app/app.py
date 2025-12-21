

from flask import Flask, jsonify, request, g
import psycopg   # usado para que se possa ver num query, no caso de sql injection o input como um string sendo dificil a injection, pode haver outros casos como passar o malicious input como um object
from psycopg.rows import dict_row   # usado para os querys retornarem como dicionarios e nao como tuplos (da jeito porque os json sao dicionarios por isso facilita o processo)
import re
import datetime;



app = Flask(__name__)




DB_Config = {
    'dbname': 'aviacao',
    'user': 'postgres',
    'password': 'postgres',
    'host': 'postgres',
    'port': 5432,
}



def get_db_connection():
    if 'db' not in g:
        g.db = psycopg.connect(**DB_Config, row_factory=dict_row)
    return g.db




# g e uma variavel global no contexto de flask para guardar dados temporarios de um pedido
@app.teardown_appcontext
def close_db(error):
    db = g.pop('db', None)
    if db is not None:
        db.close()






@app.route("/", methods=["GET"])
def listar_aeroportos():
    conn = get_db_connection()
    cur = conn.cursor()
    
    cur.execute("SELECT nome, cidade FROM aeroporto;")
    aeroportos = cur.fetchall()

    cur.close()
    conn.close()
    
    # print(jsonify.dumps(cursor.fetchall()))

    return jsonify(aeroportos), 200







@app.route('/voos/<partida>', methods=["GET"])
def listar_voos(partida):
    if not re.fullmatch(r'[A-Z]{3}', partida):
        return jsonify({"error": "Codigo de aeroporto invalido"}), 400


    agora = datetime.datetime.now() + datetime.timedelta(hours=1)
    limite = agora + datetime.timedelta(hours=12)

    query = """
            SELECT  no_serie, hora_partida, chegada 
            FROM voo
            WHERE partida = %s AND hora_partida BETWEEN %s AND %s
            ORDER BY hora_partida ASC;
            """
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute(query, (partida, agora, limite))
    voos = cur.fetchall()

    cur.close()
    conn.close()

    return jsonify(voos), 200







@app.route('/voos/<partida>/<chegada>', methods=["GET"])
def listar_proximos_voos(partida, chegada):
    if not re.fullmatch(r'[A-Z]{3}', partida) or not re.fullmatch(r'[A-Z]{3}', chegada):
        return jsonify({"error": "Codigo de aeroporto invalido"}), 400
     

    query = """
            SELECT v.no_serie, v.hora_partida
            FROM voo v
            WHERE v.partida = %s AND v.chegada = %s 
            AND (
                (SELECT count(*) FROM bilhete b WHERE v.id = b.voo_id) 
                < 
                (SELECT count(*) FROM assento a WHERE a.no_serie = v.no_serie))
            AND v.hora_partida > NOW()
            ORDER BY v.hora_partida ASC
            LIMIT 3;
    """
     
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute(query, (partida, chegada))
    voos = cur.fetchall()

    cur.close()
    conn.close()

    return jsonify(voos), 200




@app.route('/compra/<int:voo>', methods=["POST"])
def compra_bilhetes(voo):
    '''
Exemplo do jSON que temos de receber
    {
      "nif": "123456789",
      "passageiros": [
        {"nome": "João", "prim_classe": true},
        {"nome": "Ana", "prim_classe": false}
      ]
    }
    '''

    dados = request.get_json()

    nif = dados.get("nif")
    passageiros = dados.get("passageiros")

    if not nif or not passageiros:
        return jsonify({"erro": "Dados incompletos"}), 400

    conn = get_db_connection()
    # para evitar update de dados enquanto se faz uma leitura
    try:
        with conn:
            with conn.cursor() as cur:

                cur.execute("""
                    SELECT no_serie, hora_partida
                    FROM voo
                    WHERE id = %s
                """, (voo,))
                voo_data = cur.fetchone() # returnar apenas 1 voo, nao e necessario o fetchall()
                if not voo_data:
                    return jsonify({"erro": "Voo não existe"}), 404

                if datetime.datetime.now() + datetime.timedelta(hours=1) >= voo_data["hora_partida"]:
                    return jsonify({"erro": "Voo já partiu"}), 400

                no_serie = voo_data["no_serie"]

                
                cur.execute("""
                    INSERT INTO venda (nif_cliente, hora)
                    VALUES (%s, NOW())
                    RETURNING codigo_reserva
                    """, (nif,))
                res = cur.fetchone()
                if not res or "codigo_reserva" not in res:
                    return jsonify({"erro": "Falha ao obter código de reserva"}), 500
                codigo_reserva = res["codigo_reserva"]


                
                for p in passageiros:
                    nome = p.get("nome")
                    prim_classe = p.get("prim_classe")

                    if nome is None or prim_classe is None:
                        raise Exception("Dados de passageiro incompletos")

                    
                    cur.execute("""
                        SELECT lugar FROM assento
                        WHERE no_serie = %s AND prim_classe = %s
                        AND lugar NOT IN (
                            SELECT lugar FROM bilhete WHERE voo_id = %s AND lugar IS NOT NULL
                        )
                        LIMIT 1
                    """, (no_serie, prim_classe, voo))
                    lugar_data = cur.fetchone()
                    if not lugar_data:
                        raise Exception("Sem lugares disponiveis na respetiva classe")
                    

                    preco = 200 if prim_classe else 100
                    cur.execute("""
                        INSERT INTO bilhete (voo_id, codigo_reserva, nome_passegeiro, preco, prim_classe, lugar, no_serie)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """, (voo, codigo_reserva, nome, preco, prim_classe, None, no_serie))

        return jsonify({
            "mensagem": "Compra realizada com sucesso",
            "codigo_reserva": codigo_reserva
        }), 201


    except Exception as e:
        conn.rollback()
        return jsonify({"erro": str(e)}), 400
    finally:
        conn.close()






@app.route('/checkin/<bilhete>', methods=["POST"])
def check_in(bilhete):
    
    
    conn = get_db_connection()
    
    try:
        with conn:
            with conn.cursor() as cur:

                cur.execute("SELECT lugar, voo_id, prim_classe FROM bilhete WHERE id = %s", (bilhete,))
                bilhete_info = cur.fetchone()
                if bilhete_info is None:
                    raise Exception("Bilhete nao existe")
                if bilhete_info["lugar"] is not None:
                    raise Exception("Check-in ja efetuado")


                #verificar que o check-in nao e feito depois da hora
                cur.execute("SELECT hora_partida FROM voo WHERE id = %s", (bilhete_info["voo_id"],))

                data_partida_voo = cur.fetchone()

                if data_partida_voo is None:   # data_partida_voo["hora_partida"] errado porque significaria que dentro da var estava um dicionario inserido contendo a "hora_partida"
                    raise Exception("Voo não existe")
                if datetime.datetime.now() + datetime.timedelta(hours=1) > data_partida_voo["hora_partida"]:
                    raise Exception("Voo ja partiu")

                cur.execute("""
                    SELECT lugar FROM assento
                    WHERE no_serie = (SELECT no_serie FROM voo WHERE id = %s)
                    AND prim_classe = %s
                    AND lugar NOT IN (
                        SELECT lugar FROM bilhete WHERE voo_id = %s AND lugar IS NOT NULL
                    )
                    LIMIT 1
                """, (bilhete_info["voo_id"], bilhete_info["prim_classe"], bilhete_info["voo_id"]))
                lugar_livre = cur.fetchone()

                if lugar_livre is None:
                    raise Exception ("nao ha lugares livres, ja tera sido efetuada o check-in")

                cur.execute("UPDATE bilhete SET lugar = %s WHERE id = %s", (lugar_livre["lugar"], bilhete))

                return jsonify({"mensagem": "check-in realizado com sucesso"}), 201
    except Exception as e:
        return jsonify({"erro": str(e)}), 400
    finally:
        if conn is not None:
            conn.close()

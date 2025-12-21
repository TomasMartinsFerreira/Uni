TRUNCATE TABLE bilhete, venda, voo, assento, aviao, aeroporto RESTART IDENTITY CASCADE;


-- Aeroportos --

INSERT INTO aeroporto (codigo, nome, cidade, pais) VALUES
('LHR', 'Heathrow Airport', 'Londres', 'Reino Unido'),
('LGW', 'Gatwick Airport', 'Londres', 'Reino Unido'),
('CDG', 'Charles de Gaulle Airport', 'Paris', 'França'),
('ORY', 'Orly Airport', 'Paris', 'França'),
('AMS', 'Amsterdam Airport Schiphol', 'Amsterdão', 'Holanda'),
('FRA', 'Frankfurt Airport', 'Frankfurt', 'Alemanha'),
('MAD', 'Adolfo Suárez Madrid-Barajas', 'Madrid', 'Espanha'),
('BCN', 'Barcelona-El Prat Airport', 'Barcelona', 'Espanha'),
('FCO', 'Leonardo da Vinci-Fiumicino', 'Roma', 'Itália'),
('MUC', 'Munich Airport', 'Munique', 'Alemanha'),
('IST', 'Istanbul Airport', 'Istambul', 'Turquia'),
('ZRH', 'Zurich Airport', 'Zurique', 'Suíça'),
('CPH', 'Copenhagen Airport', 'Copenhaga', 'Dinamarca'),
('OSL', 'Oslo Airport', 'Oslo', 'Noruega'),
('ARN', 'Stockholm Arlanda Airport', 'Estocolmo', 'Suécia');



-- Aviões --

INSERT INTO aviao (no_serie, modelo) VALUES
('A320-001', 'Airbus A320'),
('A320-002', 'Airbus A320'),
('A320-003', 'Airbus A320'),
('A320-004', 'Airbus A320'),
('B737-001', 'Boeing 737'),
('B737-002', 'Boeing 737'),
('B737-003', 'Boeing 737'),
('B737-004', 'Boeing 737'),
('A380-001', 'Airbus A380'),
('A380-002', 'Airbus A380');



-- Assentos --

INSERT INTO assento (lugar, no_serie, prim_classe)
SELECT 
    fileira::text || letra as lugar,
    no_serie,
    fileira <= 3 as prim_classe -- Primeiras 3 fileiras (10% de 30) são 1a classe
FROM 
    (SELECT no_serie FROM aviao WHERE modelo = 'Airbus A320') as avioes,
    (SELECT generate_series(1, 30) as fileira),
    (SELECT unnest(ARRAY['A', 'B', 'C', 'D', 'E', 'F']) as letra);

-- Inserindo assentos para Boeing 737 (configuração típica: 35 fileiras, 6 assentos por fileira - ABC DEF)
INSERT INTO assento (lugar, no_serie, prim_classe)
SELECT 
    fileira::text || letra as lugar,
    no_serie,
    fileira <= 4 as prim_classe -- Primeiras 4 fileiras (~11% de 35) são 1a classe
FROM 
    (SELECT no_serie FROM aviao WHERE modelo = 'Boeing 737') as avioes,
    (SELECT generate_series(1, 35) as fileira),
    (SELECT unnest(ARRAY['A', 'B', 'C', 'D', 'E', 'F']) as letra);

-- Inserindo assentos para Airbus A380 (configuração típica: 50 fileiras, 10 assentos por fileira - ABC DEFG HJK)
INSERT INTO assento (lugar, no_serie, prim_classe)
SELECT 
    fileira::text || letra as lugar,
    no_serie,
    fileira <= 5 as prim_classe -- Primeiras 5 fileiras (10% de 50) são 1a classe
FROM 
    (SELECT no_serie FROM aviao WHERE modelo = 'Airbus A380') as avioes,
    (SELECT generate_series(1, 50) as fileira),
    (SELECT unnest(ARRAY['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K']) as letra);





-- Voo --

DO $$
DECLARE
    data_atual DATE := '2025-01-01';
    aeroporto_atual CHAR(3);
    proximo_aeroporto CHAR(3);
    aviao_atual VARCHAR(80);
    hora_partida TIMESTAMP;
    hora_chegada TIMESTAMP;
    duracao_voo INTERVAL;
    aeroportos CHAR(3)[] := ARRAY['LHR','LGW','CDG','ORY','AMS','FRA','MAD','BCN','FCO','MUC','IST','ZRH','CPH','OSL','ARN'];
    -- Major hub routes that all aircraft must fly
    rotas_principais CHAR(3)[][] := ARRAY[
        ['LHR', 'CDG'], ['CDG', 'LHR'],  -- London-Paris
        ['FRA', 'AMS'], ['AMS', 'FRA'],  -- Frankfurt-Amsterdam
        ['MAD', 'BCN'], ['BCN', 'MAD']   -- Madrid-Barcelona
    ];
    i INT;
    num_voos_dia INT;
    avioes_info JSONB;
    aviao_record RECORD;
    route_index INT;
    route_flags JSONB;
BEGIN
    -- Initialize aircraft locations and route completion flags
    SELECT jsonb_object_agg(no_serie, 
           jsonb_build_object(
               'location', aeroportos[1 + floor(random() * array_length(aeroportos, 1))::INT],
               'completed_routes', '[]'::jsonb
           ))
    INTO avioes_info
    FROM aviao;
    
    WHILE data_atual <= '2025-07-31' LOOP
        num_voos_dia := 5 + floor(random() * 6)::INT;  -- 5-10 flights per day
        
        FOR k IN 1..num_voos_dia LOOP
            -- Select random aircraft
            SELECT no_serie INTO aviao_atual FROM aviao ORDER BY random() LIMIT 1;
            aeroporto_atual := (avioes_info->aviao_atual->>'location')::CHAR(3);
            
            -- Check if aircraft needs to complete a mandatory route
            route_index := NULL;
            FOR i IN 1..array_length(rotas_principais, 1) LOOP
                IF rotas_principais[i][1] = aeroporto_atual AND
                   NOT (avioes_info->aviao_atual->'completed_routes')::jsonb @> to_jsonb(i) THEN
                    route_index := i;
                    EXIT;
                END IF;
            END LOOP;
            
            IF route_index IS NOT NULL THEN
                -- Assign mandatory route
                proximo_aeroporto := rotas_principais[route_index][2];
                
                -- Mark route as completed for this aircraft
                avioes_info := jsonb_set(
                    avioes_info, 
                    ARRAY[aviao_atual, 'completed_routes'], 
                    (avioes_info->aviao_atual->'completed_routes')::jsonb || to_jsonb(route_index)
                );
            ELSE
                -- Select random destination different from origin
                i := 1 + floor(random() * array_length(aeroportos, 1))::INT;
                proximo_aeroporto := aeroportos[i];
                WHILE proximo_aeroporto = aeroporto_atual LOOP
                    i := 1 + floor(random() * array_length(aeroportos, 1))::INT;
                    proximo_aeroporto := aeroportos[i];
                END LOOP;
            END IF;
            
            -- Generate flight times
            hora_partida := data_atual + TIME '06:00' + (random() * 12 * INTERVAL '1 hour');
            duracao_voo := INTERVAL '1 hour' + (random() * INTERVAL '3 hours');
            hora_chegada := hora_partida + duracao_voo;
            
            -- Insert flight
            INSERT INTO voo (no_serie, hora_partida, hora_chegada, partida, chegada)
            VALUES (aviao_atual, hora_partida, hora_chegada, aeroporto_atual, proximo_aeroporto);
            
            -- Update aircraft location
            avioes_info := jsonb_set(
                avioes_info, 
                ARRAY[aviao_atual, 'location'], 
                to_jsonb(proximo_aeroporto)
            );
            
            -- Schedule return flight if time permits (optional)
            IF hora_chegada::TIME < TIME '22:00' AND random() < 0.5 THEN
                hora_partida := hora_chegada + INTERVAL '1 hour';
                hora_chegada := hora_partida + duracao_voo;
                
                INSERT INTO voo (no_serie, hora_partida, hora_chegada, partida, chegada)
                VALUES (aviao_atual, hora_partida, hora_chegada, proximo_aeroporto, aeroporto_atual);
                
                avioes_info := jsonb_set(
                    avioes_info, 
                    ARRAY[aviao_atual, 'location'], 
                    to_jsonb(aeroporto_atual)
                );
            END IF;
        END LOOP;
        
        -- Reset completed routes at start of each month
        IF EXTRACT(DAY FROM data_atual) = 1 THEN
            FOR aviao_record IN SELECT no_serie FROM aviao LOOP
                avioes_info := jsonb_set(
                    avioes_info, 
                    ARRAY[aviao_record.no_serie, 'completed_routes'], 
                    '[]'::jsonb
                );
            END LOOP;
        END IF;
        
        data_atual := data_atual + INTERVAL '1 day';
    END LOOP;
END $$;





-- Vendas --

DO $$
DECLARE
    voo_record RECORD;
    assento_record RECORD;
    venda_id INT;
    passageiros TEXT[] := ARRAY['João Silva', 'Maria Santos', 'Carlos Oliveira', 'Ana Pereira', 'Pedro Costa', 'Sofia Almeida', 'Miguel Rodrigues', 'Inês Ferreira', 'Tiago Gomes', 'Beatriz Sousa'];
    aeroportos CHAR(3)[] := ARRAY['LHR','LGW','CDG','ORY','AMS','FRA','MAD','BCN','FCO','MUC','IST','ZRH','CPH','OSL','ARN'];
    nif_base TEXT := '10000000';
    preco_base NUMERIC;
    venda_hora TIMESTAMP;
    passenger_name TEXT;
    random_index INT;
    bilhetes_por_venda INT;
    bilhetes_vendidos INT;
    used_passenger_names TEXT[] := '{}'; -- Array para controlar nomes já usados na venda

    percentagem_1a NUMERIC := 0.7;  -- 70% dos assentos de 1ª classe
    percentagem_2a NUMERIC := 0.5;  -- 50% dos assentos de 2ª classe

    assentos_1a INT;
    assentos_2a INT;
    total_assentos_vendidos INT;
    
BEGIN
    RAISE NOTICE 'Iniciando a venda de bilhetes agrupados por menos transações...';

    FOR voo_record IN SELECT * FROM voo LOOP
        assentos_1a := 0;
        assentos_2a := 0;
        total_assentos_vendidos := 0;

        -- Conta total de assentos disponíveis por classe
        SELECT COUNT(*) INTO assentos_1a 
        FROM assento 
        WHERE no_serie = voo_record.no_serie AND prim_classe = TRUE;

        SELECT COUNT(*) INTO assentos_2a 
        FROM assento 
        WHERE no_serie = voo_record.no_serie AND prim_classe = FALSE;

        -- Calcula quantos vender
        assentos_1a := floor(assentos_1a * percentagem_1a);
        assentos_2a := floor(assentos_2a * percentagem_2a);

        -- Processar 1ª classe
        bilhetes_vendidos := 0;
        WHILE bilhetes_vendidos < assentos_1a LOOP
            -- Resetar array de nomes usados para cada nova venda
            used_passenger_names := '{}';
            
            -- Definir quantos bilhetes nesta venda (1-4)
            bilhetes_por_venda := 1 + floor(random() * 4)::INT;
            IF bilhetes_por_venda > (assentos_1a - bilhetes_vendidos) THEN
                bilhetes_por_venda := assentos_1a - bilhetes_vendidos;
            END IF;

            -- Criar venda
            venda_hora := voo_record.hora_partida - INTERVAL '10 days' + (random() * INTERVAL '10 days');
            random_index := 1 + floor(random() * array_length(aeroportos, 1))::INT;

            INSERT INTO venda (nif_cliente, balcao, hora)
            VALUES (
                nif_base || (floor(random()*10)::TEXT),
                aeroportos[random_index],
                venda_hora
            )
            RETURNING codigo_reserva INTO venda_id;

            -- Inserir bilhetes para esta venda
            FOR i IN 1..bilhetes_por_venda LOOP
                -- Encontrar assento disponível
                SELECT * INTO assento_record FROM assento a 
                WHERE a.no_serie = voo_record.no_serie AND prim_classe = TRUE
                AND NOT EXISTS (
                    SELECT 1 FROM bilhete b
                    WHERE b.voo_id = voo_record.id
                    AND b.no_serie = a.no_serie
                    AND b.lugar = a.lugar
                )
                LIMIT 1;

                IF NOT FOUND THEN
                    EXIT;
                END IF;

                -- Gerar nome único para esta venda
                LOOP
                    passenger_name := passageiros[1 + floor(random() * array_length(passageiros, 1))::INT] || ' ' || (floor(random() * 1000))::TEXT;
                    EXIT WHEN NOT (passenger_name = ANY(used_passenger_names));
                END LOOP;
                
                used_passenger_names := array_append(used_passenger_names, passenger_name);
                preco_base := 300 + random() * 700;

                INSERT INTO bilhete (
                    voo_id, codigo_reserva, nome_passegeiro, preco, prim_classe, lugar, no_serie
                ) VALUES (
                    voo_record.id,
                    venda_id,
                    passenger_name,
                    preco_base,
                    TRUE,
                    assento_record.lugar,
                    assento_record.no_serie
                );

                bilhetes_vendidos := bilhetes_vendidos + 1;
                total_assentos_vendidos := total_assentos_vendidos + 1;
            END LOOP;
        END LOOP;

        -- Processar 2ª classe
        bilhetes_vendidos := 0;
        WHILE bilhetes_vendidos < assentos_2a LOOP
            -- Resetar array de nomes usados para cada nova venda
            used_passenger_names := '{}';
            
            -- Definir quantos bilhetes nesta venda (1-6, pois 2ª classe pode ter mais pessoas juntas)
            bilhetes_por_venda := 1 + floor(random() * 6)::INT;
            IF bilhetes_por_venda > (assentos_2a - bilhetes_vendidos) THEN
                bilhetes_por_venda := assentos_2a - bilhetes_vendidos;
            END IF;

            -- Criar venda
            venda_hora := voo_record.hora_partida - INTERVAL '10 days' + (random() * INTERVAL '10 days');
            random_index := 1 + floor(random() * array_length(aeroportos, 1))::INT;

            INSERT INTO venda (nif_cliente, balcao, hora)
            VALUES (
                nif_base || (floor(random()*10)::TEXT),
                aeroportos[random_index],
                venda_hora
            )
            RETURNING codigo_reserva INTO venda_id;

            -- Inserir bilhetes para esta venda
            FOR i IN 1..bilhetes_por_venda LOOP
                -- Encontrar assento disponível
                SELECT * INTO assento_record FROM assento a 
                WHERE a.no_serie = voo_record.no_serie AND prim_classe = FALSE
                AND NOT EXISTS (
                    SELECT 1 FROM bilhete b
                    WHERE b.voo_id = voo_record.id
                    AND b.no_serie = a.no_serie
                    AND b.lugar = a.lugar
                )
                LIMIT 1;

                IF NOT FOUND THEN
                    EXIT;
                END IF;

                -- Gerar nome único para esta venda
                LOOP
                    passenger_name := passageiros[1 + floor(random() * array_length(passageiros, 1))::INT] || ' ' || (floor(random() * 1000))::TEXT;
                    EXIT WHEN NOT (passenger_name = ANY(used_passenger_names));
                END LOOP;
                
                used_passenger_names := array_append(used_passenger_names, passenger_name);
                preco_base := 50 + random() * 250;

                INSERT INTO bilhete (
                    voo_id, codigo_reserva, nome_passegeiro, preco, prim_classe, lugar, no_serie
                ) VALUES (
                    voo_record.id,
                    venda_id,
                    passenger_name,
                    preco_base,
                    FALSE,
                    assento_record.lugar,
                    assento_record.no_serie
                );

                bilhetes_vendidos := bilhetes_vendidos + 1;
                total_assentos_vendidos := total_assentos_vendidos + 1;
            END LOOP;
        END LOOP;

        RAISE NOTICE 'Voo %: vendidos % bilhetes em menos transações', 
            voo_record.id, total_assentos_vendidos;
    END LOOP;

    RAISE NOTICE 'Processo concluído. Bilhetes vendidos agrupados em menos transações.';
END $$;









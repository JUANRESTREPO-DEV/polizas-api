-- Individuales: tomador = asegurado = arrendatario; beneficiario = arrendador. Un único riesgo.
-- Colectivas: tomador = inmobiliaria o copropiedad; canon de la póliza = suma de riesgos activos.
-- Prima = canon mensual x meses de vigencia.

INSERT INTO poliza (id, numero, tipo, estado, tomador_tipo_documento, tomador_documento, tomador_nombre,
                    fecha_inicio_vigencia, fecha_fin_vigencia, meses_vigencia, canon_mensual, valor_prima, fecha_cancelacion)
VALUES (1, 'ARR-IND-000101', 'INDIVIDUAL', 'ACTIVA',    'CC',  '1020304050', 'Laura Marcela Gómez Ríos',
        DATE '2026-02-01', DATE '2027-02-01', 12, 2500000.00, 30000000.00, NULL),
       (2, 'ARR-IND-000102', 'INDIVIDUAL', 'CANCELADA', 'CC',  '79456123',   'Andrés Felipe Castaño Mora',
        DATE '2025-11-15', DATE '2026-11-15', 12, 1850000.00, 22200000.00, TIMESTAMP '2026-05-20 10:32:00'),
       (3, 'ARR-IND-000103', 'INDIVIDUAL', 'RENOVADA',  'CE',  '4587219',    'María José Pérez Salazar',
        DATE '2026-07-01', DATE '2027-01-01', 6,  3156000.00, 18936000.00, NULL),
       (4, 'ARR-COL-000201', 'COLECTIVA',  'ACTIVA',    'NIT', '900123456-7', 'Inmobiliaria Andina S.A.S.',
        DATE '2026-01-01', DATE '2027-01-01', 12, 7100000.00, 85200000.00, NULL),
       (5, 'ARR-COL-000202', 'COLECTIVA',  'ACTIVA',    'NIT', '901234567-1', 'Conjunto Residencial Altos del Retiro P.H.',
        DATE '2026-04-01', DATE '2026-10-01', 6,  1500000.00, 9000000.00, NULL),
       (6, 'ARR-COL-000203', 'COLECTIVA',  'CANCELADA', 'NIT', '900765432-3', 'Arrendamientos del Caribe Ltda.',
        DATE '2025-09-01', DATE '2026-09-01', 12, 2700000.00, 32400000.00, TIMESTAMP '2026-03-10 15:05:00');

INSERT INTO riesgo (id, poliza_id, direccion_inmueble, ciudad,
                    asegurado_tipo_documento, asegurado_documento, asegurado_nombre,
                    beneficiario_tipo_documento, beneficiario_documento, beneficiario_nombre,
                    canon_mensual, estado, fecha_cancelacion)
VALUES (1,  1, 'Carrera 7 # 72-41 Apto 502', 'Bogotá D.C.',
        'CC', '1020304050', 'Laura Marcela Gómez Ríos',     'CC', '19345876', 'Carlos Alberto Mejía Duque',
        2500000.00, 'ACTIVO', NULL),
       (2,  2, 'Calle 10 Sur # 43A-15 Casa 3', 'Medellín',
        'CC', '79456123', 'Andrés Felipe Castaño Mora',     'CC', '43567890', 'Gloria Inés Restrepo Vélez',
        1850000.00, 'CANCELADO', TIMESTAMP '2026-05-20 10:32:00'),
       (3,  3, 'Avenida 6N # 25-60 Apto 1102', 'Cali',
        'CE', '4587219', 'María José Pérez Salazar',        'NIT', '900345678-2', 'Inversiones Pance S.A.S.',
        3156000.00, 'ACTIVO', NULL),
       (4,  4, 'Calle 93 # 11-26 Apto 301', 'Bogotá D.C.',
        'CC', '1032456789', 'Juliana Andrea Torres León',   'CC', '51789456', 'Martha Lucía Rincón Parra',
        1800000.00, 'ACTIVO', NULL),
       (5,  4, 'Carrera 15 # 118-40 Apto 704', 'Bogotá D.C.',
        'CC', '80123987', 'Sergio Iván Moreno Quintero',    'CC', '17654321', 'Hernando José Villamil Ortiz',
        2200000.00, 'ACTIVO', NULL),
       (6,  4, 'Calle 147 # 19-50 Casa 12', 'Bogotá D.C.',
        'CC', '1015478963', 'Camila Fernanda Rojas Díaz',   'NIT', '860012345-9', 'Rentas del Norte S.A.',
        3100000.00, 'ACTIVO', NULL),
       (7,  5, 'Carrera 43A # 5A-113 Apto 1502', 'Medellín',
        'CC', '1037589412', 'Santiago Uribe Escobar',       'CC', '70123456', 'Jorge Mario Arango Botero',
        1500000.00, 'ACTIVO', NULL),
       (8,  5, 'Carrera 43A # 5A-113 Apto 902', 'Medellín',
        'CC', '1128456321', 'Valentina Zapata Henao',       'CC', '32456789', 'Luz Marina Ochoa Giraldo',
        1350000.00, 'CANCELADO', TIMESTAMP '2026-06-15 09:00:00'),
       (9,  6, 'Carrera 54 # 72-80 Apto 402', 'Barranquilla',
        'CC', '72234567', 'Ricardo Enrique Barros Pineda',  'CC', '22456123', 'Rosa Elena Char Abdala',
        1200000.00, 'CANCELADO', TIMESTAMP '2026-03-10 15:05:00'),
       (10, 6, 'Calle 84 # 51B-20 Apto 801', 'Barranquilla',
        'CC', '1143789654', 'Daniela Paola Fontalvo Ruiz',  'CC', '8745123', 'Álvaro Enrique De la Hoz Pérez',
        1500000.00, 'CANCELADO', TIMESTAMP '2026-03-10 15:05:00');

-- La póliza 3 ya fue renovada una vez: 3.000.000 x 1,052 = 3.156.000.
INSERT INTO historial_renovacion (id, poliza_id, fecha_renovacion, porcentaje_ipc, canon_anterior, canon_nuevo,
                                  prima_anterior, prima_nueva, inicio_vigencia_anterior, fin_vigencia_anterior,
                                  inicio_vigencia_nueva, fin_vigencia_nueva)
VALUES (1, 3, TIMESTAMP '2026-06-25 08:15:00', 5.2000, 3000000.00, 3156000.00, 18000000.00, 18936000.00,
        DATE '2026-01-01', DATE '2026-07-01', DATE '2026-07-01', DATE '2027-01-01');

ALTER TABLE poliza ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE riesgo ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE historial_renovacion ALTER COLUMN id RESTART WITH 1000;

-- Datos de prueba de Reputation para la demo (OFICIO-214). Solo se cargan con el perfil datos-demo.
-- Los trabajadores 101 a 107 son los simulados en Search, con su mismo promedio y número de reseñas.
-- El trabajador 108 está pausado, por eso Search no lo devuelve. Si los datos ya existen, no se hace nada.

DO $$
DECLARE
    t             RECORD;
    reputacion_pk INTEGER;
    servicio_pk   INTEGER;
    puntuaciones  INTEGER[] := ARRAY[2, 2, 2, 2, 2, 3, 3, 3, 3, 2];
    comentarios   TEXT[] := ARRAY[
        'Muy puntual y profesional. Solucionó todo y dejó limpio.',
        'Excelente trabajo, lo recomiendo.',
        'Explicó bien el problema y cumplió con lo acordado.',
        'Buen servicio y buen trato.',
        'Llegó a tiempo y terminó antes de lo esperado.'];
    quejas        TEXT[] := ARRAY[
        'Llegó muy tarde y dejó el trabajo a medias.',
        'Pésimo servicio, no solucionó el problema.',
        'No cumplió con el horario acordado.',
        'Tuve que llamar a otra persona para terminar.',
        'El resultado no fue el esperado.'];
BEGIN
    IF EXISTS (SELECT 1 FROM servicio_calificable WHERE solicitud_id = 900001) THEN
        RETURN;
    END IF;

    -- Trabajadores activos: "cuatros" es cuántas reseñas tienen 4 estrellas; las demás tienen 5.
    FOR t IN
        SELECT * FROM (VALUES (101, 128, 13, 0.0), (102, 96, 10, 0.0), (103, 84, 17, 0.0), (104, 41, 12, 0.5),
                              (105, 63, 25, 0.0), (106, 22, 7, 0.0), (107, 57, 6, 0.0))
            AS v(trabajador_id, total, cuatros, penalizacion)
    LOOP
        INSERT INTO reputacion (tipo, promedio_calificaciones, total_resenas, fecha_actualizacion)
        VALUES ('TRABAJADOR', ROUND((t.total * 5 - t.cuatros)::NUMERIC / t.total, 2), t.total, NOW())
        RETURNING id INTO reputacion_pk;

        INSERT INTO reputacion_trabajador (id, trabajador_id, penalizacion_acumulada, estado)
        VALUES (reputacion_pk, t.trabajador_id, t.penalizacion, 'ACTIVO');

        FOR i IN 1..t.total LOOP
            INSERT INTO servicio_calificable (solicitud_id, trabajador_id, contratante_id, fecha_cumplida,
                                              calificado_por_contratante)
            VALUES (t.trabajador_id * 1000 + i, t.trabajador_id, 201 + i % 10, NOW() - i * INTERVAL '31 hours', TRUE)
            RETURNING id INTO servicio_pk;

            INSERT INTO resena (codigo, servicio_id, reputacion_id, autor_id, tipo_autor, calificado_id,
                                tipo_calificado, puntuacion, comentario, fecha_creacion)
            VALUES (gen_random_uuid(), servicio_pk, reputacion_pk, 201 + i % 10, 'CONTRATANTE', t.trabajador_id,
                    'TRABAJADOR',
                    CASE WHEN i * t.cuatros / t.total <> (i - 1) * t.cuatros / t.total THEN 4 ELSE 5 END,
                    CASE WHEN i % 4 = 0 THEN NULL ELSE comentarios[1 + i % 5] END,
                    NOW() - i * INTERVAL '30 hours');
        END LOOP;
    END LOOP;

    -- Penalización de la trabajadora 104: 4,7 de promedio menos 0,5 da los 4,2 que muestra Search.
    INSERT INTO penalizacion (reputacion_trabajador_id, solicitud_id, puntos, motivo, fecha_hora_servicio,
                              fecha_cancelacion, fecha_aplicacion)
    SELECT id, 104900, 0.5, 'Cancelación con menos de 24 horas de anticipación', NOW() - INTERVAL '9 days',
           NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'
    FROM reputacion_trabajador WHERE trabajador_id = 104;

    -- Trabajador pausado: promedio de 2,4 y las últimas 5 reseñas por debajo de 3.
    INSERT INTO reputacion (tipo, promedio_calificaciones, total_resenas, fecha_actualizacion)
    VALUES ('TRABAJADOR', 2.4, 10, NOW())
    RETURNING id INTO reputacion_pk;

    INSERT INTO reputacion_trabajador (id, trabajador_id, penalizacion_acumulada, estado, fecha_pausado)
    VALUES (reputacion_pk, 108, 0, 'PAUSADO', NOW() - INTERVAL '1 day');

    FOR i IN 1..10 LOOP
        INSERT INTO servicio_calificable (solicitud_id, trabajador_id, contratante_id, fecha_cumplida,
                                          calificado_por_contratante)
        VALUES (108000 + i, 108, 201 + i % 10, NOW() - i * INTERVAL '4 days', TRUE)
        RETURNING id INTO servicio_pk;

        INSERT INTO resena (codigo, servicio_id, reputacion_id, autor_id, tipo_autor, calificado_id,
                            tipo_calificado, puntuacion, comentario, fecha_creacion)
        VALUES (gen_random_uuid(), servicio_pk, reputacion_pk, 201 + i % 10, 'CONTRATANTE', 108, 'TRABAJADOR',
                puntuaciones[i], quejas[1 + i % 5], NOW() - i * INTERVAL '4 days' + INTERVAL '2 hours');
    END LOOP;

    -- Reportes de reseñas: tres pendientes para moderar y uno ya resuelto.
    INSERT INTO reporte_resena (resena_id, reportante_id, motivo, descripcion, estado, fecha_reporte)
    SELECT r.id, 108, 'OFENSIVA', 'El comentario es irrespetuoso.', 'PENDIENTE', NOW() - INTERVAL '6 hours'
    FROM resena r JOIN servicio_calificable s ON s.id = r.servicio_id WHERE s.solicitud_id = 108001;

    INSERT INTO reporte_resena (resena_id, reportante_id, motivo, descripcion, estado, fecha_reporte)
    SELECT r.id, 108, 'FALSA', 'Ese servicio sí se terminó.', 'PENDIENTE', NOW() - INTERVAL '5 hours'
    FROM resena r JOIN servicio_calificable s ON s.id = r.servicio_id WHERE s.solicitud_id = 108002;

    INSERT INTO reporte_resena (resena_id, reportante_id, motivo, descripcion, estado, fecha_reporte)
    SELECT r.id, 104, 'INAPROPIADA', NULL, 'PENDIENTE', NOW() - INTERVAL '3 hours'
    FROM resena r JOIN servicio_calificable s ON s.id = r.servicio_id WHERE s.solicitud_id = 104001;

    INSERT INTO reporte_resena (resena_id, reportante_id, motivo, descripcion, estado, fecha_reporte, moderador_id,
                                resultado, fecha_resolucion)
    SELECT r.id, 103, 'OTRO', 'No estoy de acuerdo con la calificación.', 'RESUELTO', NOW() - INTERVAL '3 days', 1,
           'RESENA_MANTENIDA', NOW() - INTERVAL '2 days'
    FROM resena r JOIN servicio_calificable s ON s.id = r.servicio_id WHERE s.solicitud_id = 103001;

    -- Servicios cumplidos sin calificar, para probar la pantalla de calificar con el contratante 201.
    INSERT INTO servicio_calificable (solicitud_id, trabajador_id, contratante_id, fecha_cumplida)
    VALUES (900001, 101, 201, NOW() - INTERVAL '2 hours'),
           (900002, 102, 201, NOW() - INTERVAL '1 day');
END $$;

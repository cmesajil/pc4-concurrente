-- 1. Tabla de Usuarios (Clientes)
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,                   -- ID autoincremental único
    nombre VARCHAR(100) NOT NULL,            -- Nombre del cliente
    qr_token VARCHAR(255) UNIQUE NOT NULL,   -- El texto/hash que va dentro del código QR
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de Salas de Chat (para conectar a los usuarios)
CREATE TABLE salas (
    id SERIAL PRIMARY KEY,
    qr_token VARCHAR(255) UNIQUE,
    nombre VARCHAR(100) DEFAULT 'Chat Privado', -- Útil si luego agregas grupos
    creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

-- Tabla intermedia para saber qué usuarios pertenecen a qué chats (Muchos a Muchos)
CREATE TABLE participantes_sala (
    sala_id INT REFERENCES salas(id) ON DELETE CASCADE,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    PRIMARY KEY (sala_id, usuario_id)
);

-- 3. Tabla de Mensajes (El Historial)
CREATE TABLE mensajes (
    id BIGSERIAL PRIMARY KEY,                -- BIGSERIAL porque los mensajes crecen rápido
    sala_id INT REFERENCES salas(id) ON DELETE CASCADE,
    remitente_id INT REFERENCES usuarios(id) ON DELETE SET NULL, -- Si el usuario se borra, el mensaje queda como 'Anónimo'
    contenido TEXT NOT NULL,                 -- El texto del mensaje
    enviado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Fecha y hora exacta con zona horaria
);

-- Índices para que las búsquedas del historial sean ultra rápidas
CREATE INDEX idx_mensajes_sala ON mensajes(sala_id);
CREATE INDEX idx_usuarios_qr ON usuarios(qr_token);

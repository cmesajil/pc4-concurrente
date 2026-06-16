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

-- 1. Tabla de Clientes
-- Minimalista, sin relación con el QR de la app
CREATE TABLE clientes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- 2. Tabla de Productos
-- Catálogo pequeño
CREATE TABLE productos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL
);

-- 3. Tabla de Pedidos
-- El motor para los scripts de automatización
CREATE TABLE pedidos (
    id SERIAL PRIMARY KEY,
    cliente_id INT REFERENCES clientes(id) ON DELETE CASCADE,
    producto_id INT REFERENCES productos(id) ON DELETE RESTRICT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'pendiente' NOT NULL
);

-- 4. Tabla de Comprobantes
-- Solo almacena el saldo pagado; el nombre del producto se obtiene mediante consultas
CREATE TABLE comprobantes (
    id SERIAL PRIMARY KEY,
    pedido_id INT UNIQUE REFERENCES pedidos(id) ON DELETE CASCADE,
    saldo_pagado NUMERIC(10, 2) NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 1. Crear la Sala de Ventas Oficial (Donde atiende tu ClienteVendedorChatTLS)
-- El qr_token debe coincidir exactamente con la constante QR_SALA_VENTAS de tu código Java
INSERT INTO salas (qr_token, nombre)
VALUES ('SALA_VENTAS_PRINCIPAL', 'Sala de Ventas Automatizada')
ON CONFLICT (qr_token) DO NOTHING;


-- 2. Registrar al Usuario Vendedor en la capa de la Aplicación de Chat
-- El qr_token debe coincidir exactamente con la constante QR_VENDEDOR_EXISTENTE
INSERT INTO usuarios (nombre, qr_token)
VALUES ('Vendedor Automático', 'QR_VENDEDOR_OFFICIAL_XYZ')
ON CONFLICT (qr_token) DO NOTHING;


-- 4. Cargar el Catálogo de Productos (Módulo de Ventas)
-- Agregamos una lista pequeña con IDs fijos para facilitar las pruebas de texto (PAGAR 1, PAGAR 2...)
INSERT INTO productos (id, nombre, precio) VALUES
(1, 'Café Espresso Americano', 3.50),
(2, 'Croissant de Jamón y Queso', 4.20),
(3, 'Muffin de Arándanos', 2.80),
(4, 'Té Verde Orgánico', 3.00)
ON CONFLICT (id) DO NOTHING;

-- Reajustar el secuenciador de IDs de productos para evitar conflictos si agregas más después
SELECT setval(pg_get_serial_sequence('productos', 'id'), COALESCE(MAX(id), 1)) FROM productos;

-- Índices para que las búsquedas del historial sean ultra rápidas
CREATE INDEX idx_mensajes_sala ON mensajes(sala_id);
CREATE INDEX idx_usuarios_qr ON usuarios(qr_token);

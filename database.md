## 1. El Modelo de Base de Datos (Esquema Conceptual)

Necesitaremos al menos tres tablas principales:

1. **`usuarios`**: Guarda la identidad del cliente (ID, nombre, token del QR).
    
2. **`chats` o `salas`**: Permite que el historial soporte chats individuales (1 a 1) o grupales en el futuro.
    
3. **`mensajes`**: El historial puro del chat, donde se registra qué usuario envió qué cosa, en qué sala y cuándo.
    

## 2. Script de Creación en PostgreSQL (SQL)

Puedes ejecutar este script directamente en tu cliente de Postgres (como pgAdmin o DBeaver) para crear las tablas:

SQL

```sql
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
```

## 3. ¿Cómo funciona la lógica en tu arquitectura?

### El flujo del Código QR:

1. Cuando un usuario nuevo se registra, tu servidor genera un string único (por ejemplo, un UUID como `f47ac10b-58cc-4372-a567-0e02b2c3d479`).
    
2. Guardas ese UUID en el campo `qr_token`.
    
3. Tu servidor (o la app cliente) usa una librería para transformar ese UUID en la imagen de un código QR.
    
4. Cuando el usuario quiere iniciar sesión o identificarse, la cámara escanea el QR, extrae el UUID y el servidor hace un:
    
    SQL
    
    ```sql
    SELECT id, nombre FROM usuarios WHERE qr_token = 'uuid_escaneado';
    ```
    

### Consultar el Historial:

Para traer los mensajes de una sala específica ordenados por el más reciente, tu servidor solo tendrá que ejecutar:

SQL

```sql
SELECT m.id, u.nombre AS remitente, m.contenido, m.enviado_en 
FROM mensajes m
LEFT JOIN usuarios u ON m.remitente_id = u.id
WHERE m.sala_id = 1
ORDER BY m.enviado_en ASC;
```

## Un consejo para el futuro (Escalabilidad)

Como estás usando **PostgreSQL**, si en el futuro tu chat se vuelve muy popular y manejas millones de mensajes, la tabla `mensajes` puede volverse lenta.

Para prevenir esto, incluí un **Índice** (`idx_mensajes_sala`). Los índices en Postgres funcionan como el índice de un libro: le permiten a la base de datos encontrar el historial de un chat directamente sin tener que leer millones de filas de otros usuarios.

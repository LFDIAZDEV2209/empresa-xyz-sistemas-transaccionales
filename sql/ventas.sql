CREATE DATABASE IF NOT EXISTS ventas CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ventas;
CREATE TABLE IF NOT EXISTS productos (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(120) NOT NULL,
 descripcion VARCHAR(500),
 precio DECIMAL(12,2) NOT NULL,
 cantidad_disponible INT NOT NULL DEFAULT 0,
 CHECK (precio >= 0), CHECK (cantidad_disponible >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ventas (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 fecha_transaccion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 producto_id BIGINT NOT NULL,
 cantidad_vendida INT NOT NULL,
 precio_venta DECIMAL(12,2) NOT NULL,
 total DECIMAL(14,2) GENERATED ALWAYS AS (cantidad_vendida * precio_venta) STORED,
 CONSTRAINT fk_ventas_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
 CHECK(cantidad_vendida > 0), CHECK(precio_venta >= 0),
 INDEX idx_ventas_fecha (fecha_transaccion), INDEX idx_ventas_producto_fecha (producto_id,fecha_transaccion)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS integracion_contabilidad (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 venta_id BIGINT NOT NULL UNIQUE,
 evento_tipo VARCHAR(40) NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 exportado_en DATETIME(6) NULL,
 CONSTRAINT fk_integracion_venta FOREIGN KEY (venta_id) REFERENCES ventas(id)
) ENGINE=InnoDB;
INSERT INTO productos(nombre,descripcion,precio,cantidad_disponible)
SELECT 'Mouse inalámbrico','Mouse para oficina',65000,25 WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre='Mouse inalámbrico');
INSERT INTO productos(nombre,descripcion,precio,cantidad_disponible)
SELECT 'Teclado mecánico','Teclado USB',180000,12 WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre='Teclado mecánico');

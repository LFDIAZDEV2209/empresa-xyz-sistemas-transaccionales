# Empresa XYZ — Sistema de ventas (prototipo educativo)

## Requisitos
Java 17+, Apache NetBeans con Maven, servidor MySQL 8+, Maven (o Maven incluido en NetBeans).

## Instalación
1. Ejecutar `sql/ventas.sql` desde MySQL Workbench o `mysql -u root -p < sql/ventas.sql`.
2. Configurar variables de entorno `XYZ_DB_URL`, `XYZ_DB_USER` y `XYZ_DB_PASSWORD`. Nunca incluir contraseñas en repositorios.
3. Abrir la carpeta `proyecto` como **Existing Maven Project** en NetBeans.
4. Ejecutar clase `co.edu.uniminuto.xyz.App` (o `mvn compile exec:java`).
5. Seleccionar producto, ingresar cantidad y Registrar venta. Si hay stock se registrará venta, descontará inventario y creará evento contable en una sola transacción.
6. Para demostrar `rollback`, pedir más unidades de las disponibles; el stock y ventas deben permanecer iguales.
7. El botón **Exportar contabilidad CSV** crea `contabilidad_pendiente.csv`, demostración del contrato de integración. La recepción/acuse por un ERP externo está pendiente.

## Alcance y advertencias
- Código fuente preparado para uso en NetBeans, sin declaración de prueba con un servidor MySQL real en este entorno.
- El CRUD de productos está implementado. Por trazabilidad contable, no se ofrece eliminación de ventas; se recomienda añadir anulaciones compensatorias.
- Las ventas se protegen con `SELECT ... FOR UPDATE`, `commit`, `rollback`, `InnoDB`, claves foráneas y cantidades no negativas.
- El caso solicita dos tablas (`productos`, `ventas`). Se incluye **una tercera tabla técnica** para integrar contabilidad mediante patrón outbox; no sustituye ninguna de las dos obligatorias.
- La exportación CSV no constituye integración bidireccional en producción; harían falta importación, validación y acuse de recibo con el sistema contable.

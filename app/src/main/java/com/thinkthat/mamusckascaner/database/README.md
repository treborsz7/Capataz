# Implementación de SQLite en QRCodeScanner

## Descripción General

Esta implementación de SQLite proporciona persistencia local de datos para la aplicación QRCodeScanner, permitiendo que las recolecciones se guarden automáticamente y puedan recuperarse incluso si la aplicación se cierra o pierde conectividad.

## Arquitectura

### Estructura de Archivos

```
database/
├── DatabaseHelper.kt         # SQLiteOpenHelper - Gestión de base de datos
├── RecoleccionEntity.kt      # Entidad para recolecciones
├── PedidoEntity.kt           # Entidad para pedidos
├── RecoleccionRepository.kt  # Capa de acceso a datos (Repository pattern)
└── README.md                 # Este archivo
```

### Componentes

#### 1. DatabaseHelper.kt
- Hereda de `SQLiteOpenHelper`
- Gestiona la creación y actualización de la base de datos
- Proporciona métodos CRUD para todas las tablas
- Versión actual: 1

**Tablas:**

##### Tabla `recolecciones`
```sql
CREATE TABLE recolecciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_pedido INTEGER NOT NULL,
    cod_articulo TEXT NOT NULL,
    nombre_articulo TEXT NOT NULL,
    cantidad_solicitada INTEGER NOT NULL,
    ubicacion TEXT NOT NULL,
    partida TEXT NOT NULL,
    cantidad INTEGER NOT NULL,
    cod_deposito TEXT NOT NULL,
    usuario TEXT NOT NULL,
    fecha_hora TEXT NOT NULL,
    sincronizado INTEGER DEFAULT 0,
    indice_scaneo INTEGER DEFAULT 0
)
```

##### Tabla `pedidos`
```sql
CREATE TABLE pedidos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_pedido INTEGER UNIQUE NOT NULL,
    cod_deposito TEXT NOT NULL,
    fecha_creacion TEXT NOT NULL,
    estado TEXT DEFAULT 'pendiente',
    ubicaciones_json TEXT
)
```

#### 2. Entidades

##### RecoleccionEntity
Representa una recolección individual con todos sus datos:
- `id`: ID local (autoincremental)
- `idPedido`: ID del pedido al que pertenece
- `codArticulo`: Código del artículo recolectado
- `nombreArticulo`: Descripción del artículo
- `cantidadSolicitada`: Cantidad total requerida
- `ubicacion`: Ubicación de donde se recolectó
- `partida`: Número de partida
- `cantidad`: Cantidad recolectada en este escaneo
- `codDeposito`: Código del depósito
- `usuario`: Usuario que realizó la recolección
- `fechaHora`: Timestamp de la recolección
- `sincronizado`: Flag para saber si ya fue enviado al servidor
- `indiceScaneo`: Índice del escaneo (para múltiples escaneos del mismo artículo)

##### PedidoEntity
Representa un pedido completo:
- `id`: ID local (autoincremental)
- `idPedido`: ID único del pedido
- `codDeposito`: Código del depósito
- `fechaCreacion`: Fecha de creación
- `estado`: Estado del pedido (pendiente, completado, sincronizado)
- `ubicacionesJson`: JSON con las ubicaciones del pedido

#### 3. RecoleccionRepository
Capa de abstracción sobre DatabaseHelper que:
- Usa corrutinas para operaciones asíncronas
- Maneja errores de forma segura
- Proporciona una API limpia para operaciones CRUD
- Todas las operaciones corren en `Dispatchers.IO`

## Integración con RecolectarScreen

### Flujo de Datos

1. **Inicio de la pantalla:**
   - Se crea una instancia de `RecoleccionRepository`
   - Se cargan automáticamente las recolecciones guardadas del pedido actual
   - Los datos se restauran en el estado de Compose

2. **Durante la recolección:**
   - Cuando el usuario confirma una cantidad (presiona el botón Save):
     - Se guarda en el estado local de Compose
     - Se guarda automáticamente en SQLite en segundo plano
     - Se asocia con el pedido actual

3. **Al enviar al servidor:**
   - Se construye el JSON con los datos actuales
   - Si el envío es exitoso:
     - Las recolecciones se marcan como `sincronizado = true`
     - El pedido se actualiza a estado `sincronizado`
   - Si falla:
     - Los datos permanecen en SQLite sin sincronizar
     - Pueden reenviarse más tarde

### Puntos de Integración

#### Inicialización del Repository
```kotlin
val repository = remember { RecoleccionRepository(context) }
```

#### Carga de Datos Guardados
```kotlin
LaunchedEffect(qrData?.pedido) {
    val recoleccionesGuardadas = repository.getRecoleccionesByPedido(idPedido)
    // Reconstruir estado desde la base de datos
}
```

#### Guardado Automático
```kotlin
IconButton(onClick = {
    // Guardar en estado local
    cantidadesGuardadas = ...
    
    // Guardar en SQLite
    CoroutineScope(Dispatchers.IO).launch {
        val recoleccion = RecoleccionEntity(...)
        repository.saveRecoleccion(recoleccion)
    }
})
```

#### Sincronización
```kotlin
if (response.isSuccessful) {
    val recoleccionesGuardadas = repository.getRecoleccionesByPedido(idPedido)
    recoleccionesGuardadas.forEach { recoleccion ->
        repository.marcarComoSincronizado(recoleccion.id)
    }
    repository.updatePedidoEstado(idPedido, "sincronizado")
}
```

## Casos de Uso

### 1. Persistencia Offline
- El usuario puede escanear y guardar recolecciones sin conexión
- Los datos se guardan localmente en SQLite
- Cuando recupera la conexión, puede enviar todo junto

### 2. Recuperación de Sesión
- Si la app se cierra inesperadamente
- Al volver a abrir con el mismo pedido
- Los datos se recuperan automáticamente

### 3. Historial de Recolecciones
- Todas las recolecciones quedan registradas
- Pueden consultarse recolecciones sincronizadas
- Útil para auditoría y reportes

### 4. Reintento de Envío
- Si el envío al servidor falla
- Los datos permanecen en SQLite
- Se pueden recuperar y reenviar más tarde

## Operaciones Disponibles

### Recolecciones
```kotlin
// Guardar una recolección
repository.saveRecoleccion(recoleccion)

// Obtener recolecciones de un pedido
repository.getRecoleccionesByPedido(idPedido)

// Obtener recolecciones no sincronizadas
repository.getRecoleccionesNoSincronizadas()

// Marcar como sincronizado
repository.marcarComoSincronizado(id)

// Eliminar recolección
repository.deleteRecoleccion(id)

// Eliminar todas las recolecciones de un pedido
repository.deleteRecoleccionesByPedido(idPedido)
```

### Pedidos
```kotlin
// Guardar o actualizar pedido
repository.savePedido(pedido)

// Obtener pedido por ID
repository.getPedidoByIdPedido(idPedido)

// Obtener todos los pedidos
repository.getAllPedidos()

// Actualizar estado
repository.updatePedidoEstado(idPedido, "sincronizado")

// Eliminar pedido y sus recolecciones
repository.deletePedidoConRecolecciones(idPedido)
```

## Ventajas de esta Implementación

1. ✅ **Persistencia Local:** Los datos se guardan automáticamente
2. ✅ **Recuperación de Sesión:** Continuar donde se dejó
3. ✅ **Modo Offline:** Funciona sin conexión a internet
4. ✅ **Sincronización:** Control de qué datos ya se enviaron
5. ✅ **Múltiples Escaneos:** Soporte completo para múltiples ubicaciones por artículo
6. ✅ **Seguridad de Datos:** Los datos no se pierden si la app se cierra
7. ✅ **Auditoría:** Historial completo de recolecciones

## Consideraciones de Rendimiento

- Todas las operaciones de I/O se ejecutan en `Dispatchers.IO`
- Las operaciones son asíncronas usando corrutinas
- No bloquean el hilo principal (UI)
- Logs detallados para debugging

## Mantenimiento

### Actualizar la Base de Datos
Si necesitas agregar columnas o tablas:

1. Incrementa `DATABASE_VERSION` en `DatabaseHelper`
2. Implementa la lógica en `onUpgrade()`
3. Considera migración de datos existentes

### Limpiar Datos
```kotlin
// Limpiar todas las tablas (útil para testing)
repository.clearAllData()
```

### Logs
Todos los componentes registran logs con el tag correspondiente:
- `DatabaseHelper`: Operaciones de base de datos
- `RecoleccionRepository`: Operaciones del repository
- `RecolectarScreen`: Integración con la UI

## Próximas Mejoras Sugeridas

1. **Pantalla de Historial:** Ver todas las recolecciones guardadas
2. **Sincronización Masiva:** Botón para enviar todas las recolecciones pendientes
3. **Exportar Datos:** Exportar recolecciones a CSV/JSON
4. **Limpieza Automática:** Eliminar datos sincronizados antiguos
5. **Backup:** Funcionalidad de backup/restore de la base de datos

## Troubleshooting

### La base de datos no se crea
- Verifica permisos de almacenamiento
- Revisa los logs con filtro "DatabaseHelper"

### Los datos no se recuperan
- Verifica que el `idPedido` sea el mismo
- Revisa que las recolecciones existan en la base de datos

### Errores de sincronización
- Verifica conectividad de red
- Revisa logs de respuesta del servidor
- Los datos permanecen en SQLite si falla

## Autor
Implementado para QRCodeScanner App - 2025

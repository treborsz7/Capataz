package com.thinkthat.mamusckascaner.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Helper para la base de datos SQLite
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "QRCodeScanner.db"
        private const val DATABASE_VERSION = 2

        // Tabla de Recolecciones
        const val TABLE_RECOLECCIONES = "recolecciones"
        const val COLUMN_ID = "id"
        const val COLUMN_ID_PEDIDO = "id_pedido"
        const val COLUMN_COD_ARTICULO = "cod_articulo"
        const val COLUMN_NOMBRE_ARTICULO = "nombre_articulo"
        const val COLUMN_CANTIDAD_SOLICITADA = "cantidad_solicitada"
        const val COLUMN_UBICACION = "ubicacion"
        const val COLUMN_PARTIDA = "partida"
        const val COLUMN_CANTIDAD = "cantidad"
        const val COLUMN_COD_DEPOSITO = "cod_deposito"
        const val COLUMN_USUARIO = "usuario"
        const val COLUMN_FECHA_HORA = "fecha_hora"
        const val COLUMN_SINCRONIZADO = "sincronizado"
        const val COLUMN_INDICE_SCANEO = "indice_scaneo"

        // Tabla de Pedidos
        const val TABLE_PEDIDOS = "pedidos"
        const val COLUMN_PEDIDO_ID = "id"
        const val COLUMN_PEDIDO_ID_PEDIDO = "id_pedido"
        const val COLUMN_PEDIDO_COD_DEPOSITO = "cod_deposito"
        const val COLUMN_PEDIDO_FECHA_CREACION = "fecha_creacion"
        const val COLUMN_PEDIDO_ESTADO = "estado"
        const val COLUMN_PEDIDO_UBICACIONES_JSON = "ubicaciones_json"

        // Tabla de Estivaciones
        const val TABLE_ESTIVACIONES = "estivaciones"
        const val COLUMN_ESTIVACION_ID = "id"
        const val COLUMN_ESTIVACION_PARTIDA = "partida"
        const val COLUMN_ESTIVACION_UBICACION = "ubicacion"
        const val COLUMN_ESTIVACION_COD_DEPOSITO = "cod_deposito"
        const val COLUMN_ESTIVACION_FECHA_CREACION = "fecha_creacion"
        const val COLUMN_ESTIVACION_ESTADO = "estado"

        // Tabla de Reubicaciones
        const val TABLE_REUBICACIONES = "reubicaciones"
        const val COLUMN_REUBICACION_ID = "id"
        const val COLUMN_REUBICACION_PARTIDA = "partida"
        const val COLUMN_REUBICACION_UBICACION_ORIGEN = "ubicacion_origen"
        const val COLUMN_REUBICACION_UBICACION_DESTINO = "ubicacion_destino"
        const val COLUMN_REUBICACION_COD_DEPOSITO = "cod_deposito"
        const val COLUMN_REUBICACION_FECHA_CREACION = "fecha_creacion"
        const val COLUMN_REUBICACION_ESTADO = "estado"

        private const val CREATE_TABLE_RECOLECCIONES = """
            CREATE TABLE $TABLE_RECOLECCIONES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_PEDIDO INTEGER NOT NULL,
                $COLUMN_COD_ARTICULO TEXT NOT NULL,
                $COLUMN_NOMBRE_ARTICULO TEXT NOT NULL,
                $COLUMN_CANTIDAD_SOLICITADA INTEGER NOT NULL,
                $COLUMN_UBICACION TEXT NOT NULL,
                $COLUMN_PARTIDA TEXT NOT NULL,
                $COLUMN_CANTIDAD INTEGER NOT NULL,
                $COLUMN_COD_DEPOSITO TEXT NOT NULL,
                $COLUMN_USUARIO TEXT NOT NULL,
                $COLUMN_FECHA_HORA TEXT NOT NULL,
                $COLUMN_SINCRONIZADO INTEGER DEFAULT 0,
                $COLUMN_INDICE_SCANEO INTEGER DEFAULT 0
            )
        """

        private const val CREATE_TABLE_PEDIDOS = """
            CREATE TABLE $TABLE_PEDIDOS (
                $COLUMN_PEDIDO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PEDIDO_ID_PEDIDO INTEGER UNIQUE NOT NULL,
                $COLUMN_PEDIDO_COD_DEPOSITO TEXT NOT NULL,
                $COLUMN_PEDIDO_FECHA_CREACION TEXT NOT NULL,
                $COLUMN_PEDIDO_ESTADO TEXT DEFAULT 'pendiente',
                $COLUMN_PEDIDO_UBICACIONES_JSON TEXT
            )
        """

        private const val CREATE_TABLE_ESTIVACIONES = """
            CREATE TABLE $TABLE_ESTIVACIONES (
                $COLUMN_ESTIVACION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ESTIVACION_PARTIDA TEXT NOT NULL,
                $COLUMN_ESTIVACION_UBICACION TEXT NOT NULL,
                $COLUMN_ESTIVACION_COD_DEPOSITO TEXT NOT NULL,
                $COLUMN_ESTIVACION_FECHA_CREACION TEXT NOT NULL,
                $COLUMN_ESTIVACION_ESTADO TEXT DEFAULT 'pendiente'
            )
        """

        private const val CREATE_TABLE_REUBICACIONES = """
            CREATE TABLE $TABLE_REUBICACIONES (
                $COLUMN_REUBICACION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_REUBICACION_PARTIDA TEXT NOT NULL,
                $COLUMN_REUBICACION_UBICACION_ORIGEN TEXT NOT NULL,
                $COLUMN_REUBICACION_UBICACION_DESTINO TEXT NOT NULL,
                $COLUMN_REUBICACION_COD_DEPOSITO TEXT NOT NULL,
                $COLUMN_REUBICACION_FECHA_CREACION TEXT NOT NULL,
                $COLUMN_REUBICACION_ESTADO TEXT DEFAULT 'pendiente'
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_RECOLECCIONES)
        db?.execSQL(CREATE_TABLE_PEDIDOS)
        db?.execSQL(CREATE_TABLE_ESTIVACIONES)
        db?.execSQL(CREATE_TABLE_REUBICACIONES)
        Log.d("DatabaseHelper", "Base de datos creada exitosamente")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Agregar tablas nuevas en la versión 2
            db?.execSQL(CREATE_TABLE_ESTIVACIONES)
            db?.execSQL(CREATE_TABLE_REUBICACIONES)
            Log.d("DatabaseHelper", "Tablas de Estivación y Reubicación agregadas")
        }
        Log.d("DatabaseHelper", "Base de datos actualizada de versión $oldVersion a $newVersion")
        ;
    }

    // CRUD para Recolecciones

    /**
     * Inserta una nueva recolección
     */
    fun insertRecoleccion(recoleccion: RecoleccionEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_PEDIDO, recoleccion.idPedido)
            put(COLUMN_COD_ARTICULO, recoleccion.codArticulo)
            put(COLUMN_NOMBRE_ARTICULO, recoleccion.nombreArticulo)
            put(COLUMN_CANTIDAD_SOLICITADA, recoleccion.cantidadSolicitada)
            put(COLUMN_UBICACION, recoleccion.ubicacion)
            put(COLUMN_PARTIDA, recoleccion.partida)
            put(COLUMN_CANTIDAD, recoleccion.cantidad)
            put(COLUMN_COD_DEPOSITO, recoleccion.codDeposito)
            put(COLUMN_USUARIO, recoleccion.usuario)
            put(COLUMN_FECHA_HORA, recoleccion.fechaHora)
            put(COLUMN_SINCRONIZADO, if (recoleccion.sincronizado) 1 else 0)
            put(COLUMN_INDICE_SCANEO, recoleccion.indiceScaneo)
        }
        
        val id = db.insert(TABLE_RECOLECCIONES, null, values)
        Log.d("DatabaseHelper", "Recolección insertada con ID: $id")
        return id
    }

    /**
     * Actualiza una recolección existente
     */
    fun updateRecoleccion(recoleccion: RecoleccionEntity): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_PEDIDO, recoleccion.idPedido)
            put(COLUMN_COD_ARTICULO, recoleccion.codArticulo)
            put(COLUMN_UBICACION, recoleccion.ubicacion)
            put(COLUMN_PARTIDA, recoleccion.partida)
            put(COLUMN_CANTIDAD, recoleccion.cantidad)
            put(COLUMN_COD_DEPOSITO, recoleccion.codDeposito)
            put(COLUMN_USUARIO, recoleccion.usuario)
            put(COLUMN_FECHA_HORA, recoleccion.fechaHora)
            put(COLUMN_SINCRONIZADO, if (recoleccion.sincronizado) 1 else 0)
            put(COLUMN_INDICE_SCANEO, recoleccion.indiceScaneo)
        }
        
        val rowsUpdated = db.update(
            TABLE_RECOLECCIONES,
            values,
            "$COLUMN_ID = ?",
            arrayOf(recoleccion.id.toString())
        )
        
        Log.d("DatabaseHelper", "Recolección ${recoleccion.id} actualizada")
        return rowsUpdated
    }

    /**
     * Obtiene todas las recolecciones de un pedido
     */
    fun getRecoleccionesByPedido(idPedido: Int): List<RecoleccionEntity> {
        val recolecciones = mutableListOf<RecoleccionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECOLECCIONES,
            null,
            "$COLUMN_ID_PEDIDO = ?",
            arrayOf(idPedido.toString()),
            null,
            null,
            "$COLUMN_INDICE_SCANEO ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    recolecciones.add(cursorToRecoleccion(it))
                } while (it.moveToNext())
            }
        }
        
        Log.d("DatabaseHelper", "Recuperadas ${recolecciones.size} recolecciones para pedido $idPedido")
        return recolecciones
    }

    /**
     * Busca una recolección específica por pedido, artículo, ubicación e índice
     */
    fun getRecoleccionByDetails(
        idPedido: Int,
        codArticulo: String,
        ubicacion: String,
        indiceScaneo: Int
    ): RecoleccionEntity? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECOLECCIONES,
            null,
            "$COLUMN_ID_PEDIDO = ? AND $COLUMN_COD_ARTICULO = ? AND $COLUMN_UBICACION = ? AND $COLUMN_INDICE_SCANEO = ?",
            arrayOf(idPedido.toString(), codArticulo, ubicacion, indiceScaneo.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToRecoleccion(it)
            }
        }
        
        return null
    }

    /**
     * Busca una recolección por pedido, artículo e índice (ignora ubicación/partida)
     * Útil para encontrar registros cuando se modifican ubicación o partida
     */
    fun getRecoleccionByIndex(
        idPedido: Int,
        codArticulo: String,
        indiceScaneo: Int
    ): RecoleccionEntity? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECOLECCIONES,
            null,
            "$COLUMN_ID_PEDIDO = ? AND $COLUMN_COD_ARTICULO = ? AND $COLUMN_INDICE_SCANEO = ?",
            arrayOf(idPedido.toString(), codArticulo, indiceScaneo.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToRecoleccion(it)
            }
        }
        
        return null
    }

    /**
     * Inserta o actualiza una recolección (insert or update)
     * Busca primero por índice (permite actualizar si cambió ubicación/partida)
     */
    fun insertOrUpdateRecoleccion(recoleccion: RecoleccionEntity): Long {
        // Buscar primero por índice (sin ubicación) para detectar cambios en ubicación/partida
        val existentePorIndice = getRecoleccionByIndex(
            recoleccion.idPedido,
            recoleccion.codArticulo,
            recoleccion.indiceScaneo
        )
        
        return if (existentePorIndice != null) {
            // Ya existe un registro para este índice, actualizar
            val recoleccionActualizada = recoleccion.copy(id = existentePorIndice.id)
            updateRecoleccion(recoleccionActualizada)
            Log.d("DatabaseHelper", "Recolección actualizada (cambio de ubicación/partida) con ID: ${existentePorIndice.id}")
            existentePorIndice.id
        } else {
            // No existe, insertar nueva
            val id = insertRecoleccion(recoleccion)
            Log.d("DatabaseHelper", "Nueva recolección insertada con ID: $id")
            id
        }
    }

    /**
     * Obtiene recolecciones no sincronizadas
     */
    fun getRecoleccionesNoSincronizadas(): List<RecoleccionEntity> {
        val recolecciones = mutableListOf<RecoleccionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECOLECCIONES,
            null,
            "$COLUMN_SINCRONIZADO = ?",
            arrayOf("0"),
            null,
            null,
            "$COLUMN_FECHA_HORA DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    recolecciones.add(cursorToRecoleccion(it))
                } while (it.moveToNext())
            }
        }
        
        Log.d("DatabaseHelper", "Recuperadas ${recolecciones.size} recolecciones no sincronizadas")
        return recolecciones
    }

    /**
     * Marca una recolección como sincronizada
     */
    fun marcarComoSincronizado(id: Long): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SINCRONIZADO, 1)
        }
        
        val rowsAffected = db.update(
            TABLE_RECOLECCIONES,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
        
        Log.d("DatabaseHelper", "Recolección $id marcada como sincronizada")
        return rowsAffected
    }

    /**
     * Elimina una recolección
     */
    fun deleteRecoleccion(id: Long): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_RECOLECCIONES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
        
        Log.d("DatabaseHelper", "Recolección $id eliminada")
        return rowsDeleted
    }

    /**
     * Elimina todas las recolecciones de un pedido
     */
    fun deleteRecoleccionesByPedido(idPedido: Int): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_RECOLECCIONES,
            "$COLUMN_ID_PEDIDO = ?",
            arrayOf(idPedido.toString())
        )
        
        Log.d("DatabaseHelper", "$rowsDeleted recolecciones eliminadas del pedido $idPedido")
        return rowsDeleted
    }

    // CRUD para Pedidos

    /**
     * Inserta o actualiza un pedido
     */
    fun insertOrUpdatePedido(pedido: PedidoEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PEDIDO_ID_PEDIDO, pedido.idPedido)
            put(COLUMN_PEDIDO_COD_DEPOSITO, pedido.codDeposito)
            put(COLUMN_PEDIDO_FECHA_CREACION, pedido.fechaCreacion)
            put(COLUMN_PEDIDO_ESTADO, pedido.estado)
            put(COLUMN_PEDIDO_UBICACIONES_JSON, pedido.ubicacionesJson)
        }
        
        // Intentar actualizar primero
        val rowsUpdated = db.update(
            TABLE_PEDIDOS,
            values,
            "$COLUMN_PEDIDO_ID_PEDIDO = ?",
            arrayOf(pedido.idPedido.toString())
        )
        
        return if (rowsUpdated > 0) {
            Log.d("DatabaseHelper", "Pedido ${pedido.idPedido} actualizado")
            getPedidoByIdPedido(pedido.idPedido)?.id ?: -1
        } else {
            val id = db.insert(TABLE_PEDIDOS, null, values)
            Log.d("DatabaseHelper", "Pedido ${pedido.idPedido} insertado con ID: $id")
            id
        }
    }

    /**
     * Obtiene un pedido por su idPedido
     */
    fun getPedidoByIdPedido(idPedido: Int): PedidoEntity? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEDIDOS,
            null,
            "$COLUMN_PEDIDO_ID_PEDIDO = ?",
            arrayOf(idPedido.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToPedido(it)
            }
        }
        
        return null
    }

    /**
     * Obtiene todos los pedidos
     */
    fun getAllPedidos(): List<PedidoEntity> {
        val pedidos = mutableListOf<PedidoEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEDIDOS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_PEDIDO_FECHA_CREACION DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    pedidos.add(cursorToPedido(it))
                } while (it.moveToNext())
            }
        }
        
        Log.d("DatabaseHelper", "Recuperados ${pedidos.size} pedidos")
        return pedidos
    }

    /**
     * Actualiza el estado de un pedido
     */
    fun updatePedidoEstado(idPedido: Int, nuevoEstado: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PEDIDO_ESTADO, nuevoEstado)
        }
        
        val rowsAffected = db.update(
            TABLE_PEDIDOS,
            values,
            "$COLUMN_PEDIDO_ID_PEDIDO = ?",
            arrayOf(idPedido.toString())
        )
        
        Log.d("DatabaseHelper", "Estado del pedido $idPedido actualizado a $nuevoEstado")
        return rowsAffected
    }

    /**
     * Elimina un pedido
     */
    fun deletePedido(idPedido: Int): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_PEDIDOS,
            "$COLUMN_PEDIDO_ID_PEDIDO = ?",
            arrayOf(idPedido.toString())
        )
        
        Log.d("DatabaseHelper", "Pedido $idPedido eliminado")
        return rowsDeleted
    }

    // Métodos auxiliares

    private fun cursorToRecoleccion(cursor: Cursor): RecoleccionEntity {
        return RecoleccionEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            idPedido = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID_PEDIDO)),
            codArticulo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COD_ARTICULO)),
            nombreArticulo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_ARTICULO)),
            cantidadSolicitada = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CANTIDAD_SOLICITADA)),
            ubicacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UBICACION)),
            partida = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTIDA)),
            cantidad = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CANTIDAD)),
            codDeposito = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COD_DEPOSITO)),
            usuario = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USUARIO)),
            fechaHora = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_HORA)),
            sincronizado = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SINCRONIZADO)) == 1,
            indiceScaneo = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INDICE_SCANEO))
        )
    }

    private fun cursorToPedido(cursor: Cursor): PedidoEntity {
        return PedidoEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_ID)),
            idPedido = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_ID_PEDIDO)),
            codDeposito = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_COD_DEPOSITO)),
            fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_FECHA_CREACION)),
            estado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_ESTADO)),
            ubicacionesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEDIDO_UBICACIONES_JSON))
        )
    }

    /**
     * Limpia todas las tablas (útil para testing)
     */
    fun clearAllTables() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_RECOLECCIONES")
        db.execSQL("DELETE FROM $TABLE_PEDIDOS")
        db.execSQL("DELETE FROM $TABLE_ESTIVACIONES")
        db.execSQL("DELETE FROM $TABLE_REUBICACIONES")
        Log.d("DatabaseHelper", "Todas las tablas limpiadas")
    }

    // ===== CRUD ESTIVACIONES =====

    fun insertEstivacion(estivacion: EstivacionEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ESTIVACION_PARTIDA, estivacion.partida)
            put(COLUMN_ESTIVACION_UBICACION, estivacion.ubicacion)
            put(COLUMN_ESTIVACION_COD_DEPOSITO, estivacion.codDeposito)
            put(COLUMN_ESTIVACION_FECHA_CREACION, estivacion.fechaCreacion)
            put(COLUMN_ESTIVACION_ESTADO, estivacion.estado)
        }
        val id = db.insert(TABLE_ESTIVACIONES, null, values)
        Log.d("DatabaseHelper", "Estivación insertada con ID: $id")
        return id
    }

    fun getAllEstivaciones(): List<EstivacionEntity> {
        val db = readableDatabase
        val estivaciones = mutableListOf<EstivacionEntity>()
        val cursor = db.query(
            TABLE_ESTIVACIONES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ESTIVACION_FECHA_CREACION DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                estivaciones.add(cursorToEstivacion(it))
            }
        }
        Log.d("DatabaseHelper", "Recuperadas ${estivaciones.size} estivaciones")
        return estivaciones
    }

    fun getEstivacionesPendientes(): List<EstivacionEntity> {
        val db = readableDatabase
        val estivaciones = mutableListOf<EstivacionEntity>()
        val cursor = db.query(
            TABLE_ESTIVACIONES,
            null,
            "$COLUMN_ESTIVACION_ESTADO = ?",
            arrayOf("pendiente"),
            null,
            null,
            "$COLUMN_ESTIVACION_FECHA_CREACION DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                estivaciones.add(cursorToEstivacion(it))
            }
        }
        return estivaciones
    }

    fun deleteEstivacion(id: Long): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_ESTIVACIONES,
            "$COLUMN_ESTIVACION_ID = ?",
            arrayOf(id.toString())
        )
        Log.d("DatabaseHelper", "Estivación $id eliminada")
        return rowsDeleted
    }

    fun updateEstivacionEstado(id: Long, nuevoEstado: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ESTIVACION_ESTADO, nuevoEstado)
        }
        return db.update(
            TABLE_ESTIVACIONES,
            values,
            "$COLUMN_ESTIVACION_ID = ?",
            arrayOf(id.toString())
        )
    }
    
    fun updateEstivacion(estivacion: EstivacionEntity): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ESTIVACION_PARTIDA, estivacion.partida)
            put(COLUMN_ESTIVACION_UBICACION, estivacion.ubicacion)
            put(COLUMN_ESTIVACION_COD_DEPOSITO, estivacion.codDeposito)
            put(COLUMN_ESTIVACION_ESTADO, estivacion.estado)
        }
        val rowsUpdated = db.update(
            TABLE_ESTIVACIONES,
            values,
            "$COLUMN_ESTIVACION_ID = ?",
            arrayOf(estivacion.id.toString())
        )
        Log.d("DatabaseHelper", "Estivación ${estivacion.id} actualizada")
        return rowsUpdated
    }

    private fun cursorToEstivacion(cursor: Cursor): EstivacionEntity {
        return EstivacionEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_ID)),
            partida = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_PARTIDA)),
            ubicacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_UBICACION)),
            codDeposito = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_COD_DEPOSITO)),
            fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_FECHA_CREACION)),
            estado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTIVACION_ESTADO))
        )
    }

    // ===== CRUD REUBICACIONES =====

    fun insertReubicacion(reubicacion: ReubicacionEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_REUBICACION_PARTIDA, reubicacion.partida)
            put(COLUMN_REUBICACION_UBICACION_ORIGEN, reubicacion.ubicacionOrigen)
            put(COLUMN_REUBICACION_UBICACION_DESTINO, reubicacion.ubicacionDestino)
            put(COLUMN_REUBICACION_COD_DEPOSITO, reubicacion.codDeposito)
            put(COLUMN_REUBICACION_FECHA_CREACION, reubicacion.fechaCreacion)
            put(COLUMN_REUBICACION_ESTADO, reubicacion.estado)
        }
        val id = db.insert(TABLE_REUBICACIONES, null, values)
        Log.d("DatabaseHelper", "Reubicación insertada con ID: $id")
        return id
    }

    fun getAllReubicaciones(): List<ReubicacionEntity> {
        val db = readableDatabase
        val reubicaciones = mutableListOf<ReubicacionEntity>()
        val cursor = db.query(
            TABLE_REUBICACIONES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_REUBICACION_FECHA_CREACION DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                reubicaciones.add(cursorToReubicacion(it))
            }
        }
        Log.d("DatabaseHelper", "Recuperadas ${reubicaciones.size} reubicaciones")
        return reubicaciones
    }

    fun getReubicacionesPendientes(): List<ReubicacionEntity> {
        val db = readableDatabase
        val reubicaciones = mutableListOf<ReubicacionEntity>()
        val cursor = db.query(
            TABLE_REUBICACIONES,
            null,
            "$COLUMN_REUBICACION_ESTADO = ?",
            arrayOf("pendiente"),
            null,
            null,
            "$COLUMN_REUBICACION_FECHA_CREACION DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                reubicaciones.add(cursorToReubicacion(it))
            }
        }
        return reubicaciones
    }

    fun deleteReubicacion(id: Long): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(
            TABLE_REUBICACIONES,
            "$COLUMN_REUBICACION_ID = ?",
            arrayOf(id.toString())
        )
        Log.d("DatabaseHelper", "Reubicación $id eliminada")
        return rowsDeleted
    }

    fun updateReubicacionEstado(id: Long, nuevoEstado: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_REUBICACION_ESTADO, nuevoEstado)
        }
        return db.update(
            TABLE_REUBICACIONES,
            values,
            "$COLUMN_REUBICACION_ID = ?",
            arrayOf(id.toString())
        )
    }
    
    fun updateReubicacion(reubicacion: ReubicacionEntity): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_REUBICACION_PARTIDA, reubicacion.partida)
            put(COLUMN_REUBICACION_UBICACION_ORIGEN, reubicacion.ubicacionOrigen)
            put(COLUMN_REUBICACION_UBICACION_DESTINO, reubicacion.ubicacionDestino)
            put(COLUMN_REUBICACION_COD_DEPOSITO, reubicacion.codDeposito)
            put(COLUMN_REUBICACION_ESTADO, reubicacion.estado)
        }
        val rowsUpdated = db.update(
            TABLE_REUBICACIONES,
            values,
            "$COLUMN_REUBICACION_ID = ?",
            arrayOf(reubicacion.id.toString())
        )
        Log.d("DatabaseHelper", "Reubicación ${reubicacion.id} actualizada")
        return rowsUpdated
    }

    private fun cursorToReubicacion(cursor: Cursor): ReubicacionEntity {
        return ReubicacionEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_ID)),
            partida = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_PARTIDA)),
            ubicacionOrigen = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_UBICACION_ORIGEN)),
            ubicacionDestino = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_UBICACION_DESTINO)),
            codDeposito = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_COD_DEPOSITO)),
            fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_FECHA_CREACION)),
            estado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REUBICACION_ESTADO))
        )
    }
}

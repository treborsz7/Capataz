package com.thinkthat.mamusckascaner.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository para gestionar las operaciones de base de datos
 * Proporciona una capa de abstracción sobre DatabaseHelper
 */
class RecoleccionRepository(context: Context) {
    
    private val dbHelper = DatabaseHelper(context)

    // ===== Operaciones de Recolecciones =====

    /**
     * Guarda una recolección en la base de datos (inserta nueva)
     */
    suspend fun saveRecoleccion(recoleccion: RecoleccionEntity): Long = withContext(Dispatchers.IO) {
        try {
            dbHelper.insertRecoleccion(recoleccion)
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al guardar recolección", e)
            -1L
        }
    }

    /**
     * Inserta o actualiza una recolección (evita duplicados)
     */
    suspend fun saveOrUpdateRecoleccion(recoleccion: RecoleccionEntity): Long = withContext(Dispatchers.IO) {
        try {
            dbHelper.insertOrUpdateRecoleccion(recoleccion)
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al guardar/actualizar recolección", e)
            -1L
        }
    }

    /**
     * Guarda múltiples recolecciones
     */
    suspend fun saveRecolecciones(recolecciones: List<RecoleccionEntity>): List<Long> = withContext(Dispatchers.IO) {
        recolecciones.map { recoleccion ->
            try {
                dbHelper.insertRecoleccion(recoleccion)
            } catch (e: Exception) {
                Log.e("RecoleccionRepository", "Error al guardar recolección", e)
                -1L
            }
        }
    }

    /**
     * Obtiene todas las recolecciones de un pedido
     */
    suspend fun getRecoleccionesByPedido(idPedido: Int): List<RecoleccionEntity> = withContext(Dispatchers.IO) {
        try {
            dbHelper.getRecoleccionesByPedido(idPedido)
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al obtener recolecciones del pedido $idPedido", e)
            emptyList()
        }
    }

    /**
     * Obtiene recolecciones no sincronizadas
     */
    suspend fun getRecoleccionesNoSincronizadas(): List<RecoleccionEntity> = withContext(Dispatchers.IO) {
        try {
            dbHelper.getRecoleccionesNoSincronizadas()
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al obtener recolecciones no sincronizadas", e)
            emptyList()
        }
    }

    /**
     * Marca una recolección como sincronizada
     */
    suspend fun marcarComoSincronizado(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.marcarComoSincronizado(id) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al marcar recolección como sincronizada", e)
            false
        }
    }

    /**
     * Elimina una recolección
     */
    suspend fun deleteRecoleccion(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.deleteRecoleccion(id) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al eliminar recolección", e)
            false
        }
    }

    /**
     * Actualiza una recolección existente
     */
    suspend fun updateRecoleccion(recoleccion: RecoleccionEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.updateRecoleccion(recoleccion) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al actualizar recolección", e)
            false
        }
    }

    /**
     * Elimina todas las recolecciones de un pedido
     */
    suspend fun deleteRecoleccionesByPedido(idPedido: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.deleteRecoleccionesByPedido(idPedido) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al eliminar recolecciones del pedido $idPedido", e)
            false
        }
    }

    // ===== Operaciones de Pedidos =====

    /**
     * Guarda o actualiza un pedido
     */
    suspend fun savePedido(pedido: PedidoEntity): Long = withContext(Dispatchers.IO) {
        try {
            dbHelper.insertOrUpdatePedido(pedido)
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al guardar pedido", e)
            -1L
        }
    }

    /**
     * Obtiene un pedido por su ID
     */
    suspend fun getPedidoByIdPedido(idPedido: Int): PedidoEntity? = withContext(Dispatchers.IO) {
        try {
            dbHelper.getPedidoByIdPedido(idPedido)
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al obtener pedido $idPedido", e)
            null
        }
    }

    /**
     * Obtiene todos los pedidos
     */
    suspend fun getAllPedidos(): List<PedidoEntity> = withContext(Dispatchers.IO) {
        try {
            dbHelper.getAllPedidos()
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al obtener todos los pedidos", e)
            emptyList()
        }
    }

    /**
     * Actualiza el estado de un pedido
     */
    suspend fun updatePedidoEstado(idPedido: Int, nuevoEstado: String): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.updatePedidoEstado(idPedido, nuevoEstado) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al actualizar estado del pedido $idPedido", e)
            false
        }
    }

    /**
     * Elimina un pedido y sus recolecciones
     */
    suspend fun deletePedidoConRecolecciones(idPedido: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.deleteRecoleccionesByPedido(idPedido)
            dbHelper.deletePedido(idPedido) > 0
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al eliminar pedido $idPedido", e)
            false
        }
    }

    /**
     * Limpia todas las tablas
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            dbHelper.clearAllTables()
        } catch (e: Exception) {
            Log.e("RecoleccionRepository", "Error al limpiar datos", e)
        }
    }

    /**
     * Cierra la base de datos
     */
    fun close() {
        dbHelper.close()
    }
}

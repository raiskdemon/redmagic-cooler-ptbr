package com.hitomatito.redmagicooler.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.util.Log

/**
 * Utilidades para manejo seguro de conexiones Bluetooth
 */
object BleConnectionHelper {
    private const val TAG = "BleConnectionHelper"
    
    /**
     * Cierra una conexión GATT de forma segura
     * @param gatt Conexión GATT a cerrar
     * @param tag Tag para logging
     * @return true si se cerró correctamente, false si hubo errores
     */
    @SuppressLint("MissingPermission")
    fun safeCloseGatt(gatt: BluetoothGatt?, tag: String = TAG): Boolean {
        return try {
            gatt?.let {
                it.disconnect()
                it.close()
                Log.d(tag, "GATT cerrado correctamente")
                true
            } ?: false
        } catch (e: Exception) {
            Log.w(tag, "Error al cerrar GATT: ${e.message}")
            false
        }
    }
    
    /**
     * Detiene un escaneo BLE de forma segura
     * @param scanner Scanner a detener
     * @param callback Callback del escaneo
     * @param tag Tag para logging
     * @return true si se detuvo correctamente, false si hubo errores
     */
    @SuppressLint("MissingPermission")
    fun safeStopScan(scanner: BluetoothLeScanner?, callback: ScanCallback, tag: String = TAG): Boolean {
        return try {
            scanner?.stopScan(callback)
            Log.d(tag, "Escaneo detenido correctamente")
            true
        } catch (e: Exception) {
            Log.w(tag, "Error al detener escaneo: ${e.message}")
            false
        }
    }
    
    /**
     * Refresca el caché de servicios GATT usando reflection
     * 
     * Esto soluciona un bug conocido de Android BLE donde gatt.services
     * puede devolver una lista vacía después de onServicesDiscovered.
     * 
     * @param gatt Conexión GATT a refrescar
     * @param tag Tag para logging
     * @return true si el refresh fue exitoso, false si falló
     */
    fun refreshGattCache(gatt: BluetoothGatt?, tag: String = TAG): Boolean {
        return try {
            gatt?.let {
                val refreshMethod = it.javaClass.getMethod("refresh")
                val result = refreshMethod.invoke(it) as? Boolean ?: false
                if (result) {
                    Log.d(tag, "Cache GATT refrescado exitosamente")
                } else {
                    Log.w(tag, "refresh() devolvio false")
                }
                result
            } ?: false
        } catch (e: Exception) {
            Log.w(tag, "No se pudo refrescar cache GATT (puede no ser necesario): ${e.message}")
            // No es un error crítico, algunos dispositivos no lo necesitan
            false
        }
    }

    /**
     * Remove o pareamento (bond) de um dispositivo Bluetooth via reflection.
     * 
     * Coolers RedMagic não armazenam chaves LTK de forma persistente.
     * Se o Android mantiver o registro de pareamento após uma desconexão ou ciclo de energia,
     * as tentativas subsequentes de conexão falham com GATT status 22 (LOCAL HOST TERMINATED).
     * 
     * @param device Dispositivo cujo pareamento deve ser removido
     * @param tag Tag para logging
     * @return true se removeBond foi invocado com sucesso, false caso contrário
     */
    @SuppressLint("MissingPermission")
    fun unpairDevice(device: BluetoothDevice?, tag: String = TAG): Boolean {
        if (device == null) return false
        return try {
            if (device.bondState != BluetoothDevice.BOND_NONE) {
                Log.d(tag, "Removendo pareamento obsoleto de ${device.address} (bondState=${device.bondState})...")
                val removeBondMethod = device.javaClass.getMethod("removeBond")
                val result = removeBondMethod.invoke(device) as? Boolean ?: false
                Log.d(tag, "removeBond() retorno: $result")
                result
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(tag, "Erro ao invocar removeBond: ${e.message}")
            false
        }
    }
}

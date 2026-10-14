package com.example.otomuzik.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import com.example.otomuzik.model.Song
import java.io.File

object RingtoneHelper {

    fun setAsRingtone(context: Context, song: Song): Boolean {
        // Android 6.0+ için sistem ayarlarını değiştirme izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Toast.makeText(
                    context,
                    "Zil sesi ayarlayabilmek için 'Sistem ayarlarını değiştirme' iznini açın.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return false
            }
        }

        try {
            val ringtoneUri = getMediaUri(context, song)
            if (ringtoneUri != null) {
                // MediaStore üzerinde IS_RINGTONE işaretini güncelle
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    }
                    context.contentResolver.update(ringtoneUri, values, null, null)
                } catch (ignored: Exception) {
                }

                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    ringtoneUri
                )
                Toast.makeText(
                    context,
                    "✓ Zil sesi olarak ayarlandı: ${song.title}",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            } else {
                Toast.makeText(
                    context,
                    "Parçanın medya bağlantısı bulunamadı.",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Zil sesi ayarlanamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    private fun getMediaUri(context: Context, song: Song): Uri? {
        if (song.uriString.startsWith("content://")) {
            return Uri.parse(song.uriString)
        }
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs = arrayOf(song.path)
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.fromFile(File(song.path))
    }
}

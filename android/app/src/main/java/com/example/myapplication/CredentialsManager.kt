package com.example.myapplication

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

// Simple data class for the file rows
data class UserCredential(val login: String, val password: String)

class CredentialsManager(private val context: Context) {
    private val gson = Gson()
    private val fileName = "saved_logins.json"

    // 1. Parse Excel Uri -> List<UserCredential>
    fun parseExcelAndSave(uri: Uri): Int {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
        val tempUsers = mutableListOf<UserCredential>()

        try {
            // Apache POI logic to read Excel
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Read first sheet

            // Iterate rows (Skip header if necessary, here we assume Row 0 is data)
            for (row in sheet) {
                // Assuming Column 0 = Login, Column 1 = Password
                val loginCell = row.getCell(0)
                val passCell = row.getCell(1)

                if (loginCell != null && passCell != null) {
                    // Force cells to string to avoid numeric formatting issues
                    loginCell.cellType = CellType.STRING
                    passCell.cellType = CellType.STRING

                    val login = loginCell.stringCellValue.trim()
                    val pass = passCell.stringCellValue.trim()

                    if (login.isNotEmpty()) {
                        tempUsers.add(UserCredential(login, pass))
                    }
                }
            }
            workbook.close()

            // Save to local JSON file
            saveListToJson(tempUsers)
            return tempUsers.size

        } catch (e: Exception) {
            e.printStackTrace()
            return -1 // Error code
        } finally {
            inputStream.close()
        }
    }

    // 2. Save List to Internal Storage
    private fun saveListToJson(users: List<UserCredential>) {
        val jsonString = gson.toJson(users)
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }

    // 3. Load List from Internal Storage
    fun loadUsers(): List<UserCredential> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        val jsonString = file.readText()
        val type = object : TypeToken<List<UserCredential>>() {}.type
        return gson.fromJson(jsonString, type)
    }
}
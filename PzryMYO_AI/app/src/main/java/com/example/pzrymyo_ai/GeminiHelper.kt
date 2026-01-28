package com.example.pzrymyo_ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiHelper {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "//GEMINI_API_KEY//"
    )

    // 2. Soru Sorma Fonksiyonu
    suspend fun askGemini(prompt: String): String {
        // IO Thread: İnternet işlemleri için en güvenli hat.
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                // Cevap varsa döndür, yoksa hata mesajı ver
                response.text ?: "Boş cevap geldi."
            } catch (e: Exception) {
                // İnternet yoksa veya hata varsa buraya düşer
                "Hata oluştu: ${e.localizedMessage}"
            }
        }
    }
}
package com.example.ui

object PrayerPromptGenerator {
    private val templates = listOf(
        "Focus today's prayer on seeking wisdom to walk in {theme}, asking God to reveal practical areas where you can cultivate this virtue.",
        "Dear Lord, we invite Your presence into our schedule today; help us to fully surrender our fears and rely on Your {theme}.",
        "Focus today's prayer on thankfulness, praising God for the promise of {theme} that anchors our souls through life's unpredictable seasons.",
        "Focus today's prayer on requesting the grace and spiritual endurance to extend {theme} to those in your immediate family and workplace.",
        "Lord, let the message of {theme} sink deep into our hearts today, driving out all anxiety and replacing it with quiet confidence.",
        "Focus today's prayer on inviting the Holy Spirit to guide your thoughts and keep your heart aligned with the truth of {theme} today.",
        "Heavenly Father, inspire us to live with bold courage, anchoring every choice on the rock-solid foundation of Your {theme}."
    )

    fun generatePrompt(theme: String, seed: Int): String {
        val cleanTheme = theme.trim().removeSuffix(".")
        val displayTheme = if (cleanTheme.isNotBlank()) cleanTheme.lowercase() else "faith and grace"
        
        // Pick template deterministically using the seed
        val index = if (seed >= 0) seed % templates.size else (seed * -1) % templates.size
        val prompt = templates[index].replace("{theme}", displayTheme)
        
        // Ensure first character is capitalized (if cleanTheme was capitalized, templates capitalize correctly or we can adjust)
        return prompt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

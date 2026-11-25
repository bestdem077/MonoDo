package com.example.data.model

enum class WishCategory(val label: String) {
    FRIEND("Friend"),
    FAMILY("Family"),
    FORMAL("Formal")
}

data class BirthdayWishOption(
    val category: WishCategory,
    val title: String,
    val message: String
)

object BirthdayWishTemplates {
    fun defaultWishFor(
        name: String,
        relationship: String = "Friend",
        turningAge: Int? = null
    ): String {
        val cleanName = name.trim().ifBlank { "there" }
        val agePhrase = turningAge?.let { " on turning $it" } ?: ""
        return when {
            relationship.equals("Family", ignoreCase = true) ||
                relationship.equals("Partner", ignoreCase = true) ->
                "Happy Birthday $cleanName! Wishing you good health, peace, and endless happiness$agePhrase. So grateful to have you in my life!"
            relationship.equals("Colleague", ignoreCase = true) ->
                "Wishing you a very Happy Birthday, $cleanName! May the year ahead bring continued success, good health, and great milestones."
            else ->
                "Happy Birthday $cleanName! Wishing you an amazing year ahead$agePhrase filled with joy, laughter, and great memories!"
        }
    }

    fun wishesFor(
        name: String,
        turningAge: Int? = null
    ): List<BirthdayWishOption> {
        val cleanName = name.trim().ifBlank { "there" }
        val ageSuffix = turningAge?.let { " Welcome to $it!" } ?: ""
        return listOf(
            // Friend
            BirthdayWishOption(
                category = WishCategory.FRIEND,
                title = "Warm & Classic",
                message = "Happy Birthday $cleanName!$ageSuffix Wishing you an awesome day and a year packed with happiness, health, and good times."
            ),
            BirthdayWishOption(
                category = WishCategory.FRIEND,
                title = "Short & Crisp",
                message = "Happy Birthday, $cleanName! Hope your day is as great as you are. Let's catch up and celebrate soon!"
            ),
            BirthdayWishOption(
                category = WishCategory.FRIEND,
                title = "Hinglish Friendly",
                message = "Janamdin ki dher saari shubhkamnayein $cleanName! Hamesha khush raho aur har sapna poora ho. Party pending hai!"
            ),
            // Family
            BirthdayWishOption(
                category = WishCategory.FAMILY,
                title = "Heartfelt Family",
                message = "Happy Birthday $cleanName! Thank you for always being such a wonderful part of our family. Wishing you good health, joy, and peace always."
            ),
            BirthdayWishOption(
                category = WishCategory.FAMILY,
                title = "Warm Blessing",
                message = "Wishing you a very Happy Birthday, $cleanName!$ageSuffix May this new year of your life bring endless blessings, smiles, and strength."
            ),
            BirthdayWishOption(
                category = WishCategory.FAMILY,
                title = "Hinglish Family",
                message = "Happy Birthday $cleanName! Bhagwan aapko hamesha swasth, khush aur kamyab rakhe. Aapka saath hamare liye sabse bada tohfa hai."
            ),
            // Formal
            BirthdayWishOption(
                category = WishCategory.FORMAL,
                title = "Professional",
                message = "Wishing you a very Happy Birthday, $cleanName. May the coming year bring you continued success, good health, and rewarding milestones."
            ),
            BirthdayWishOption(
                category = WishCategory.FORMAL,
                title = "Respectful & Brief",
                message = "Warmest birthday wishes to you, $cleanName. Wishing you a pleasant celebration and a prosperous year ahead."
            )
        )
    }
}

package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,  // Singleton — only one profile
    val name: String = "",
    val targetSpecialty: String = "",
    val experienceLevel: String = "",  // "first_time", "retake_1", "retake_2plus"
    val onboardingCompleted: Boolean = false,
    val joinDate: Long = System.currentTimeMillis(),
    val totalQuestionsAnswered: Int = 0,
    val currentStreakDays: Int = 0,
    val lastStudyDate: Long = 0L,
    val totalStudyTimeMinutes: Int = 0
)

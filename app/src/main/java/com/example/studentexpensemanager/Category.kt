package com.example.studentexpensemanager

import androidx.compose.ui.graphics.Color

data class Category(
    val name: String,
    val icon: String,
    val color: Color
)

val expenseCategories = listOf(
    Category("Food", "🍽️", Color(0xFFFFE0B2)),
    Category("Transport", "🚗", Color(0xFFB3E5FC)),
    Category("Bills", "🧾", Color(0xFFD1C4E9)),
    Category("Shopping", "🛍️", Color(0xFFF8BBD0)),
    Category("Other", "⋯", Color(0xFFE0E0E0))
)

val incomeCategories = listOf(
    Category("Salary", "🏦", Color(0xFFC5CAE9)),
    Category("Business", "🏢", Color(0xFFD7CCC8)),
    Category("Investment", "📈", Color(0xFFA5D6A7)),
    Category("Gift", "🎀", Color(0xFFF8BBD0)),
    Category("Other", "⋯", Color(0xFFE0E0E0))
)

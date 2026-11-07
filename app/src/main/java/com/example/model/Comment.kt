package com.example.model

data class Comment(
    val id: String,
    val author: String,
    val avatarInitials: String,
    val text: String,
    val timeAgo: String,
    var likes: Int,
    var isLiked: Boolean = false
)

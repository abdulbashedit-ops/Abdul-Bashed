package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ChatLanguage

data class QuickPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector,
    val langTag: String
)

@Composable
fun QuickStartersView(
    onSelectPrompt: (String) -> Unit,
    currentLanguage: ChatLanguage,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        QuickPrompt(
            title = "ছোট গল্প",
            subtitle = "একটি ছোট অনুপ্রেরণামূলক গল্প বলো",
            prompt = "আমাকে একটি ছোট ও অনুপ্রেরণামূলক শিক্ষণীয় গল্প বলো।",
            icon = Icons.Default.Create,
            langTag = "বাংলা"
        ),
        QuickPrompt(
            title = "Explain Simply",
            subtitle = "Quantum computing basics",
            prompt = "Explain quantum computing in simple terms for a beginner.",
            icon = Icons.Default.Lightbulb,
            langTag = "English"
        ),
        QuickPrompt(
            title = "কোটলিন প্রোগ্রামিং",
            subtitle = "কোটলিন ও জাভার মধ্যে মূল পার্থক্য কী?",
            prompt = "অ্যান্ড্রয়েড অ্যাপ ডেভেলপমেন্টে কোটলিন এবং জাভার মধ্যে প্রধান পার্থক্যগুলো সহজভাবে ব্যাখ্যা করো।",
            icon = Icons.Default.Code,
            langTag = "বাংলা"
        ),
        QuickPrompt(
            title = "Professional Email",
            subtitle = "Draft a formal leave request",
            prompt = "Draft a polite and professional sick leave email to my team lead.",
            icon = Icons.Default.Create,
            langTag = "English"
        ),
        QuickPrompt(
            title = "ভাষা অনুবাদ",
            subtitle = "বাংলা থেকে ইংরেজি অনুবাদ",
            prompt = "নিচের বাক্যটি প্রাতিষ্ঠানিক ইংরেজিতে অনুবাদ করো:\n'আমরা উদ্ভাবনী সমাধান তৈরিতে প্রতিশ্রুতিবদ্ধ এবং প্রযুক্তিকে মানুষের কল্যাণে ব্যবহার করতে চাই।'",
            icon = Icons.Default.Translate,
            langTag = "বাংলা"
        ),
        QuickPrompt(
            title = "Creative Ideas",
            subtitle = "Brainstorm tech project concepts",
            prompt = "Give me 5 unique, modern mobile app ideas using AI for students.",
            icon = Icons.Default.Lightbulb,
            langTag = "English"
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Logo Badge
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Welcome to MyAI",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "কীভাবে সাহায্য করতে পারি? • How can I help you today?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // Cards list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (item in prompts) {
                QuickPromptCard(
                    prompt = item,
                    onClick = { onSelectPrompt(item.prompt) }
                )
            }
        }
    }
}

@Composable
fun QuickPromptCard(
    prompt: QuickPrompt,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("quick_prompt_${prompt.langTag}_${prompt.title}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = prompt.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = prompt.langTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = prompt.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

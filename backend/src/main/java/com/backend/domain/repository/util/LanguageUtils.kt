package com.backend.domain.repository.util

import com.backend.domain.repository.entity.Language

object LanguageUtils {

    fun fromGitHubName(name: String?): Language {
        val normalized = name
            ?.trim()
            ?.lowercase()
            ?.replace("+", "P")
            ?.replace("#", "sharp")
            ?.replace("-", "")
            ?.replace(" ", "")
            ?: return Language.OTHER

        return when (normalized) {
            "java" -> Language.JAVA
            "python" -> Language.PYTHON
            "c" -> Language.C
            "cpp" -> Language.CPP
            "csharp" -> Language.CSHARP
            "go" -> Language.GO
            "rust" -> Language.RUST
            "kotlin" -> Language.KOTLIN
            "swift" -> Language.SWIFT
            "ruby" -> Language.RUBY
            "php" -> Language.PHP
            "objectivc", "objectivec" -> Language.OBJECTIVEC
            "javascript" -> Language.JAVASCRIPT
            "typescript" -> Language.TYPESCRIPT
            "html" -> Language.HTML
            "css" -> Language.CSS
            "scss", "sass" -> Language.SCSS
            "vue" -> Language.VUE
            "r" -> Language.R
            "sql" -> Language.SQL
            "shell", "bash", "sh" -> Language.SHELL
            "yaml", "yml" -> Language.YAML
            "json" -> Language.JSON
            "perl" -> Language.PERL
            "dart" -> Language.DART
            "haskell" -> Language.HASKELL

            else -> Language.OTHER
        }
    }
}

package com.moondicine.app.ai.prompts

object PromptTemplates {

    fun systemPrompt(): String = """
        Você é um especialista em educação médica para provas de residência médica no Brasil.
        Responda sempre e exclusivamente em português do Brasil, incluindo explicações, campos de texto,
        classificações e mensagens. Nunca responda em inglês, salvo quando o texto original exigir a preservação.
        Você conhece as especialidades cobradas em provas como ENAM, ANCAR e VINCI.
        Forneça informações médicas precisas e baseadas em evidências.
        Quando solicitado a analisar ou gerar questões, responda sempre em JSON válido.
    """.trimIndent()

    fun parseQuestionsPrompt(textChunk: String): String = """
        Analise o texto a seguir, extraído de um PDF de prova de residência médica.
        Identifique e extraia cada questão, suas alternativas e a resposta correta, quando disponível.

        Para cada questão, retorne um objeto JSON com:
        - questionNumber: número da questão (inteiro)
        - questionText: texto completo da questão (string)
        - options: array de objetos com "letter" (string: "A", "B", "C", "D", "E") e "text" (string)
        - correctAnswer: letra da alternativa correta, quando disponível, ou null
        - specialty: classifique em exatamente uma destas cinco especialidades: "Clínica Médica", "Cirurgia Geral", "Pediatria", "Ginecologia e Obstetrícia", "Medicina Preventiva"
        - subTopic: subtema específico dentro da especialidade (string)
        - difficulty: dificuldade estimada de 1 a 5, em que 1 é mais fácil e 5 é mais difícil (inteiro)

        Retorne um objeto JSON com um array "questions" contendo todas as questões extraídas.
        Exemplo:
        {"questions": [{"questionNumber": 1, "questionText": "...", "options": [{"letter": "A", "text": "..."}], "correctAnswer": "C", "specialty": "Clínica Médica", "subTopic": "Cardiologia", "difficulty": 3}]}

        Text to analyze:
        ---
        $textChunk
        ---
    """.trimIndent()

    fun generateExplanationPrompt(
        questionText: String,
        options: Map<String, String>,
        correctAnswer: String,
        userAnswer: String?
    ): String {
        val optionsText = options.entries.joinToString("\n") { (letter, text) -> "$letter) $text" }
        val userAnswerText = userAnswer?.let { "\nResposta do usuário: $it" } ?: ""

        return """
            Você é um especialista em educação médica explicando respostas de provas de residência.
            Responda exclusivamente em português do Brasil e forneça uma explicação detalhada.

            Questão: $questionText

            Alternativas:
            $optionsText

            Resposta correta: $correctAnswer)$options[correctAnswer]
            $userAnswerText

            Return a JSON object with:
            Retorne um objeto JSON com:
            - "correctReasoning": explicação detalhada, em português do Brasil, do motivo pelo qual a resposta correta está certa
            - "wrongReasoning": objeto que associa cada alternativa errada a uma explicação específica do motivo do erro
            - "highYieldPoints": array com 2 a 4 pontos essenciais para a prova
            - "relatedTopics": array com 2 a 3 temas relacionados para estudo

            Torne as explicações clinicamente relevantes e focadas na prova.
        """.trimIndent()
    }

    fun baselineAssessmentPrompt(): String = """
        Gere 20 questões de múltipla escolha em português do Brasil para uma avaliação inicial de residência médica.
        Cubra estas áreas com distribuição aproximadamente equilibrada:
        - 3-4 questões de Clínica Médica
        - 2-3 questões de Pediatria
        - 2-3 questões de Ginecologia e Obstetrícia
        - 2-3 questões de Cirurgia Geral
        - 2-3 questões de Medicina Preventiva
        - 1-2 questões de Psiquiatria
        - 1-2 questões de Medicina de Família
        - 1-2 questões de outras especialidades

        Cada questão deve ter 5 alternativas (A-E) e uma única resposta correta.
        Varie a dificuldade entre fácil, média e difícil.

        Retorne JSON: {"questions": [...]}
    """.trimIndent()
}
